import Foundation

/// 网络层 / 鉴权层向上抛出的所有错误。
///
/// `LocalizedError` 描述可直接用于 UI 展示。需要按错误类型分发处理时使用结构化 case，
/// 例如 `.identityOccupied` 触发 UI 弹窗、`.kidRevoked` 强制登出。
enum APIError: LocalizedError, Equatable {

    /// URLSession 层网络错误（无响应、DNS 错误、TLS 错误等）。
    case network(message: String)

    /// 服务器有响应，但响应体无法解码为期望的 schema。
    case decoding(message: String)

    /// 响应缺失或畸形（例如没有 `HTTPURLResponse`）。
    case invalidResponse

    /// 标准 OAuth 2.0 / OAuth 2.1 错误响应（`{ "error": ..., "error_description": ... }`）。
    case oauth(error: String, description: String?, statusCode: Int)

    /// 非 2xx 且不属于 OAuth 错误格式的通用响应。
    case http(statusCode: Int, body: String)

    /// `POST /user/identities` 返回 `409 identity_occupied` —— UI 应提示用户改用其他凭证
    /// 重试（方案 A）；方案 B（Conflict Token 换绑）不在脚手架范畴。
    case identityOccupied(conflictToken: String?)

    /// Apple App Attest key 已被服务器吊销（例如风控信号）。当前会话不可恢复，
    /// 调用方必须登出并强制走一次完整的标准登录。
    case kidRevoked

    /// 当前设备不支持 App Attest（较旧的 iPad、未使用 Apple silicon 的模拟器等）。
    case appAttestNotSupported

    /// 本地尚未注册 `kid` —— 没有它，刷新 / 绑定流程都无法继续。
    case noRegisteredKey

    /// 运行时配置非法（issuer URL 畸形等）。
    case invalidConfiguration(message: String)

    var errorDescription: String? {
        switch self {
        case .network(let message):
            return "网络错误: \(message)"
        case .decoding(let message):
            return "响应解析失败: \(message)"
        case .invalidResponse:
            return "服务器返回无效响应"
        case .oauth(let error, let description, let statusCode):
            if let description, !description.isEmpty {
                return "[\(statusCode)] \(error): \(description)"
            }
            return "[\(statusCode)] \(error)"
        case .http(let statusCode, let body):
            return "[\(statusCode)] \(body)"
        case .identityOccupied:
            return "该手机号已绑定其他账号, 请更换一个手机号"
        case .kidRevoked:
            return "设备凭证已被服务器吊销, 请重新登录"
        case .appAttestNotSupported:
            return "当前设备不支持 App Attest"
        case .noRegisteredKey:
            return "本地无可用的设备凭证 (kid)"
        case .invalidConfiguration(let message):
            return "配置错误: \(message)"
        }
    }
}

/// 标准 OAuth 2 错误响应载荷，用于解码 token 端点的 4xx/5xx 响应体。
struct OAuthErrorPayload: Decodable {
    let error: String
    let errorDescription: String?
    let conflictToken: String?

    enum CodingKeys: String, CodingKey {
        case error
        case errorDescription = "error_description"
        case conflictToken = "conflict_token"
    }
}
