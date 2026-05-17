import Foundation

/// `POST /otp/tickets` 的客户端封装。
///
/// 该端点对业务通用 —— 同一个调用既用于标准登录的 OTP，也用于账号绑定流程的 OTP。
/// 两类用途由服务端通过 `purpose` 字段区分：
/// - 登录用 ticket（搭配 `grant_type=otp` 使用）省略 `purpose`。
/// - 绑定用 ticket（最终在 `POST /user/identities` 兑换）传入 `purpose=sign_in`，
///   或部署方约定的其他绑定专用值。
///
/// 响应中 `retry_after` 用于节流二次发送，UI 应当尊重；脚手架将其作为 `OTPTicket` 上的
/// 强类型属性向上层暴露。
final class OTPClient {

    private let http: HTTPClient
    private let discovery: OIDCDiscoveryService

    init(http: HTTPClient = .shared, discovery: OIDCDiscoveryService = .shared) {
        self.http = http
        self.discovery = discovery
    }

    /// 触发一次 OTP 投递。返回的 ticket 必须在用户填入验证码后回传给服务端。
    /// - Parameters:
    ///   - channel: `"sms"` 表示短信，`"email"` 表示邮件。脚手架当前固定使用 `"sms"`。
    ///   - recipient: E.164 格式手机号（`+8613...`）或邮箱地址。
    ///   - purpose: 可选的业务用途标识。普通登录传 `nil` 即可。
    func sendOTP(
        channel: String,
        recipient: String,
        purpose: String? = nil
    ) async throws -> OTPTicket {
        let endpoints = await discovery.endpoints()
        var pairs: [(String, String)] = [
            ("channel", channel),
            ("recipient", recipient)
        ]
        if let purpose, !purpose.isEmpty {
            pairs.append(("purpose", purpose))
        }
        let request = http.formRequest(url: endpoints.otpTicketsEndpoint, pairs: pairs)
        return try await http.send(request, as: OTPTicket.self)
    }
}

/// `/otp/tickets` 响应的解码模型。
struct OTPTicket: Decodable, Equatable {
    /// 不透明 ticket 句柄。一次性使用，与 OTP 同步过期。
    let otpTicket: String
    /// OTP 有效期（秒），驱动 UI 倒计时。
    let expiresIn: Int
    /// 同一调用方再次申请 ticket 前必须等待的最短间隔。即便更换 recipient 也不会重置该节流。
    let retryAfter: Int

    enum CodingKeys: String, CodingKey {
        case otpTicket = "otp_ticket"
        case expiresIn = "expires_in"
        case retryAfter = "retry_after"
    }
}
