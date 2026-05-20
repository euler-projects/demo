import Foundation

/// 用户账号服务上 `/user/identities` 端点的客户端封装。
///
/// 三种使用方式：
/// - `GET`    → 枚举当前账号上已绑定的登录身份，用于设置页渲染。
/// - `POST`   → 在当前账号上新增手机号登录身份（匿名升级为真实账号）。
/// - `DELETE` → 解绑某个已绑定的登录身份（仅在还会保留至少一个登录身份时可用，
///   或部署方允许解绑最后一个登录身份时也可用）。
///
/// 所有调用都需要带上 `Authorization: Bearer {AT}`。token 续期的"刷新 + 重试"由
/// `AppSession` 在调用前通过 `AuthService.refreshIfNeeded()` 完成 —— 该 client 不感知
/// 也不依赖 OIDC Discovery，体现"账号服务可独立部署"的边界。
final class UserIdentitiesClient {

    private let http: HTTPClient
    private let endpointsResolver: () -> AccountServiceEndpoints

    /// - Parameters:
    ///   - http: HTTP 传输层；测试中可注入 mock。
    ///   - endpoints: 端点解析闭包。默认每次取用 `AccountServiceEndpoints.current()`，
    ///     使设置页修改账号服务基地址后能立即生效；测试中可注入固定端点。
    init(
        http: HTTPClient = .shared,
        endpoints: @escaping () -> AccountServiceEndpoints = AccountServiceEndpoints.current
    ) {
        self.http = http
        self.endpointsResolver = endpoints
    }

    // MARK: - 列表查询

    /// `GET /user/identities` —— 当前账号上所有已绑定登录身份的完整列表。
    func list(accessToken: String) async throws -> [Identity] {
        let endpoints = endpointsResolver()
        let request = http.jsonRequest(
            url: endpoints.identitiesEndpoint,
            method: "GET",
            bearerToken: accessToken
        )
        let envelope = try await http.send(request, as: IdentitiesEnvelope.self)
        return envelope.identities
    }

    // MARK: - 绑定手机号

    /// 携带 `identity_type=phone` 与 OTP 凭证向 `POST /user/identities` 发起绑定。
    /// 当手机号已被其他账号占用时端点返回 `409 identity_occupied`，HTTP 层会将其映射为
    /// `APIError.identityOccupied`。
    func bindPhone(
        accessToken: String,
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        let endpoints = endpointsResolver()
        let pairs: [(String, String)] = [
            ("identity_type", IdentityType.phone.rawValue),
            ("otp_ticket", otpTicket),
            ("otp", otp)
        ]
        let request = http.formRequest(
            url: endpoints.identitiesEndpoint,
            pairs: pairs,
            bearerToken: accessToken
        )
        return try await http.send(request, as: Identity.self)
    }

    // MARK: - 解绑

    /// `DELETE /user/identities/{identity_id}` —— 解绑某个已绑定的登录身份。
    /// 用于 "设置 → 已绑定手机号 → 解绑" 这类入口。是否允许解绑最后一个登录身份
    /// 完全由服务端策略决定。
    func unbind(accessToken: String, identityId: String) async throws {
        let endpoints = endpointsResolver()
        let url = endpoints.identitiesEndpoint.appendingPathComponent(identityId)
        let request = http.jsonRequest(url: url, method: "DELETE", bearerToken: accessToken)
        _ = try await http.sendVoid(request)
    }
}

/// 部分部署会把列表响应包装成 `{ "identities": [...] }`，另一些部署直接返回裸 JSON 数组。
/// 这里的自定义解码器对两种形式都兼容。
private struct IdentitiesEnvelope: Decodable {
    let identities: [Identity]

    init(from decoder: Decoder) throws {
        // 先尝试包装形式。
        if let container = try? decoder.container(keyedBy: AnyCodingKey.self),
           let key = AnyCodingKey(stringValue: "identities"),
           container.contains(key),
           let nested = try? container.decode([Identity].self, forKey: key) {
            self.identities = nested
            return
        }
        // 否则回退到裸数组。
        let single = try decoder.singleValueContainer()
        self.identities = try single.decode([Identity].self)
    }

    private struct AnyCodingKey: CodingKey {
        var stringValue: String
        init?(stringValue: String) { self.stringValue = stringValue }
        var intValue: Int? { nil }
        init?(intValue: Int) { return nil }
    }
}
