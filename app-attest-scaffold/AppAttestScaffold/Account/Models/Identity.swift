import Foundation

/// 当前账号上已绑定的一个登录身份（`phone`、`email`、`wechat` ……）。
///
/// 由用户账号服务 (Account Service) 的 `/user/identities` 接口下发。脚手架会解析返回的所有
/// 元素，但 UI 中仅渲染并支持编辑 `phone` 这一种登录身份；其他类型作为不透明条目透传，留待
/// 后续迭代加入支持时不必触动持久化层。
struct Identity: Codable, Equatable, Identifiable {

    /// 服务端分配的 UUID。在重新绑定后保持稳定 —— 用于 `PUT/DELETE /user/identities/{id}`。
    var identityId: String
    var identityType: IdentityType
    /// 登录身份的确定性唯一标识。由服务端根据 `identityType` 与绑定凭据自动生成，
    /// 在 `(identityType, subject)` 维度全局唯一；换绑后 `subject` 会随之变化。
    var subject: String
    /// 首次绑定时间，单位毫秒（自 epoch）。
    var boundAt: Int64

    /// 用于展示的脱敏后手机号（例如 `+86138*****00`）。
    /// 服务端已返回隐私安全的字符串，客户端原样保存。
    var phone: String?
    /// 用于展示的脱敏后邮箱。脚手架 UI 暂未使用，保留以便后续扩展。
    var email: String?

    var id: String { identityId }

    enum CodingKeys: String, CodingKey {
        case identityId = "identity_id"
        case identityType = "identity_type"
        case subject
        case boundAt = "bound_at"
        case phone
        case email
    }

    var boundAtDate: Date {
        Date(timeIntervalSince1970: TimeInterval(boundAt) / 1000)
    }
}

/// 平台已知的登录身份类型。采用宽容解码：未知的未来类型不会让 identities 列表的解析整体失败，
/// 而是被收敛为 `.unknown(rawValue)`。
enum IdentityType: Codable, Equatable, Hashable {
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
