import Foundation

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

/// 与「OAuth 2.1 Authorization Server」交互的高层编排器，把 "用户希望登录 / 刷新" 翻译为
/// 正确顺序的 challenge → attestation → token 调用链。
///
/// 仅承担授权域职责：
/// - App Attest key 生成、attestation / assertion 取证。
/// - challenge / token / userinfo 端点调用。
/// - access token 续期与会话凭证持久化。
///
/// 账号身份管理（绑定 / 解绑 / 列表）属于用户账号服务的职责，由 `AccountService` 处理。
/// `AppSession` 在调用账号服务前先调用 `refreshIfNeeded()` 拿到 fresh AT，使得
/// `AccountService` 不必感知 App Attest 续期细节。
final class AuthService {

    private let oauth: OAuthClient
    private let otp: OTPClient
    private let userInfo: UserInfoClient
    private let attest: AppAttestService

    init(
        oauth: OAuthClient = OAuthClient(),
        otp: OTPClient = OTPClient(),
        userInfo: UserInfoClient = UserInfoClient(),
        attest: AppAttestService = .shared
    ) {
        self.oauth = oauth
        self.otp = otp
        self.userInfo = userInfo
        self.attest = attest
    }

    // MARK: - Bootstrap

    /// 冷启动尝试从持久化中恢复授权域会话。
    /// - Returns: 没有可用 kid / token 时返回 `nil`（应展示登录页）；
    ///            否则返回 `AuthSession`，必要时已完成一次 token 刷新。
    func resumeFromStorage() async -> AuthSession? {
        guard
            let kid = SessionStore.loadKid(),
            !kid.isEmpty
        else { return nil }
        guard let cachedAccount = AccountStore.load() else { return nil }
        guard let key = cachedAccount.appAttestKey else { return nil }

        let tokens: TokenBundle
        if let cached = SessionStore.loadTokenBundle(), !cached.willExpireSoon {
            tokens = cached
        } else {
            do {
                tokens = try await refreshTokens(kid: kid)
            } catch APIError.kidRevoked {
                await wipeSession()
                return nil
            } catch {
                // 冷启动遇到网络抖动 —— 回退到任何已缓存的 AT。
                // 下一次携带 AT 的调用会自行重试 / 登出。
                guard let cached = SessionStore.loadTokenBundle() else {
                    return nil
                }
                tokens = cached
            }
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

        try SessionStore.saveKid(kid)
        try SessionStore.saveTokenBundle(tokens)

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

        try SessionStore.saveKid(kid)
        try SessionStore.saveTokenBundle(tokens)

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

    // MARK: - 刷新

    /// 当缓存的 AT 已过期或即将过期时刷新它。返回当前有效的 token bundle。
    /// 调用方在使用返回值发起业务请求时不应再自行检查 `expiresAt` —— 把刷新逻辑集中在
    /// 这一处即可。
    @discardableResult
    func refreshIfNeeded() async throws -> TokenBundle {
        guard let tokens = SessionStore.loadTokenBundle() else {
            throw APIError.invalidConfiguration(message: "缺少访问令牌, 请重新登录")
        }
        return try await ensureFreshTokens(tokens)
    }

    /// 诊断用：忽略过期检查强制走一轮 App Assertion 续期。
    ///
    /// 与 `refreshIfNeeded()` 的区别：`refreshIfNeeded` 仅在 `willExpireSoon` 时才走，适用于
    /// 业务调用前的手动保鲜；本方法不论剩余有效期多长都会重新签发，仅供设置页诊断入口
    /// 驱动。遇到 `kidRevoked` 会走完整登出清理。
    @discardableResult
    func forceRefresh() async throws -> TokenBundle {
        guard let kid = SessionStore.loadKid() else {
            throw APIError.noRegisteredKey
        }
        do {
            return try await refreshTokens(kid: kid)
        } catch APIError.kidRevoked {
            await wipeSession()
            throw APIError.kidRevoked
        }
    }

    // MARK: - 登出

    /// 清除会话凭证与 account，但保留首次启动标志位
    /// （详见 App-Attest-Login.md §5.1）。
    func signOut() async {
        await wipeSession()
    }

    // MARK: - 内部实现

    /// 校验 `bundle` 是否仍然有效；若已不可用则通过 App Assertion 刷新。
    private func ensureFreshTokens(_ bundle: TokenBundle) async throws -> TokenBundle {
        guard bundle.willExpireSoon else { return bundle }
        guard let kid = SessionStore.loadKid() else {
            throw APIError.noRegisteredKey
        }
        do {
            return try await refreshTokens(kid: kid)
        } catch APIError.kidRevoked {
            await wipeSession()
            throw APIError.kidRevoked
        }
    }

    /// 完整跑一次 App Assertion 刷新往返，并持久化结果。
    private func refreshTokens(kid: String) async throws -> TokenBundle {
        let challenge = try await oauth.fetchChallenge()
        let assertion = try await attest.assert(kid: kid, challenge: challenge)
        let tokens = try await oauth.refreshWithAssertion(
            kid: kid,
            assertionBase64: assertion,
            challenge: challenge
        )
        try SessionStore.saveTokenBundle(tokens)
        return tokens
    }

    /// 抹掉所有本地数据（包括首次启动标志位），恢复到刚安装的状态。
    /// 调用后用户可再次匿名试用。
    func wipeAllData() async {
        await wipeSession()
        FirstLaunchFlag.resetForTesting()
    }

    /// 登出清理；`kid` 被吊销时也会调用。
    private func wipeSession() async {
        SessionStore.clearAll()
        AccountStore.clear()
    }
}
