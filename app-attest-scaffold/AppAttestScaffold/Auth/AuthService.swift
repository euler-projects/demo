import Foundation

/// 一次成功登录或刷新的产出。`AppSession` 据此切换 phase。
struct AuthResult: Equatable {
    let account: Account
    let tokens: TokenBundle
    /// 当前账号尚未绑定任何实名 factor 时为 `true`。
    /// 脚手架仅检查 `phone`；下游若新增 email / wechat，应同步扩展 `Account.identities`
    /// 并相应调整该判断。
    let isAnonymous: Bool

    init(account: Account, tokens: TokenBundle) {
        self.account = account
        self.tokens = tokens
        self.isAnonymous = account.phoneIdentity == nil
    }
}

/// 高层编排器，把 "用户希望登录 / 绑定 / 刷新" 翻译为正确顺序的
/// challenge → attestation → token → identities → userinfo 调用链。
///
/// 所有耗时调用均为 async；UI 状态由 `AppSession` 持有，它会观察各方法的结果并据此更新
/// 自身的 `Phase`。
final class AuthService {

    private let oauth: OAuthClient
    private let otp: OTPClient
    private let identities: UserIdentitiesClient
    private let userInfo: UserInfoClient
    private let attest: AppAttestService
    private let discovery: OIDCDiscoveryService

    init(
        oauth: OAuthClient = OAuthClient(),
        otp: OTPClient = OTPClient(),
        identities: UserIdentitiesClient = UserIdentitiesClient(),
        userInfo: UserInfoClient = UserInfoClient(),
        attest: AppAttestService = .shared,
        discovery: OIDCDiscoveryService = .shared
    ) {
        self.oauth = oauth
        self.otp = otp
        self.identities = identities
        self.userInfo = userInfo
        self.attest = attest
        self.discovery = discovery
    }

    // MARK: - Bootstrap

    /// 决定冷启动时显示哪种 UI。
    /// - Returns: 需要登录时返回 `nil`（无 kid / 无 token）；恢复成功（必要时已完成刷新）
    ///            时返回 `AuthResult`。
    func bootstrap() async -> AuthResult? {
        guard
            let kid = SessionStore.loadKid(),
            !kid.isEmpty,
            let cachedAccount = AccountStore.load()
        else { return nil }

        // 已有 kid + 缓存的 account。尝试加载（或刷新）token bundle。
        let tokens: TokenBundle
        if let cached = SessionStore.loadTokenBundle(), !cached.willExpireSoon {
            tokens = cached
        } else {
            do {
                tokens = try await refreshTokens(kid: kid)
            } catch APIError.kidRevoked {
                await fullSignOut()
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

        // 尽力同步一次身份信息。这里失败不阻塞 bootstrap。
        var account = cachedAccount
        if let updated = try? await identities.list(accessToken: tokens.accessToken) {
            account.identities = updated
            try? AccountStore.save(account)
        }
        return AuthResult(account: account, tokens: tokens)
    }

    // MARK: - 匿名登录（App Attestation 用法 1）

    /// 全新设备 + 全新匿名账号。生成新的 kid、attest 之后再用 attestation 换取 AT。
    func anonymousSignIn() async throws -> AuthResult {
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
        let account = Account(
            profile: profile,
            appAttestKey: Account.AppAttestKey(kid: kid, iat: Date()),
            identities: []
        )
        try AccountStore.save(account)

        return AuthResult(account: account, tokens: tokens)
    }

    // MARK: - 标准 OTP 登录（App Attestation 用法 2）

    /// 标准登录：在同一个 `/oauth2/token` 调用中完成 OTP 校验 + 设备注册。
    /// `recipient` 为 E.164 手机号；OTP ticket 通过 `sendPhoneOTP` 预先获取。
    func signInWithPhoneOTP(recipient: String, otpTicket: String, otp: String) async throws -> AuthResult {
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
        let identitiesList = (try? await identities.list(accessToken: tokens.accessToken)) ?? []
        let account = Account(
            profile: profile,
            appAttestKey: Account.AppAttestKey(kid: kid, iat: Date()),
            identities: identitiesList
        )
        try AccountStore.save(account)

        return AuthResult(account: account, tokens: tokens)
    }

    // MARK: - OTP 投递

    /// 触发一次 SMS OTP。返回的 ticket 在用户输入验证码后交给
    /// `signInWithPhoneOTP` / `bindPhone` 使用。
    /// - Parameters:
    ///   - phone: E.164 格式手机号，例如 `+8613900000000`。
    ///   - purpose: 登录场景传 `nil`；绑定场景传该绑定流程对应的标签。
    func sendPhoneOTP(phone: String, purpose: String? = nil) async throws -> OTPTicket {
        try await otp.sendOTP(
            channel: AppConfiguration.otpChannelSMS,
            recipient: phone,
            purpose: purpose
        )
    }

    // MARK: - 绑定手机号（匿名账号 → 实名）

    /// 将手机号 factor 绑定到当前（匿名或实名）账号上。调用方必须已持有有效 AT —— 不确定时
    /// 先调用 `refreshIfNeeded()` 刷新一次。
    func bindPhone(otpTicket: String, otp: String) async throws -> AuthResult {
        guard let tokens = SessionStore.loadTokenBundle() else {
            throw APIError.invalidConfiguration(message: "缺少访问令牌, 请重新登录")
        }
        let fresh = try await ensureFreshTokens(tokens)
        _ = try await identities.bindPhone(
            accessToken: fresh.accessToken,
            otpTicket: otpTicket,
            otp: otp
        )
        // 重新拉一次完整列表，确保本地与服务端规范状态一致。
        let identitiesList = try await identities.list(accessToken: fresh.accessToken)
        var account = AccountStore.load() ?? Account(
            profile: Account.Profile(username: "user", nickname: nil, avatarUrl: nil),
            appAttestKey: SessionStore.loadKid().map { Account.AppAttestKey(kid: $0, iat: Date()) },
            identities: []
        )
        account.identities = identitiesList
        try AccountStore.save(account)
        return AuthResult(account: account, tokens: fresh)
    }

    // MARK: - 解绑

    /// 按 `factor_id` 解绑某个已绑定 factor。
    func unbind(factorId: String) async throws -> AuthResult {
        guard let tokens = SessionStore.loadTokenBundle() else {
            throw APIError.invalidConfiguration(message: "缺少访问令牌, 请重新登录")
        }
        let fresh = try await ensureFreshTokens(tokens)
        try await identities.unbind(accessToken: fresh.accessToken, factorId: factorId)
        let identitiesList = (try? await identities.list(accessToken: fresh.accessToken)) ?? []
        var account = AccountStore.load() ?? Account(
            profile: Account.Profile(username: "user", nickname: nil, avatarUrl: nil),
            appAttestKey: SessionStore.loadKid().map { Account.AppAttestKey(kid: $0, iat: Date()) },
            identities: []
        )
        account.identities = identitiesList
        try AccountStore.save(account)
        return AuthResult(account: account, tokens: fresh)
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

    /// 诊断用: 忽略过期检查强制走一轮 App Assertion 续期。
    ///
    /// 与 `refreshIfNeeded()` 的区别: `refreshIfNeeded` 仅在 `willExpireSoon` 时才走, 适用于
    /// 业务调用前的手动保鲜; 本方法不论剩余有效期多长都会重新签发, 仅供
    /// 设置页诊断入口驱动。遇到 `kidRevoked` 会走完整登出清理。
    @discardableResult
    func forceRefresh() async throws -> TokenBundle {
        guard let kid = SessionStore.loadKid() else {
            throw APIError.noRegisteredKey
        }
        do {
            return try await refreshTokens(kid: kid)
        } catch APIError.kidRevoked {
            await fullSignOut()
            throw APIError.kidRevoked
        }
    }

    // MARK: - 登出

    /// 清除会话凭证与 account，但保留首次启动标志位
    /// （详见 App-Attest-Login.md §5.1）。
    func signOut() async {
        await fullSignOut()
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
            await fullSignOut()
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

    /// 登出清理；`kid` 被吊销时也会调用。
    private func fullSignOut() async {
        SessionStore.clearAll()
        AccountStore.clear()
    }
}
