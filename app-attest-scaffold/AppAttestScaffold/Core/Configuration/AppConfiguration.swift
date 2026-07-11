import Foundation

/// 脚手架的静态配置项。
///
/// 任何随环境变化的取值（issuer、Account Service base、默认 scope 等）
/// 都集中在这里，避免下游服务硬编码字符串。运行时可变的是 issuer 与
/// `accountServiceBaseURL`，它们各自持久化在 `UserDefaults` 中，并可在应用内的
/// 「服务配置」弹框编辑。
enum AppConfiguration {

    /// 首次启动时使用的默认 OAuth 2.1 issuer。
    /// 运行时可通过 `AppConfiguration.issuer = ...` 覆盖（持久化在 UserDefaults）。
    static let defaultIssuer = "https://auth.example.com"

    /// 首次启动时使用的默认用户账号服务 (Account Service) 基地址。
    /// `/user/identities` 等账号身份管理接口由账号服务提供，与授权服务可能部署在不同域名。
    /// demo 环境与授权服务合部署，因此默认值与 `defaultIssuer` 一致；客户端实现仍
    /// 将二者作为独立配置项维护，详见 App-Attest-Login.md §1.x。
    static let defaultAccountServiceBaseURL = "https://auth.example.com"

    /// 每次 `/oauth2/token` 请求默认携带的 scope。
    /// 必须包含 `openid`，服务端才会返回带 `sub`（即 username）的 `id_token`。
    static let defaultScope = "openid"

    /// `OAuth-Client-Attestation-Type` 请求头取值，对应 Apple App Attest。
    static let attestationType = "apple_app_attest"

    /// `urn:ietf:params:oauth:grant-type:app_assertion` —— 既用于匿名注册（携带
    /// `attestation`），也用于例行的 token 刷新（携带 `assertion`）。
    static let appAssertionGrantType = "urn:ietf:params:oauth:grant-type:app_assertion"

    /// 自定义 grant type，用于基于 OTP 的标准登录（携带 `attestation`，包含设备注册流程）。
    static let otpGrantType = "otp"

    /// 通过 SMS 投递 OTP 的渠道标识。
    static let otpChannelSMS = "sms"

    // MARK: - 持久化的 issuer

    /// 授权服务器的 issuer URL。持久化在 AppDataStore 中；抹掉所有数据后回退到 defaultIssuer。
    static var issuer: String {
        get { AppDataStore.get(String.self, for: AppDataStore.Keys.issuer, default: defaultIssuer) }
        set {
            let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
            try? AppDataStore.set(trimmed, for: AppDataStore.Keys.issuer, lifecycle: .persistent, secret: false)
        }
    }

    /// 用户账号服务 (Account Service) 的基地址。持久化在 AppDataStore 中；抹掉所有数据后回退到 defaultAccountServiceBaseURL。
    static var accountServiceBaseURL: String {
        get { AppDataStore.get(String.self, for: AppDataStore.Keys.accountServiceBase, default: defaultAccountServiceBaseURL) }
        set {
            let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
            try? AppDataStore.set(trimmed, for: AppDataStore.Keys.accountServiceBase, lifecycle: .persistent, secret: false)
        }
    }

    /// 去除尾部斜杠后的 issuer，可直接用于路径拼接。
    static var issuerBase: String {
        return stripTrailingSlash(issuer)
    }

    /// 去除尾部斜杠后的账号服务基地址，可直接用于路径拼接。
    static var accountServiceBase: String {
        return stripTrailingSlash(accountServiceBaseURL)
    }

    private static func stripTrailingSlash(_ s: String) -> String {
        var result = s
        while result.hasSuffix("/") { result.removeLast() }
        return result
    }
}
