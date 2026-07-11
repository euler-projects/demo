import Foundation

/// 与「用户账号服务」(Account Service) 交互的高层封装。
///
/// 与授权服务 (`AuthorizationFacade`) 严格解耦：本服务的方法签名不出现 `accessToken`，
/// token 续期由 `AuthorizationFacade.getAccessToken()` 自闭环处理 —— 业务 Client
/// (`UserIdentitiesClient`) 在每个请求开头自取 fresh AT，账号服务因此完全不感知
/// App Attest / Assertion 等授权域细节。
///
/// 当前仅承担登录身份管理（`/user/identities`），未来扩展（账号注销、隐私偏好等）也应集中
/// 进入此服务，使账号域的对外面向保持单一入口。
final class AccountService {

    private let identities: UserIdentitiesClient

    /// - Parameter identities: 业务 Client，需由调用方注入（持有相同的 `auth` + `http`
    ///   组合）；不再提供无参便捷构造，避免在内部偷偷新建一个隔绝于 `AppSession` 装配栈
    ///   的 Alamofire Session。
    init(identities: UserIdentitiesClient) {
        self.identities = identities
    }

    // MARK: - 身份列表

    /// 列出当前账号已绑定的全部登录身份。
    func listIdentities() async throws -> [Identity] {
        try await identities.list()
    }

    // MARK: - 绑定 / 解绑

    /// 在当前账号上绑定手机号。
    ///
    /// - Returns: 服务端确认的新身份记录。规范化的 identities 列表通常需要再调
    ///   `listIdentities` 同步本地缓存。
    @discardableResult
    func bindPhone(
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        try await identities.bindPhone(
            otpTicket: otpTicket,
            otp: otp
        )
    }

    /// 按 `identity_id` 解绑一个已绑定的登录身份。是否允许解绑最后一个登录身份完全由
    /// 服务端策略决定。
    func unbind(identityId: String) async throws {
        try await identities.unbind(identityId: identityId)
    }

    // MARK: - 换绑

    /// 原子换绑手机号：通过 `PUT /user/identities/{identity_id}` 在同一 identity 下更新绑定。
    /// `identity_id` 保持不变，`subject` 会随新手机号变化。
    @discardableResult
    func rebindPhone(
        identityId: String,
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        try await identities.updatePhone(
            identityId: identityId,
            otpTicket: otpTicket,
            otp: otp
        )
    }
}
