import Foundation
import Alamofire

/// `POST /otp/tickets` 的客户端封装。
///
/// 该端点对业务通用 —— 同一个调用既用于标准登录的 OTP，也用于账号绑定流程的 OTP。
/// 脚手架不区分业务用途，统一省略 `purpose`，服务端使用默认行为。
///
/// 响应中 `retry_after` 用于节流二次发送，UI 应当尊重；脚手架将其作为 `OTPTicket` 上的
/// 强类型属性向上层暴露。
final class OTPClient {

    private let http: Session
    private let endpoints: AuthorizationEndpointsProvider

    init(
        http: Session,
        endpoints: AuthorizationEndpointsProvider = .shared
    ) {
        self.http = http
        self.endpoints = endpoints
    }

    /// 触发一次 OTP 投递。返回的 ticket 必须在用户填入验证码后回传给服务端。
    /// - Parameters:
    ///   - channel: `"sms"` 表示短信，`"email"` 表示邮件。脚手架当前固定使用 `"sms"`。
    ///   - recipient: E.164 格式手机号（`+8613...`）或邮箱地址。
    func sendOTP(
        channel: String,
        recipient: String
    ) async throws -> OTPTicket {
        let resolved = await endpoints.endpoints()
        let pairs: [(String, String)] = [
            ("channel", channel),
            ("recipient", recipient)
        ]
        let request = OAuthRequestBuilder.formRequest(url: resolved.otpTicketsEndpoint, pairs: pairs)
        return try await OAuthTransport.send(request, on: http)
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
