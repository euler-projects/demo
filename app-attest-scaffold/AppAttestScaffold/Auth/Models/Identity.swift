import Foundation

/// 当前账号上已绑定的一个鉴权 factor（`phone`、`email`、`wechat` ……）。
///
/// 脚手架会解析 `GET /user/identities` 返回的所有元素结构，但 UI 中仅渲染并支持编辑
/// `phone` 这一种 factor；其他类型作为不透明条目透传，留待后续迭代加入支持时不必触动
/// 持久化层。
struct Identity: Codable, Equatable, Identifiable {

    /// 服务端分配的 UUID。在重新绑定后保持稳定 —— 用于 `DELETE /user/identities/{id}`。
    var factorId: String
    var factorType: FactorType
    /// 已脱敏的标识符（按规范，是原始 phone/email/openid 的 SHA-256）。
    /// 客户端按不透明字符串处理。
    var identifier: String
    /// 首次绑定时间，单位毫秒（自 epoch）。
    var boundAt: Int64
    /// 最近一次成功验证时间，单位毫秒（自 epoch）。
    var lastVerifiedAt: Int64

    /// 用于展示的脱敏后手机号（例如 `+86138*****00`）。
    /// 服务端已返回隐私安全的字符串，客户端原样保存。
    var phone: String?
    /// 用于展示的脱敏后邮箱。脚手架 UI 暂未使用，保留以便后续扩展。
    var email: String?

    var id: String { factorId }

    enum CodingKeys: String, CodingKey {
        case factorId = "factor_id"
        case factorType = "factor_type"
        case identifier
        case boundAt = "bound_at"
        case lastVerifiedAt = "last_verified_at"
        case phone
        case email
    }

    var boundAtDate: Date {
        Date(timeIntervalSince1970: TimeInterval(boundAt) / 1000)
    }

    var lastVerifiedAtDate: Date {
        Date(timeIntervalSince1970: TimeInterval(lastVerifiedAt) / 1000)
    }
}

/// 平台已知的 factor 类型。采用宽容解码：未知的未来类型不会让 identities 列表的解析整体失败，
/// 而是被收敛为 `.unknown(rawValue)`。
enum FactorType: Codable, Equatable, Hashable {
    case phone
    case email
    case wechat
    case unknown(String)

    var rawValue: String {
        switch self {
        case .phone: return "phone"
        case .email: return "email"
        case .wechat: return "wechat"
        case .unknown(let raw): return raw
        }
    }

    init(from decoder: Decoder) throws {
        let container = try decoder.singleValueContainer()
        let raw = try container.decode(String.self)
        switch raw {
        case "phone": self = .phone
        case "email": self = .email
        case "wechat": self = .wechat
        default: self = .unknown(raw)
        }
    }

    func encode(to encoder: Encoder) throws {
        var container = encoder.singleValueContainer()
        try container.encode(rawValue)
    }
}
