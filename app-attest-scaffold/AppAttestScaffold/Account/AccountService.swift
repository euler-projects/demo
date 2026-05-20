import Foundation

/// 与「用户账号服务」(Account Service) 交互的高层封装。
///
/// 与授权服务 (AuthService) 严格解耦：所有方法都接收一个外部传入的 `accessToken`，
/// token 续期的责任在调用方（`AppSession`）—— 调用前先 `AuthService.refreshIfNeeded()`
/// 拿到 fresh AT 再调本服务，避免账号服务知道 App Attest / Assertion 等授权域细节。
///
/// 当前仅承担登录身份管理（`/user/identities`），未来扩展（账号注销、隐私偏好等）也应集中
/// 进入此服务，使账号域的对外面向保持单一入口。
final class AccountService {

    private let identities: UserIdentitiesClient

    init(identities: UserIdentitiesClient = UserIdentitiesClient()) {
        self.identities = identities
    }

    // MARK: - 身份列表

    /// 列出当前账号已绑定的全部登录身份。
    func listIdentities(accessToken: String) async throws -> [Identity] {
        try await identities.list(accessToken: accessToken)
    }

    // MARK: - 绑定 / 解绑

    /// 在当前账号上绑定手机号。
    ///
    /// - Returns: 服务端确认的新身份记录。规范化的 identities 列表通常需要再调
    ///   `listIdentities` 同步本地缓存。
    @discardableResult
    func bindPhone(
        accessToken: String,
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        try await identities.bindPhone(
            accessToken: accessToken,
            otpTicket: otpTicket,
            otp: otp
        )
    }

    /// 按 `identity_id` 解绑一个已绑定的登录身份。是否允许解绑最后一个登录身份完全由
    /// 服务端策略决定。
    func unbind(accessToken: String, identityId: String) async throws {
        try await identities.unbind(
            accessToken: accessToken,
            identityId: identityId
        )
    }
}
