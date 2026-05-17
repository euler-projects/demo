import Foundation

/// `/user/identities` —— 账号绑定端点的客户端封装。
///
/// 脚手架共有三种使用方式：
/// - `GET`    → 枚举当前账号上已绑定的 factor，用于设置页渲染。
/// - `POST`   → 在当前账号上新增手机号 factor（匿名升级为真实账号）。
/// - `DELETE` → 解绑某个已绑定的 factor（仅在还会保留至少一个 factor 时可用，
///   或部署方允许解绑最后一个 factor 时也可用）。
///
/// 所有调用都需要带上 `Authorization: Bearer {AT}`。401 时 "刷新 + 重试" 由
/// `AuthService` 负责，不由该 client 处理。
final class UserIdentitiesClient {

    private let http: HTTPClient
    private let discovery: OIDCDiscoveryService

    init(http: HTTPClient = .shared, discovery: OIDCDiscoveryService = .shared) {
        self.http = http
        self.discovery = discovery
    }

    // MARK: - 列表查询

    /// `GET /user/identities` —— 当前账号上所有已绑定 factor 的完整列表。
    func list(accessToken: String) async throws -> [Identity] {
        let endpoints = await discovery.endpoints()
        let request = http.jsonRequest(
            url: endpoints.userIdentitiesEndpoint,
            method: "GET",
            bearerToken: accessToken
        )
        let envelope = try await http.send(request, as: IdentitiesEnvelope.self)
        return envelope.identities
    }

    // MARK: - 绑定手机号

    /// 携带 `factor_type=phone` 与 OTP 凭证向 `POST /user/identities` 发起绑定。
    /// 当手机号已被其他账号占用时端点返回 `409 factor_occupied`，HTTP 层会将其映射为
    /// `APIError.factorOccupied`。
    func bindPhone(
        accessToken: String,
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        let endpoints = await discovery.endpoints()
        let pairs: [(String, String)] = [
            ("factor_type", FactorType.phone.rawValue),
            ("otp_ticket", otpTicket),
            ("otp", otp)
        ]
        let request = http.formRequest(
            url: endpoints.userIdentitiesEndpoint,
            pairs: pairs,
            bearerToken: accessToken
        )
        return try await http.send(request, as: Identity.self)
    }

    // MARK: - 解绑

    /// `DELETE /user/identities/{factor_id}` —— 解绑某个已绑定的 factor。
    /// 用于 "设置 → 已绑定手机号 → 解绑" 这类入口。是否允许解绑最后一个 factor
    /// 完全由服务端策略决定。
    func unbind(accessToken: String, factorId: String) async throws {
        let endpoints = await discovery.endpoints()
        let url = endpoints.userIdentitiesEndpoint.appendingPathComponent(factorId)
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
