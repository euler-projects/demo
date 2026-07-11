import Foundation
import SwiftUI
import Alamofire

/// UI 层观测的"已登录态产物"。把授权域的 `AuthSession`（profile + tokens + kid）与账号域
/// 的 identities 列表合成为同一个聚合，便于 SwiftUI 统一渲染。
///
/// `isAnonymous` 仅检查 `phone` factor 是否存在；下游若新增 email / wechat 登录身份，应同步
/// 调整该判断与 `Account.identities` 的派生属性。
struct AuthResult: Equatable {
    let account: Account
    let tokens: TokenBundle
    let isAnonymous: Bool

    init(account: Account, tokens: TokenBundle) {
        self.account = account
        self.tokens = tokens
        self.isAnonymous = account.phoneIdentity == nil
    }
}

/// "用户当前处于鉴权流程的哪个阶段" 的唯一权威状态源，并负责把授权域（`AuthorizationFacade`）
/// 与账号域（`AccountService`）的调用编排为对 UI 友好的单一状态切换。
///
/// 设计原则：
/// - 持有一份 `Alamofire.Session`（业务 Session，不挂任何 `Authenticator`）作为整个 App 的网络
///   入口；同一个 Session 实例同时被授权域内部的 OAuth 端点 Client 与业务 Client 使用，使所有
///   流量共享日志器与连接复用。
/// - `AuthorizationFacade` 通过 `AuthorizationTokenProvider` 协议把 fresh AT 透出给业务 Client；
///   业务 Client (`UserIdentitiesClient`) 在每个请求前调用 `getAccessToken()` 自闭环保鲜，
///   `AppSession` 不再暴露"先 refreshIfNeeded 再调账号服务"这类样板。
/// - `AuthorizationFacade` 续期失败时通过注入的 `RefreshFailureCallback` 通知 `AppSession`，
///   `AppSession` 按错误类型决策：`.kidRevoked` 强制登出；网络错误仅写入 `lastError` 保留会话。
/// - 标注为 `@MainActor`，SwiftUI 状态更新必然落在主线程，调用方无需显式 dispatch。
@MainActor
final class AppSession: ObservableObject {

    /// 顶层 UI 状态。驱动根导航栈。
    enum Phase: Equatable {
        /// 冷启动探测中，应展示闪屏 / 进度。
        case bootstrapping
        /// 没有可用 token / kid，渲染 `LoginView`。
        case unauthenticated
        /// 已登录；账号可能仍是匿名（未绑定手机号）。
        case signedIn(AuthResult)

        var account: Account? {
            if case let .signedIn(result) = self { return result.account }
            return nil
        }

        var tokens: TokenBundle? {
            if case let .signedIn(result) = self { return result.tokens }
            return nil
        }

        var isAnonymous: Bool {
            if case let .signedIn(result) = self { return result.isAnonymous }
            return false
        }
    }

    @Published private(set) var phase: Phase = .bootstrapping
    @Published var lastError: String?

    /// 业务 Client 直接复用的 Alamofire Session。`HomeView` 受保护资源面板等场景也通过它发请求，
    /// 避免再额外构造一份无日志器的 `URLSession`。
    let http: Session

    /// 授权域门面。`HomeView` 诊断面板调用 `auth.refreshTokenForce()` / `auth.getAccessToken()`
    /// 走生产路径；任何业务 Client 同样通过协议视图 `AuthorizationTokenProvider` 取 fresh AT。
    let auth: AuthorizationFacade

    private let account: AccountService

    init() {
        let session = Self.makeBusinessSession()
        self.http = session

        // 续期失败回调需要 代 self 到 `handleRefreshFailure(_:)` —— 但这一行发生
        // 在 self 存储属性还未初始化完的 init 阶段。用一个 thread-safe holder
        // 接住 weak self、并在 init 尾部赋值。
        let holder = WeakSessionHolder()
        let facade = AuthorizationFacade(http: session) { [holder] error in
            guard let session = holder.session else { return }
            await session.handleRefreshFailure(error)
        }
        self.auth = facade
        self.account = AccountService(
            identities: UserIdentitiesClient(auth: facade, http: session)
        )

        holder.session = self
    }

    // MARK: - Bootstrap

    /// 从 Keychain 恢复会话，否则切换到 `.unauthenticated`。
    /// 由 `AppAttestScaffoldApp` 在启动时调用一次。identities 同步是尽力而为的 ——
    /// 失败不阻塞 bootstrap，下一次进入设置页或绑定流程时会再次尝试。
    func bootstrap() async {
        guard let session = await auth.resumeFromStorage() else {
            phase = .unauthenticated
            return
        }
        var identities: [Identity] = AccountStore.load()?.identities ?? []
        if let updated = try? await account.listIdentities() {
            identities = updated
        }
        phase = .signedIn(persist(session: session, identities: identities))
    }

    // MARK: - 登录

    /// 匿名登录（App Attestation 用法 1）。仅在首次启动时可用 ——
    /// 详见 `FirstLaunchFlag` 与 `LoginView` 中的入口控制。
    func anonymousSignIn() async {
        do {
            let session = try await auth.anonymousSignIn()
            // 匿名账号通常没有 identities，此处仍尝试拉取一次以保证状态一致。
            let identities = (try? await account.listIdentities()) ?? []
            phase = .signedIn(persist(session: session, identities: identities))
            lastError = nil
        } catch let error as APIError {
            switch error {
            case .kidRevoked:
                // 强制登出路径 —— `AuthorizationFacade` 已经清掉了本地状态。
                phase = .unauthenticated
                lastError = error.errorDescription
            default:
                lastError = error.errorDescription
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    /// 标准 OTP 登录（App Attestation 用法 2）。
    ///
    /// 错误直接抛出, 由调用方 (PhoneOTPSheet) 就地处理 — 避免走 `lastError`
    /// 触发全局 alert 导致 sheet 被关闭。仅 kid 被吊销时同时写入 `lastError`
    /// 以触发全局登出提示。
    func signInWithPhoneOTP(recipient: String, ticket: String, otp: String) async throws {
        do {
            let session = try await auth.signInWithPhoneOTP(
                recipient: recipient, otpTicket: ticket, otp: otp
            )
            let identities = (try? await account.listIdentities()) ?? []
            phase = .signedIn(persist(session: session, identities: identities))
            lastError = nil
        } catch let error as APIError {
            if case .kidRevoked = error {
                phase = .unauthenticated
                lastError = error.errorDescription
            }
            throw error
        }
    }

    // MARK: - OTP 投递

    /// 调用方负责展示倒计时。错误以 `APIError` 抛出（UI 层就地捕获并显示 ——
    /// 这里不写入 `lastError`，因为这只是流程中的一步，并非 phase 切换）。
    func sendPhoneOTP(phone: String) async throws -> OTPTicket {
        try await auth.sendPhoneOTP(phone: phone)
    }

    // MARK: - 绑定 / 解绑（账号服务调用）

    /// 绑定手机号。错误直接抛出, 由调用方就地处理。
    /// 不需要显式取 token —— `UserIdentitiesClient` 内部会先调
    /// `auth.getAccessToken()` 自闭环续期。
    func bindPhone(ticket: String, otp: String) async throws {
        do {
            _ = try await account.bindPhone(otpTicket: ticket, otp: otp)
            let identities = try await account.listIdentities()
            // 续期可能在 bindPhone 内部发生 —— 重新读一次 SessionStore 以拿到最新 tokens。
            let freshTokens = SessionStore.loadTokenBundle() ?? phase.tokens
            if let freshTokens {
                phase = .signedIn(mergeIdentities(tokens: freshTokens, identities: identities))
            }
            lastError = nil
        } catch let error as APIError {
            if case .kidRevoked = error {
                phase = .unauthenticated
                lastError = error.errorDescription
            }
            throw error
        }
    }

    /// 解绑指定身份。token 续期由 `UserIdentitiesClient` 自闭环。
    func unbind(identityId: String) async {
        do {
            try await account.unbind(identityId: identityId)
            let identities = (try? await account.listIdentities()) ?? []
            let freshTokens = SessionStore.loadTokenBundle() ?? phase.tokens
            if let freshTokens {
                phase = .signedIn(mergeIdentities(tokens: freshTokens, identities: identities))
            }
            lastError = nil
        } catch let error as APIError {
            switch error {
            case .kidRevoked:
                phase = .unauthenticated
                lastError = error.errorDescription
            default:
                lastError = error.errorDescription
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    /// 原子换绑手机号：先解绑旧 identity，再绑定新手机号。
    /// 错误直接抛出, 由调用方 (PhoneOTPSheet) 就地处理。
    func rebindPhone(identityId: String, ticket: String, otp: String) async throws {
        do {
            _ = try await account.rebindPhone(
                identityId: identityId,
                otpTicket: ticket,
                otp: otp
            )
            let identities = try await account.listIdentities()
            let freshTokens = SessionStore.loadTokenBundle() ?? phase.tokens
            if let freshTokens {
                phase = .signedIn(mergeIdentities(tokens: freshTokens, identities: identities))
            }
            lastError = nil
        } catch let error as APIError {
            if case .kidRevoked = error {
                phase = .unauthenticated
                lastError = error.errorDescription
            }
            throw error
        }
    }

    // MARK: - 诊断 / 手动续期

    /// 手动走一轮 App Assertion 续期。设置页"诊断"入口调用，复用与业务路径完全相同的
    /// `AuthorizationFacade.refreshTokenForce()` —— 没有任何"演示专用"分支。
    /// 成功：更新 phase 的 tokens 部分同时保留 account 不变；
    /// 失败：错误信息写入 `lastError` 供 UI 展示，kid 被吊销则回到未登录态。
    func manualRefreshTokens() async {
        guard case let .signedIn(current) = phase else {
            lastError = "当前会话不可用, 无法续期"
            return
        }
        do {
            let fresh = try await auth.refreshTokenForce()
            phase = .signedIn(AuthResult(account: current.account, tokens: fresh))
            lastError = nil
        } catch let error as APIError {
            switch error {
            case .kidRevoked:
                phase = .unauthenticated
                lastError = error.errorDescription
            default:
                lastError = error.errorDescription
            }
        } catch {
            lastError = error.localizedDescription
        }
    }

    // MARK: - 登出

    func signOut() async {
        await auth.signOut()
        phase = .unauthenticated
        lastError = nil
    }

    /// 抹掉所有本地数据并恢复到刚安装状态；包括重置首次启动标志位，使用户可再次匿名试用。
    func wipeAllData() async {
        await auth.wipeAllData()
        phase = .unauthenticated
        lastError = nil
    }

    // MARK: - 切换服务配置

    /// 应用一组新的服务配置（issuer 与账号服务基地址）。会清空 Discovery / 端点缓存、
    /// 登出并回到登录页。
    func updateServiceConfiguration(issuer: String, accountServiceBaseURL: String) async {
        AppConfiguration.issuer = issuer
        AppConfiguration.accountServiceBaseURL = accountServiceBaseURL
        await AuthorizationEndpointsProvider.shared.invalidate()
        await signOut()
    }

    // MARK: - 续期失败回调入口

    /// `AuthorizationFacade` 续期失败时通过 `RefreshFailureCallback` 通知。
    /// 这里仅做 UI 层决策：`.kidRevoked` → 强制登出（facade 内部已 wipe 本地态）；
    /// 其它错误（网络抖动、临时 5xx）仅写入 `lastError`，会话保持。
    fileprivate func handleRefreshFailure(_ error: APIError) async {
        switch error {
        case .kidRevoked:
            phase = .unauthenticated
            lastError = error.errorDescription
        default:
            lastError = error.errorDescription
        }
    }

    // MARK: - 内部：聚合根写回

    /// 从 `AuthSession`（授权域）+ identities 列表（账号域）构造完整 `Account`，写入持久化。
    /// 用于全新登录场景，account 作为聚合根整体替换。
    private func persist(session: AuthSession, identities: [Identity]) -> AuthResult {
        let merged = Account(
            profile: session.profile,
            appAttestKey: session.appAttestKey,
            identities: identities
        )
        try? AccountStore.save(merged)
        return AuthResult(account: merged, tokens: session.tokens)
    }

    /// 在已有 phase.account 基础上仅替换 identities 与 tokens，并回写持久化。
    /// 用于绑定 / 解绑场景：profile 与 appAttestKey 保持不变。
    private func mergeIdentities(tokens: TokenBundle, identities: [Identity]) -> AuthResult {
        let baseline: Account
        if let current = phase.account {
            baseline = current
        } else if let cached = AccountStore.load() {
            baseline = cached
        } else {
            // 理论上 bindPhone / unbind 之前必然已登录；这里作为最后兜底，保证逻辑闭合。
            baseline = Account(
                profile: Account.Profile(username: "user", nickname: nil, avatarUrl: nil),
                appAttestKey: SessionStore.loadKid().map { Account.AppAttestKey(kid: $0, iat: Date()) },
                identities: []
            )
        }
        let updated = Account(
            profile: baseline.profile,
            appAttestKey: baseline.appAttestKey,
            identities: identities
        )
        try? AccountStore.save(updated)
        return AuthResult(account: updated, tokens: tokens)
    }

    // MARK: - 装配

    /// 构造业务 `Alamofire.Session`：默认 URLSessionConfiguration + 脱敏日志 EventMonitor。
    /// 不挂 `Authenticator` —— 业务 Client 显式调 `auth.getAccessToken()` 取 fresh AT，
    /// 401 处理由各业务 Client 按自身语义决策。
    private static func makeBusinessSession() -> Session {
        let configuration = URLSessionConfiguration.default
        configuration.timeoutIntervalForRequest = 30
        configuration.timeoutIntervalForResource = 60
        return Session(
            configuration: configuration,
            eventMonitors: [RedactingEventMonitor()]
        )
    }
}

/// `AppSession.init` 阶段为 `AuthorizationFacade.onRefreshFailure` 闭包提供的
/// thread-safe weak 容器。允许在所有 `self` 存储属性初始化之后再把 `self`
/// 赋值进去，满足 Swift init 阶段不能 capture self 的限制。
/// `actor` 保证跨线程访问安全，`weak` 避免与 `AppSession` 成环。
final class WeakSessionHolder: @unchecked Sendable {
    private let lock = NSLock()
    private weak var _session: AppSession?

    var session: AppSession? {
        get { lock.withLock { _session } }
        set { lock.withLock { _session = newValue } }
    }
}

