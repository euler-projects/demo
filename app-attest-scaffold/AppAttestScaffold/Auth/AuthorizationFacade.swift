import Foundation
import Alamofire

/// 一次成功登录或刷新后由授权服务（OAuth 2.1 Authorization Server）产出的认证态。
///
/// 仅包含授权域内的事实：token bundle、来自 `/userinfo` 的用户档案、当前会话所绑定的
/// App Attest key 元数据。账号身份信息（identities）属于用户账号服务，由 `AccountService`
/// 维护，不在此结构中体现 —— 上层 `AppSession` 会把二者合成 UI 层使用的 `AuthResult`。
struct AuthSession: Equatable {
    var tokens: TokenBundle
    var profile: Account.Profile
    var appAttestKey: Account.AppAttestKey
}

/// 续期失败回调签名。`AppSession` 注入实例，`AuthorizationFacade` 在内部续期失败时调用。
/// Session 层收到后按错误类型决策（`.kidRevoked` 强制登出 / 网络错误仅报错保留会话）。
typealias RefreshFailureCallback = @Sendable (APIError) async -> Void

/// OAuth 层对外暴露的 token 提供者契约。业务 Client 仅持有此协议视图，避免感知到
/// 登录流程、App Attest 私钥、Discovery 等授权域内部细节。
protocol AuthorizationTokenProvider: AnyObject {

    /// 返回当前可用的 access token。
    /// 保证返回值的剩余有效期严格大于 `TokenBundle.refreshLeadTime`（默认 60 秒）。
    /// 缓存命中即直返；剩余有效期不足则内部走一轮 App Assertion 续期。
    /// - Throws: 续期失败时先调用 `onRefreshFailure`、再向上抛 `APIError`。
    func getAccessToken() async throws -> String

    /// 业务 Client 收到非临期 401（服务端中途吊销 / 时钟漂移）时可调用一次。
    /// 标记本地缓存的 AT 不再可信，下次 `getAccessToken` 必经续期路径。
    func invalidateCurrentToken() async
}

/// 与「OAuth 2.1 Authorization Server」交互的高层门面 —— 把 "用户希望登录 / 刷新" 翻译为
/// 正确顺序的 challenge → attestation → token 调用链。
///
/// 仅承担授权域职责：
/// - App Attest key 生成、attestation / assertion 取证。
/// - challenge / token / userinfo 端点调用。
/// - access token 续期与会话凭证持久化。
/// - 通过 `AuthorizationTokenProvider` 协议向业务 Client 暴露 fresh AT。
///
/// 账号身份管理（绑定 / 解绑 / 列表）属于用户账号服务的职责，由 `AccountService` 处理。
/// 业务 Client 同时持有 `AuthorizationTokenProvider` 与 Alamofire `Session`：每次请求前
/// 调 `getAccessToken()` 拿 fresh AT、拼到 `Authorization` 头，然后直接 `session.request(...)`
/// 发请求 —— 不再有 `refreshIfNeeded()` 之类需要外部记得调用的样板。
final class AuthorizationFacade: AuthorizationTokenProvider {

    private let oauth: OAuthClient
    private let otp: OTPClient
    private let userInfo: UserInfoClient
    private let attest: AppAttestService
    private let onRefreshFailure: RefreshFailureCallback
    private let coordinator: TokenCoordinator

    init(
        http: Session,
        attest: AppAttestService = .shared,
        onRefreshFailure: @escaping RefreshFailureCallback
    ) {
        let oauth = OAuthClient(http: http)
        self.oauth = oauth
        self.otp = OTPClient(http: http)
        self.userInfo = UserInfoClient(http: http)
        self.attest = attest
        self.onRefreshFailure = onRefreshFailure
        self.coordinator = TokenCoordinator(oauth: oauth, attest: attest)
    }

    // MARK: - Bootstrap

    /// 冷启动尝试从持久化中恢复授权域会话。
    /// - Returns: 没有可用 kid / token 时返回 `nil`（应展示登录页）；
    ///            否则返回 `AuthSession`，必要时已完成一次 token 刷新。
    func resumeFromStorage() async -> AuthSession? {
        guard
            let kid = AppDataStore.get(String.self, for: AppDataStore.Keys.kid),
            !kid.isEmpty
        else { return nil }
        guard let cachedAccount = AppDataStore.get(Account.self, for: AppDataStore.Keys.account) else { return nil }
        guard let key = cachedAccount.appAttestKey else { return nil }

        let tokens: TokenBundle
        do {
            tokens = try await coordinator.obtain()
        } catch APIError.kidRevoked {
            await wipeSession()
            return nil
        } catch {
            // 冷启动遇到网络抖动 —— 回退到任何已缓存的 AT。
            // 下一次携带 AT 的调用会自行重试 / 登出。
            guard let cached = AppDataStore.get(TokenBundle.self, for: AppDataStore.Keys.tokenBundle) else {
                return nil
            }
            tokens = cached
        }

        return AuthSession(
            tokens: tokens,
            profile: cachedAccount.profile,
            appAttestKey: key
        )
    }

    // MARK: - 匿名登录（App Attestation 用法 1）

    /// 全新设备 + 全新匿名账号。生成新的 kid、attest 之后再用 attestation 换取 AT。
    /// 服务端可能不下发 userinfo（匿名账号），此时回退到占位 profile，由调用方决定如何展示。
    func anonymousSignIn() async throws -> AuthSession {
        guard attest.isSupported else { throw APIError.appAttestNotSupported }

        let kid = try await attest.generateKey()
        let challenge = try await oauth.fetchChallenge()
        let attestation = try await attest.attest(kid: kid, challenge: challenge)

        let tokens = try await oauth.anonymousRegister(
            kid: kid,
            attestationBase64: attestation,
            challenge: challenge
        )

        try AppDataStore.set(kid, for: AppDataStore.Keys.kid, lifecycle: .session, secret: true)
        try AppDataStore.set(tokens, for: AppDataStore.Keys.tokenBundle, lifecycle: .session, secret: true)

        let profile = (try? await userInfo.fetch(accessToken: tokens.accessToken))
            ?? Account.Profile(username: "anonymous", nickname: nil, avatarUrl: nil)
        return AuthSession(
            tokens: tokens,
            profile: profile,
            appAttestKey: Account.AppAttestKey(kid: kid, iat: Date())
        )
    }

    // MARK: - 标准 OTP 登录（App Attestation 用法 2）

    /// 标准登录：在同一个 `/oauth2/token` 调用中完成 OTP 校验 + 设备注册。
    /// `recipient` 为 E.164 手机号；OTP ticket 通过 `sendPhoneOTP` 预先获取。
    func signInWithPhoneOTP(
        recipient: String,
        otpTicket: String,
        otp: String
    ) async throws -> AuthSession {
        guard attest.isSupported else { throw APIError.appAttestNotSupported }

        let kid = try await attest.generateKey()
        let challenge = try await oauth.fetchChallenge()
        let attestation = try await attest.attest(kid: kid, challenge: challenge)

        let tokens = try await oauth.signInWithOTP(
            otpTicket: otpTicket,
            otp: otp,
            kid: kid,
            attestationBase64: attestation,
            challenge: challenge
        )

        try AppDataStore.set(kid, for: AppDataStore.Keys.kid, lifecycle: .session, secret: true)
        try AppDataStore.set(tokens, for: AppDataStore.Keys.tokenBundle, lifecycle: .session, secret: true)

        let profile = (try? await userInfo.fetch(accessToken: tokens.accessToken))
            ?? Account.Profile(username: recipient, nickname: nil, avatarUrl: nil)
        return AuthSession(
            tokens: tokens,
            profile: profile,
            appAttestKey: Account.AppAttestKey(kid: kid, iat: Date())
        )
    }

    // MARK: - OTP 投递

    /// 触发一次 SMS OTP。返回的 ticket 在用户输入验证码后交给 `signInWithPhoneOTP`
    /// 或账号服务的绑定接口使用。
    /// - Parameters:
    ///   - phone: E.164 格式手机号，例如 `+8613900000000`。
    func sendPhoneOTP(phone: String) async throws -> OTPTicket {
        try await otp.sendOTP(
            channel: AppConfiguration.otpChannelSMS,
            recipient: phone
        )
    }

    // MARK: - Token 管理（AuthorizationTokenProvider）

    /// 返回当前可用的 access token；保证有效期 > 1 分钟。临期内部续期。
    func getAccessToken() async throws -> String {
        do {
            let bundle = try await coordinator.obtain()
            return bundle.accessToken
        } catch let error as APIError {
            await onRefreshFailure(error)
            if case .kidRevoked = error {
                await wipeSession()
            }
            throw error
        } catch {
            let mapped = APIError.network(message: (error as NSError).localizedDescription)
            await onRefreshFailure(mapped)
            throw mapped
        }
    }

    /// 标记本地缓存 AT 不再可信，下次 `getAccessToken` 必经续期路径。
    /// 业务 Client 收到非临期 401 时调用，作为"服务端中途吊销 kid"小概率窗口的兜底。
    func invalidateCurrentToken() async {
        await coordinator.invalidateCache()
    }

    /// 不论剩余有效期多长都重新走一轮 App Assertion 续期，返回新 `TokenBundle`。
    /// 生产场景：设置页手动续期、外部跨设备失效检测、预热试验。
    @discardableResult
    func refreshTokenForce() async throws -> TokenBundle {
        do {
            return try await coordinator.forceRefresh()
        } catch let error as APIError {
            await onRefreshFailure(error)
            if case .kidRevoked = error {
                await wipeSession()
            }
            throw error
        } catch {
            let mapped = APIError.network(message: (error as NSError).localizedDescription)
            await onRefreshFailure(mapped)
            throw mapped
        }
    }

    // MARK: - 登出

    /// 清除会话凭证与 account，但保留首次启动标志位
    /// （详见 App-Attest-Login.md §5.1）。
    func signOut() async {
        await wipeSession()
    }

    /// 抹掉所有本地数据（包括首次启动标志位与服务配置），恢复到刚安装的状态。
    /// 调用后用户可再次匿名试用。
    func wipeAllData() async {
        AppDataStore.clearAll()
        await coordinator.invalidateCache()
    }

    // MARK: - 内部实现

    /// 登出清理；`kid` 被吊销时也会调用。
    private func wipeSession() async {
        AppDataStore.clearSession()
        await coordinator.invalidateCache()
    }
}

/// AT 续期协调器：缓存命中直返；缺失或临期则单飞续期，并发请求共享同一次续期。
///
/// 通过 `actor` 保证并发安全 —— 多个业务 Client 同时调 `getAccessToken()` 在缓存即将过期
/// 时不会触发多次 `/oauth2/token` 调用。`forceRefresh()` 与 `obtain()` 共用同一个 in-flight
/// Task，因此设置页"手动续期"在临期窗口内不会与正常业务请求抢发。
private actor TokenCoordinator {

    private let oauth: OAuthClient
    private let attest: AppAttestService
    private var inflightRefresh: Task<TokenBundle, Error>?
    /// 业务 Client 通过 `invalidateCurrentToken()` 标记缓存不可信；下一次 `obtain()` 必经续期路径。
    private var cacheInvalidated: Bool = false

    init(oauth: OAuthClient, attest: AppAttestService) {
        self.oauth = oauth
        self.attest = attest
    }

    /// `getAccessToken` 路径：命中缓存（且未被 invalidate、未临期）则直返，否则走单飞续期。
    func obtain() async throws -> TokenBundle {
        if !cacheInvalidated,
           let cached = AppDataStore.get(TokenBundle.self, for: AppDataStore.Keys.tokenBundle),
           !cached.willExpireSoon {
            return cached
        }
        return try await runOrJoinRefresh()
    }

    /// `refreshTokenForce` 路径：跳过临期判断，但同样复用单飞 Task。
    /// 并发 force + getAccessToken 不会重复抢发。
    func forceRefresh() async throws -> TokenBundle {
        return try await runOrJoinRefresh()
    }

    func invalidateCache() {
        cacheInvalidated = true
    }

    private func runOrJoinRefresh() async throws -> TokenBundle {
        if let inflight = inflightRefresh {
            return try await inflight.value
        }
        let task = Task { [oauth, attest] in
            try await Self.refreshNow(oauth: oauth, attest: attest)
        }
        inflightRefresh = task
        defer {
            inflightRefresh = nil
            cacheInvalidated = false
        }
        return try await task.value
    }

    /// 完整跑一次 App Assertion 刷新往返，并持久化结果。
    private static func refreshNow(
        oauth: OAuthClient,
        attest: AppAttestService
    ) async throws -> TokenBundle {
        guard let kid = AppDataStore.get(String.self, for: AppDataStore.Keys.kid) else {
            throw APIError.noRegisteredKey
        }
        let challenge = try await oauth.fetchChallenge()
        let assertion = try await attest.assert(kid: kid, challenge: challenge)
        let tokens = try await oauth.refreshWithAssertion(
            kid: kid,
            assertionBase64: assertion,
            challenge: challenge
        )
        try AppDataStore.set(tokens, for: AppDataStore.Keys.tokenBundle, lifecycle: .session, secret: true)
        return tokens
    }
}
