import Foundation

/// 把 OAuth 2.0 / OAuth 2.1 标准错误响应载荷翻译为 `APIError`。
///
/// 仅 OAuth 域使用：`AuthorizationFacade` 续期路径与 OAuth 端点客户端
/// (`OAuthClient` / `OTPClient` / `UserInfoClient`) 内部解析 token / userinfo
/// 端点的非 2xx 响应时调用。业务 Client（`UserIdentitiesClient` 等）不再
/// 依赖该工具 —— 业务 4xx/5xx 由各 Client 按自己的语义处理。
enum OAuthErrorMapper {

    /// 把 `(Data, HTTPURLResponse)` 翻译为 `APIError`。
    /// - Returns: 2xx 时返回 `nil`，否则返回结构化错误。
    static func map(data: Data, response: HTTPURLResponse) -> APIError? {
        guard !(200...299).contains(response.statusCode) else { return nil }

        let bodyString = String(data: data, encoding: .utf8) ?? ""
        let oauthError = try? JSONDecoder().decode(OAuthErrorPayload.self, from: data)

        // 409 identity_occupied：把 conflict_token 透传给 UI 处理。
        if response.statusCode == 409, oauthError?.error == "identity_occupied" {
            return .identityOccupied(conflictToken: oauthError?.conflictToken)
        }

        // 启发式判定：服务端明确指出 kid 已失效 / 被吊销时，向上抛出供调用方登出后重新走一次完整
        // 标准登录。参见 App-Attest-Login.md §IV。
        if let oauthError, isKidRevocation(error: oauthError.error) {
            return .kidRevoked
        }

        if let oauthError {
            return .oauth(
                error: oauthError.error,
                description: oauthError.errorDescription,
                statusCode: response.statusCode
            )
        }

        return .http(statusCode: response.statusCode, body: bodyString)
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
