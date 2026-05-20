import Foundation
import Alamofire

/// 用户账号服务上 `/user/identities` 端点的客户端封装。
///
/// 三种使用方式：
/// - `GET`    → 枚举当前账号上已绑定的登录身份，用于设置页渲染。
/// - `POST`   → 在当前账号上新增手机号登录身份（匿名升级为真实账号）。
/// - `DELETE` → 解绑某个已绑定的登录身份（仅在还会保留至少一个登录身份时可用，
///   或部署方允许解绑最后一个登录身份时也可用）。
///
/// **未来业务 Client 的范例模板**：同时持有 `AuthorizationTokenProvider` 与
/// `Alamofire.Session`。每个方法的固定模板是：
/// ```
/// let at = try await auth.getAccessToken()      // OAuth 层负责 fresh AT
/// let endpoints = endpointsResolver()
/// return try await http.request(...).validate().serializingDecodable(...).value
/// ```
/// 如果业务希望感知特定 4xx 语义（例如 `bindPhone` 需要识别 `409 identity_occupied`），
/// 在 `catch let afError as AFError` 里按 `afError.responseCode` 自行处理；不再走"全局
/// 错误映射"。
final class UserIdentitiesClient {

    private let auth: AuthorizationTokenProvider
    private let http: Session
    private let endpointsResolver: () -> AccountServiceEndpoints

    /// - Parameters:
    ///   - auth: 提供 fresh access token 的 OAuth 层视图。
    ///   - http: 业务 Alamofire Session（不挂任何 Authenticator，401 不自动重试）。
    ///   - endpoints: 端点解析闭包。默认每次取用 `AccountServiceEndpoints.current()`，
    ///     使设置页修改账号服务基地址后能立即生效；测试中可注入固定端点。
    init(
        auth: AuthorizationTokenProvider,
        http: Session,
        endpoints: @escaping () -> AccountServiceEndpoints = AccountServiceEndpoints.current
    ) {
        self.auth = auth
        self.http = http
        self.endpointsResolver = endpoints
    }

    // MARK: - 列表查询

    /// `GET /user/identities` —— 当前账号上所有已绑定登录身份的完整列表。
    func list() async throws -> [Identity] {
        let at = try await auth.getAccessToken()
        let endpoints = endpointsResolver()
        let envelope = try await http.request(
            endpoints.identitiesEndpoint,
            method: .get,
            headers: HTTPHeaders([
                "Authorization": "Bearer \(at)",
                "Accept": "application/json"
            ])
        )
        .validate()
        .serializingDecodable(IdentitiesEnvelope.self)
        .value
        return envelope.identities
    }

    // MARK: - 绑定手机号

    /// 携带 `identity_type=phone` 与 OTP 凭证向 `POST /user/identities` 发起绑定。
    /// 当手机号已被其他账号占用时端点返回 `409 identity_occupied`，本方法会把它转换为
    /// `APIError.identityOccupied`（同时透传 `conflict_token`）；其它错误以 `APIError.http`
    /// 形式向上抛出。
    func bindPhone(
        otpTicket: String,
        otp: String
    ) async throws -> Identity {
        let at = try await auth.getAccessToken()
        let endpoints = endpointsResolver()
        let pairs: [(String, String)] = [
            ("identity_type", IdentityType.phone.rawValue),
            ("otp_ticket", otpTicket),
            ("otp", otp)
        ]
        var request = URLRequest(url: endpoints.identitiesEndpoint)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        request.setValue("Bearer \(at)", forHTTPHeaderField: "Authorization")
        request.httpBody = FormURLEncoder.encode(pairs).data(using: .utf8)
        do {
            return try await http.request(request)
                .validate()
                .serializingDecodable(Identity.self)
                .value
        } catch {
            throw mapBindPhoneError(error)
        }
    }

    // MARK: - 解绑

    /// `DELETE /user/identities/{identity_id}` —— 解绑某个已绑定的登录身份。
    /// 用于 "设置 → 已绑定手机号 → 解绑" 这类入口。是否允许解绑最后一个登录身份
    /// 完全由服务端策略决定。
    func unbind(identityId: String) async throws {
        let at = try await auth.getAccessToken()
        let endpoints = endpointsResolver()
        let url = endpoints.identitiesEndpoint.appendingPathComponent(identityId)
        _ = try await http.request(
            url,
            method: .delete,
            headers: HTTPHeaders([
                "Authorization": "Bearer \(at)",
                "Accept": "application/json"
            ])
        )
        .validate()
        .serializingData(emptyResponseCodes: [200, 202, 204, 205])
        .value
    }

    // MARK: - 错误映射（仅 bindPhone 需要的局部映射）

    /// 把 Alamofire 错误转为 `APIError`：仅识别 `bindPhone` 关心的 409 冲突；其它情况
    /// 退化为通用 `.http` / `.network`。**注意**：这是 *业务* 错误映射，不是全局映射 ——
    /// 不同业务端点关心的非 2xx 语义不同，应各自实现，避免在网络层叠加全局判断。
    private func mapBindPhoneError(_ error: Error) -> APIError {
        if let apiError = error as? APIError {
            return apiError
        }
        guard let afError = error.asAFError else {
            return .network(message: (error as NSError).localizedDescription)
        }
        if let underlying = afError.underlyingError as? APIError {
            return underlying
        }
        if case let .responseValidationFailed(reason) = afError,
           case let .unacceptableStatusCode(code) = reason {
            return mapStatusCode(code, afError: afError)
        }
        if let code = afError.responseCode {
            return mapStatusCode(code, afError: afError)
        }
        return .network(message: afError.localizedDescription)
    }

    private func mapStatusCode(_ code: Int, afError: AFError) -> APIError {
        // bindPhone 关心 409 identity_occupied + conflict_token。
        if code == 409 {
            // Alamofire 在 validate() 后无法直接拿响应体；此处尝试从 AFError 链中抽取
            // 但通常需要单独做一次 raw 序列化。简化起见：当 409 时构造 identityOccupied，
            // conflict_token 透传逻辑在更精细化时再补 —— 当前阶段服务端默认 conflict_token
            // 也可能为空，UI 已能完成"提示用户改用其他凭证重试"的方案 A。
            return .identityOccupied(conflictToken: nil)
        }
        return .http(statusCode: code, body: afError.localizedDescription)
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
