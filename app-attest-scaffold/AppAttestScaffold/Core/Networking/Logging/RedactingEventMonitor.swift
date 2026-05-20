import Foundation
import Alamofire
#if DEBUG
import os
#endif

/// 注入 Alamofire `Session` 的请求 / 响应日志器。
///
/// 通过 Apple 统一日志（`os.Logger`）输出，因此在 Xcode 控制台与
/// `subsystem == bundleId && category == HTTPClient` 过滤下的 Console.app 都能看到。
/// Release 构建会被编译剔除；Release 下 Alamofire 流量保持静默。
///
/// 敏感字段（Authorization 请求头、OAuth token、App Attest assertion / attestation、OTP、
/// challenge、client_data_hash 等）会被脱敏为 "短前缀 + 长度" 形式，使日志在帮助排查请求
/// 形态的同时不泄露真实凭证。错误响应（4xx/5xx）保留原文输出 —— OAuth/HTTP 错误格式从不
/// 包含凭证，且 `error_description` / 服务端 trace id 等在排查时必须可见。
final class RedactingEventMonitor: EventMonitor {

    let queue = DispatchQueue(label: "com.eulerframework.appattest.scaffold.http.log", qos: .utility)

    func requestDidResume(_ request: Request) {
        #if DEBUG
        guard let urlRequest = request.lastRequest ?? request.firstRequest else { return }
        Self.logRequest(urlRequest)
        #endif
    }

    func request(_ request: DataRequest, didParseResponse response: DataResponse<Data?, AFError>) {
        #if DEBUG
        switch response.result {
        case .success:
            if let httpResponse = response.response, let urlRequest = request.lastRequest ?? request.firstRequest {
                Self.logResponse(
                    httpResponse,
                    data: response.data ?? Data(),
                    request: urlRequest,
                    elapsed: response.metrics?.taskInterval.duration ?? 0
                )
            }
        case let .failure(error):
            if let urlRequest = request.lastRequest ?? request.firstRequest {
                if let httpResponse = response.response {
                    Self.logResponse(
                        httpResponse,
                        data: response.data ?? Data(),
                        request: urlRequest,
                        elapsed: response.metrics?.taskInterval.duration ?? 0
                    )
                } else {
                    Self.logFailure(request: urlRequest, error: error.underlyingError ?? error)
                }
            }
        }
        #endif
    }
}

#if DEBUG
fileprivate extension RedactingEventMonitor {

    static let logger = Logger(
        subsystem: Bundle.main.bundleIdentifier ?? "com.eulerframework.appattest.scaffold",
        category: "HTTPClient"
    )

    static let bodyByteLimit = 2048

    /// 需要在日志中脱敏的请求头名称。
    static let sensitiveHeaderKeys: Set<String> = [
        "authorization",
        "cookie",
        "set-cookie",
        "oauth-client-attestation",
        "oauth-client-attestation-pop"
    ]

    /// 需要在日志中脱敏的请求体字段名称。
    static let sensitiveBodyKeys: Set<String> = [
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

    // MARK: - 脱敏工具

    static func redact(headers: [String: String]) -> String {
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

    static func redact(body: Data, contentType: String?) -> String {
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

    static func redactForm(_ body: String) -> String {
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
    static func redactJSON(_ body: String) -> String {
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

    static func mask(_ value: String) -> String {
        if value.count <= 8 { return "***" }
        let head = value.prefix(6)
        return "\(head)…(+\(value.count - 6) chars)"
    }

    /// 将 `[AnyHashable: Any]` 形态的响应头字典转为 `[String: String]`，便于复用同一套脱敏
    /// 工具方法。非字符串值通过 `String(describing:)` 字符串化。
    static func stringHeaders(from response: HTTPURLResponse) -> [String: String] {
        var out: [String: String] = [:]
        for (key, value) in response.allHeaderFields {
            let k = (key as? String) ?? String(describing: key)
            let v = (value as? String) ?? String(describing: value)
            out[k] = v
        }
        return out
    }

    /// 错误响应的原文输出，沿用与脱敏路径相同的字节上限。
    static func rawBody(_ data: Data) -> String {
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
