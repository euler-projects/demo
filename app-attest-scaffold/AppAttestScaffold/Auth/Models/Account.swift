import Foundation

/// `Account` 表示与会话 token 一同持久化在 Keychain 中、与账号绑定的稳定数据。
///
/// 结构对应 App-Attest-Login.md §2.2，仅做了两处务实折中：
/// - `profile.nickname` 与 `profile.avatarUrl` 设为可选，因为 userinfo 接口当前不返回；
///   UI 会回退使用 `username` 与内置 SF Symbol。
/// - `appAttestKey.iat` 由客户端在 attestation 时刻记录（服务端不会回显），其精度对于
///   "显示设备注册时间" 这类 UX 已足够。
struct Account: Codable, Equatable {

    var profile: Profile
    var appAttestKey: AppAttestKey?
    var identities: [Identity]

    struct Profile: Codable, Equatable {
        var username: String
        var nickname: String?
        var avatarUrl: String?

        enum CodingKeys: String, CodingKey {
            case username
            case nickname
            case avatarUrl = "avatar_url"
        }

        /// 尽力而为的展示名。按脚手架规范，永远以 `username` 作为兜底
        /// （userinfo 接口目前不返回 `nickname`）。
        var displayName: String {
            if let nickname, !nickname.isEmpty { return nickname }
            return username
        }
    }

    struct AppAttestKey: Codable, Equatable {
        var kid: String
        var iat: Date

        enum CodingKeys: String, CodingKey {
            case kid
            case iat
        }
    }

    enum CodingKeys: String, CodingKey {
        case profile
        case appAttestKey = "app_attest_key"
        case identities
    }

    /// 便捷取值：已绑定的手机号 identity（若有）。脚手架仅处理 `phone` 一种 factor。
    var phoneIdentity: Identity? {
        identities.first { $0.factorType == .phone }
    }
}
