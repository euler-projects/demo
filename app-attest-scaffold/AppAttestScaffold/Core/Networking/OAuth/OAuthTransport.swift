import Foundation
import Alamofire

/// OAuth 域端点客户端共用的请求构造与传输 helper。
///
/// 仅 `OAuthClient` / `OTPClient` / `UserInfoClient` 使用：它们的请求体均为
/// `application/x-www-form-urlencoded`（按 RFC 3986 严格百分号编码），响应体
/// 为 `application/json`，错误响应使用 OAuth 标准格式（`{ "error": ..., "error_description": ... }`）
/// —— 这一组合相对固定，因此在域内封装一组 helper，避免每个端点客户端重复样板。
///
/// 业务 Client（`UserIdentitiesClient` 等）不使用本 helper，它们直接调用
/// `Alamofire.Session` 拼装请求，响应错误按各自语义处理。
enum OAuthRequestBuilder {

    /// 构造一个 `application/x-www-form-urlencoded` 的 POST 请求。
    /// `pairs` 使用数组以保留稳定字段顺序（便于调试与防重放 nonce 哈希）。
    static func formRequest(
        url: URL,
        method: String = "POST",
        pairs: [(String, String)],
        bearerToken: String? = nil,
        extraHeaders: [String: String] = [:]
    ) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let bearerToken {
            request.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        }
        for (key, value) in extraHeaders {
            request.setValue(value, forHTTPHeaderField: key)
        }
        request.httpBody = FormURLEncoder.encode(pairs).data(using: .utf8)
        return request
    }

    /// 构造一个 JSON GET（或其它 method）请求。
    static func jsonRequest(
        url: URL,
        method: String = "GET",
        bearerToken: String? = nil,
        extraHeaders: [String: String] = [:]
    ) -> URLRequest {
        var request = URLRequest(url: url)
        request.httpMethod = method
        request.setValue("application/json", forHTTPHeaderField: "Accept")
        if let bearerToken {
            request.setValue("Bearer \(bearerToken)", forHTTPHeaderField: "Authorization")
        }
        for (key, value) in extraHeaders {
            request.setValue(value, forHTTPHeaderField: key)
        }
        return request
    }
}

/// OAuth 域请求传输 helper：发请求 → 拿原始响应 → 用 `OAuthErrorMapper` 校验
/// → 解码为 `T`。仅供 OAuth 端点客户端内部使用。
enum OAuthTransport {

    /// 发送请求并将 2xx 响应解码为 `T`；非 2xx 通过 `OAuthErrorMapper.map` 翻译为
    /// `APIError` 抛出。Alamofire 自身的错误（网络不可达等）映射为 `.network` /
    /// `.invalidResponse`。
    static func send<T: Decodable>(
        _ request: URLRequest,
        on session: Session,
        decoder: JSONDecoder = JSONDecoder()
    ) async throws -> T {
        let (data, response) = try await raw(request, on: session)
        if let mapped = OAuthErrorMapper.map(data: data, response: response) {
            throw mapped
        }
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decoding(message: String(describing: error))
        }
    }

    /// 发送请求并仅做 OAuth 错误映射；返回原始 `(Data, HTTPURLResponse)`。
    /// 用于明确不需要解码 body 的端点（例如 204 / DELETE 端点）。
    @discardableResult
    static func sendVoid(
        _ request: URLRequest,
        on session: Session
    ) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await raw(request, on: session)
        if let mapped = OAuthErrorMapper.map(data: data, response: response) {
            throw mapped
        }
        return (data, response)
    }

    /// 发送请求并返回原始 `(Data, HTTPURLResponse)`，不做任何 2xx / 错误映射。
    /// 把 Alamofire 的错误归并为 `APIError.network` / `APIError.invalidResponse`，
    /// 便于上层区分「未到达服务」与「到达后返回错误」。
    private static func raw(
        _ request: URLRequest,
        on session: Session
    ) async throws -> (Data, HTTPURLResponse) {
        let response = await session.request(request)
            .serializingData(emptyResponseCodes: [200, 204, 205])
            .response
        if let httpResponse = response.response {
            let data = (try? response.result.get()) ?? response.data ?? Data()
            return (data, httpResponse)
        }
        if let underlying = response.error?.underlyingError {
            throw APIError.network(message: (underlying as NSError).localizedDescription)
        }
        if let afError = response.error {
            throw APIError.network(message: afError.localizedDescription)
        }
        throw APIError.invalidResponse
    }
}
