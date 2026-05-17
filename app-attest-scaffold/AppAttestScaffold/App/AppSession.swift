import Foundation
import SwiftUI

/// "用户当前处于鉴权流程的哪个阶段" 的唯一权威状态源。
///
/// 所有 UI 层都通过观察 `phase` 来决定渲染什么；可变操作统一通过该类的方法发起，
/// 使状态切换集中可见。该类标注为 `@MainActor`，因此 SwiftUI 状态更新必然落在主线程，
/// 调用方无需显式 dispatch。
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

    private let auth: AuthService

    init(auth: AuthService = AuthService()) {
        self.auth = auth
    }

    // MARK: - Bootstrap

    /// 从 Keychain 恢复会话，否则切换到 `.unauthenticated`。
    /// 由 `AppAttestScaffoldApp` 在启动时调用一次。
    func bootstrap() async {
        if let resumed = await auth.bootstrap() {
            phase = .signedIn(resumed)
        } else {
            phase = .unauthenticated
        }
    }

    // MARK: - 登录

    /// 匿名登录（App Attestation 用法 1）。仅在首次启动时可用 ——
    /// 详见 `FirstLaunchFlag` 与 `LoginView` 中的入口控制。
    func anonymousSignIn() async {
        await runAuth { try await self.auth.anonymousSignIn() }
    }

    /// 标准 OTP 登录（App Attestation 用法 2）。
    func signInWithPhoneOTP(recipient: String, ticket: String, otp: String) async {
        await runAuth {
            try await self.auth.signInWithPhoneOTP(
                recipient: recipient,
                otpTicket: ticket,
                otp: otp
            )
        }
    }

    // MARK: - OTP 投递

    /// 调用方负责展示倒计时。错误以 `APIError` 抛出（UI 层就地捕获并显示 ——
    /// 这里不写入 `lastError`，因为这只是流程中的一步，并非 phase 切换）。
    func sendPhoneOTP(phone: String, purpose: String? = nil) async throws -> OTPTicket {
        try await auth.sendPhoneOTP(phone: phone, purpose: purpose)
    }

    // MARK: - 绑定 / 解绑

    func bindPhone(ticket: String, otp: String) async {
        await runAuth { try await self.auth.bindPhone(otpTicket: ticket, otp: otp) }
    }

    func unbind(factorId: String) async {
        await runAuth { try await self.auth.unbind(factorId: factorId) }
    }

    // MARK: - 诊断

    /// 手动走一轮 App Assertion 续期。仅由设置页“诊断”入口调用, 供开发/调试
    /// 验证服务端 refresh 链路。成功: 更新 phase 的 tokens 部分同时保留 account 不变;
    /// 失败: 错误信息写入 `lastError` 供 UI 展示, kid 被吊销则回到未登录态。
    func manualRefreshTokens() async {
        guard case let .signedIn(current) = phase else {
            lastError = "当前会话不可用, 无法续期"
            return
        }
        do {
            let fresh = try await auth.forceRefresh()
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

    // MARK: - 切换 issuer

    /// 应用一个新的 issuer URL。会清空 Discovery 缓存、登出并回到登录页。
    func updateIssuer(_ newIssuer: String) async {
        AppConfiguration.issuer = newIssuer
        await OIDCDiscoveryService.shared.invalidate()
        await signOut()
    }

    // MARK: - 内部实现

    /// 把一个产出 `AuthResult` 的闭包包成 phase + error 处理 pipeline，让各个调用点保持简洁。
    /// 错误通过 `lastError` 暴露给 UI 行内展示。
    private func runAuth(_ body: () async throws -> AuthResult) async {
        do {
            let result = try await body()
            phase = .signedIn(result)
            lastError = nil
        } catch let error as APIError {
            switch error {
            case .kidRevoked:
                // 强制登出路径 —— `AuthService` 已经清掉了本地状态。
                phase = .unauthenticated
                lastError = error.errorDescription
            default:
                lastError = error.errorDescription
            }
        } catch {
            lastError = error.localizedDescription
        }
    }
}
