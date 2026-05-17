import Foundation
#if DEBUG
import os
#endif

/// 围绕 `URLSession` 的轻量 async 封装。
///
/// 职责刻意收窄：
/// - 用严格的 form / JSON 编码构造请求。
/// - 解码 JSON 成功响应体。
/// - 把非 2xx 响应转换为结构化的 `APIError`（存在 OAuth 错误格式时优先使用）。
///
/// 鉴权相关的细节（401 处理、刷新后重试）由调用方负责，不在这里处理。这样可以让该 client
/// 远离 token 维护逻辑，并让编排逻辑在 `AuthService` 中保持显式可见。
final class HTTPClient {

    static let shared = HTTPClient()

    private let session: URLSession
    private let decoder: JSONDecoder
    private let encoder: JSONEncoder

    init(session: URLSession = .shared) {
        self.session = session

        let dec = JSONDecoder()
        dec.keyDecodingStrategy = .useDefaultKeys
        self.decoder = dec

        let enc = JSONEncoder()
        enc.keyEncodingStrategy = .useDefaultKeys
        self.encoder = enc
    }

    // MARK: - 请求构造

    /// 构造一个 `application/x-www-form-urlencoded` 的 POST 请求。
    /// 使用数组传入 pairs，以便在多次运行间保持稳定的字段顺序。
    func formRequest(
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
    func jsonRequest(
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

    // MARK: - 执行

    /// 执行请求，并将成功响应体解码为 `T`。
    /// 非 2xx 响应在可解析时按 OAuth 错误格式抛出。
    func send<T: Decodable>(_ request: URLRequest, as type: T.Type) async throws -> T {
        let (data, response) = try await perform(request)
        return try decode(type, from: data, response: response)
    }

    /// 执行请求但不解析响应体 —— 用于合法返回 204 空响应的端点。
    @discardableResult
    func sendVoid(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        let (data, response) = try await perform(request)
        try validate(data: data, response: response)
        return (data, response)
    }

    // MARK: - 内部实现

    private func perform(_ request: URLRequest) async throws -> (Data, HTTPURLResponse) {
        #if DEBUG
        HTTPRequestLog.logRequest(request)
        let start = Date()
        #endif
        let data: Data
        let response: URLResponse
        do {
            (data, response) = try await session.data(for: request)
        } catch {
            #if DEBUG
            HTTPRequestLog.logFailure(request: request, error: error)
            #endif
            throw APIError.network(message: (error as NSError).localizedDescription)
        }
        guard let httpResponse = response as? HTTPURLResponse else {
            #if DEBUG
            HTTPRequestLog.logInvalidResponse(request: request)
            #endif
            throw APIError.invalidResponse
        }
        #if DEBUG
        HTTPRequestLog.logResponse(
            httpResponse,
            data: data,
            request: request,
            elapsed: Date().timeIntervalSince(start)
        )
        #endif
        return (data, httpResponse)
    }

    private func decode<T: Decodable>(
        _ type: T.Type, from data: Data, response: HTTPURLResponse
    ) throws -> T {
        try validate(data: data, response: response)
        do {
            return try decoder.decode(T.self, from: data)
        } catch {
            throw APIError.decoding(message: String(describing: error))
        }
    }

    /// 把非 2xx 响应映射为合适的 `APIError`。响应体可被解析为 OAuth 错误格式时优先使用，
    /// 否则回退为裸 `.http` 错误。
    private func validate(data: Data, response: HTTPURLResponse) throws {
        guard !(200...299).contains(response.statusCode) else { return }

        let bodyString = String(data: data, encoding: .utf8) ?? ""
        let oauthError = try? decoder.decode(OAuthErrorPayload.self, from: data)

        // 409 identity_occupied：把 conflict_token 透传给 UI 处理。
        if response.statusCode == 409, oauthError?.error == "identity_occupied" {
            throw APIError.identityOccupied(conflictToken: oauthError?.conflictToken)
        }

        // 启发式判定：服务端明确指出 kid 已失效 / 被吊销时，向上抛出供调用方登出后重新走一次完整
        // 标准登录。参见 App-Attest-Login.md §IV。
        if let oauthError, Self.isKidRevocation(error: oauthError.error) {
            throw APIError.kidRevoked
        }

        if let oauthError {
            throw APIError.oauth(
                error: oauthError.error,
                description: oauthError.errorDescription,
                statusCode: response.statusCode
            )
        }

        throw APIError.http(statusCode: response.statusCode, body: bodyString)
    }

    /// 启发式判定 "当前 kid 已不可信" 的响应。具体错误字符串由服务端实现自定义，这里识别几种
    /// 常见写法，便于脚手架在不等服务端契约变更的情况下做出反应。
    private static func isKidRevocation(error: String) -> Bool {
        let lowered = error.lowercased()
        return lowered.contains("kid_revoked")
            || lowered.contains("attestation_key_revoked")
            || lowered == "invalid_attestation"
    }
}

#if DEBUG
/// 仅 DEBUG 构建启用的请求 / 响应日志器。
///
/// 通过 Apple 统一日志（`os.Logger`）输出，因此在 Xcode 控制台与
/// `subsystem == bundleId && category == HTTPClient` 过滤下的 Console.app 都能看到。
/// Release 构建会被编译剔除；Release 下 `URLSession` 流量保持静默。
///
/// 敏感字段（Authorization 请求头、OAuth token、App Attest assertion / attestation、OTP、
/// challenge、client_data_hash 等）会被脱敏为 "短前缀 + 长度" 形式，使日志在帮助排查请求
/// 形态的同时不泄露真实凭证。
fileprivate enum HTTPRequestLog {

    private static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "com.eulerframework.appattest.scaffold",
        category: "HTTPClient"
    )

    private static let bodyByteLimit = 2048

    /// 需要在日志中脱敏的请求头名称。
    private static let sensitiveHeaderKeys: Set<String> = [
        "authorization",
        "cookie",
        "set-cookie",
        "oauth-client-attestation",
        "oauth-client-attestation-pop"
    ]

    /// 需要在日志中脱敏的请求体字段名称。
    private static let sensitiveBodyKeys: Set<String> = [
        "client_assertion",
        "assertion",
        "attestation",
        "access_token",
        "refresh_token",
        "id_token",
        "otp",
        "password",
        "client_secret",
        "code",
        "challenge",
        "client_data_hash"
    ]

    static func logRequest(_ request: URLRequest) {
        let method = request.httpMethod ?? "GET"
        let url = request.url?.absoluteString ?? "<no url>"
        let headers = redact(headers: request.allHTTPHeaderFields ?? [:])
        let body = request.httpBody.map {
            redact(body: $0, contentType: request.value(forHTTPHeaderField: "Content-Type"))
        } ?? "<empty>"
        logger.debug("→ \(method, privacy: .public) \(url, privacy: .public) | headers={\(headers, privacy: .public)} | body=\(body, privacy: .public)")
    }

    static func logResponse(
        _ response: HTTPURLResponse,
        data: Data,
        request: URLRequest,
        elapsed: TimeInterval
    ) {
        let url = request.url?.absoluteString ?? "<no url>"
        let contentType = response.value(forHTTPHeaderField: "Content-Type")
        let isError = response.statusCode >= 400
        // 错误响应保留原文输出：OAuth/HTTP 错误格式从不会包含凭证，且我们希望每一个字节
        // （error_description、error_uri、trace id）都可见。成功响应仍然走脱敏路径
        // （token 等敏感字段都出现在成功响应体里）。
        let body: String
        if data.isEmpty {
            body = "<empty>"
        } else if isError {
            body = rawBody(data)
        } else {
            body = redact(body: data, contentType: contentType)
        }
        let headers = redact(headers: stringHeaders(from: response))
        let ms = Int(elapsed * 1000)
        let arrow = isError ? "←!" : "←"
        logger.debug("\(arrow, privacy: .public) \(response.statusCode, privacy: .public) \(url, privacy: .public) (\(ms, privacy: .public)ms) | headers={\(headers, privacy: .public)} | body=\(body, privacy: .public)")
    }

    static func logFailure(request: URLRequest, error: Error) {
        let url = request.url?.absoluteString ?? "<no url>"
        let message = (error as NSError).localizedDescription
        logger.error("× \(url, privacy: .public) network error: \(message, privacy: .public)")
    }

    static func logInvalidResponse(request: URLRequest) {
        let url = request.url?.absoluteString ?? "<no url>"
        logger.error("× \(url, privacy: .public) invalid (non-HTTP) response")
    }

    // MARK: - 脱敏工具

    private static func redact(headers: [String: String]) -> String {
        guard !headers.isEmpty else { return "" }
        return headers
            .sorted { $0.key.lowercased() < $1.key.lowercased() }
            .map { key, value in
                if sensitiveHeaderKeys.contains(key.lowercased()) {
                    return "\(key): \(mask(value))"
                }
                return "\(key): \(value)"
            }
            .joined(separator: ", ")
    }

    private static func redact(body: Data, contentType: String?) -> String {
        let truncated = body.prefix(bodyByteLimit)
        let suffix = body.count > bodyByteLimit
            ? "...(+\(body.count - bodyByteLimit) more bytes)"
            : ""
        guard let raw = String(data: truncated, encoding: .utf8) else {
            return "<binary \(body.count) bytes>"
        }
        let ct = (contentType ?? "").lowercased()
        if ct.contains("application/x-www-form-urlencoded") {
            return redactForm(raw) + suffix
        }
        if ct.contains("json") || raw.first == "{" || raw.first == "[" {
            return redactJSON(raw) + suffix
        }
        return raw + suffix
    }

    private static func redactForm(_ body: String) -> String {
        body.split(separator: "&").map { pair -> String in
            let parts = pair.split(separator: "=", maxSplits: 1).map(String.init)
            guard parts.count == 2 else { return String(pair) }
            let key = parts[0]
            let value = parts[1]
            if sensitiveBodyKeys.contains(key.lowercased()) {
                return "\(key)=\(mask(value))"
            }
            return "\(key)=\(value)"
        }.joined(separator: "&")
    }

    /// 基于正则的 JSON value 脱敏。并不是完整 JSON parser，但对调试日志足够，
    /// 且无需为每次请求多解码一次。
    private static func redactJSON(_ body: String) -> String {
        var out = body
        for key in sensitiveBodyKeys {
            let pattern = "(\"\(key)\"\\s*:\\s*\")([^\"]+)(\")"
            guard let regex = try? NSRegularExpression(pattern: pattern, options: [.caseInsensitive]) else {
                continue
            }
            let range = NSRange(out.startIndex..., in: out)
            let matches = regex.matches(in: out, range: range).reversed()
            for match in matches where match.numberOfRanges >= 4 {
                guard let valueRange = Range(match.range(at: 2), in: out) else { continue }
                let masked = mask(String(out[valueRange]))
                out.replaceSubrange(valueRange, with: masked)
            }
        }
        return out
    }

    private static func mask(_ value: String) -> String {
        if value.count <= 8 { return "***" }
        let head = value.prefix(6)
        return "\(head)…(+\(value.count - 6) chars)"
    }

    /// 将 `[AnyHashable: Any]` 形态的响应头字典转为 `[String: String]`，便于复用同一套脱敏
    /// 工具方法。非字符串值通过 `String(describing:)` 字符串化。
    private static func stringHeaders(from response: HTTPURLResponse) -> [String: String] {
        var out: [String: String] = [:]
        for (key, value) in response.allHeaderFields {
            let k = (key as? String) ?? String(describing: key)
            let v = (value as? String) ?? String(describing: value)
            out[k] = v
        }
        return out
    }

    /// 错误响应的原文输出，沿用与脱敏路径相同的字节上限。
    private static func rawBody(_ data: Data) -> String {
        let truncated = data.prefix(bodyByteLimit)
        let suffix = data.count > bodyByteLimit
            ? "...(+\(data.count - bodyByteLimit) more bytes)"
            : ""
        guard let raw = String(data: truncated, encoding: .utf8) else {
            return "<binary \(data.count) bytes>"
        }
        return raw + suffix
    }
}
#endif
