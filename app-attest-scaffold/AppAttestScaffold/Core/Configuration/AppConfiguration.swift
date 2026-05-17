import Foundation

/// 脚手架的静态配置项。
///
/// 任何随环境变化的取值（issuer、默认 scope、OTP purpose 等）都集中在这里，
/// 避免下游服务硬编码字符串。运行时唯一可变的是 issuer，它持久化在 `UserDefaults`
/// 中，并可在应用内的设置页编辑。
enum AppConfiguration {

    /// 首次启动时使用的默认 OAuth 2.1 issuer。
    /// 运行时可通过 `AppConfiguration.issuer = ...` 覆盖（持久化在 UserDefaults）。
    static let defaultIssuer = "https://uc.kidostory.com"

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

    /// 调用 `POST /otp/tickets` 时使用的 purpose，表示该 OTP 将被 `/oauth2/token` 用于登录。
    static let otpPurposeSignIn = "sign_in"

    /// 通过 SMS 投递 OTP 的渠道标识。
    static let otpChannelSMS = "sms"

    // MARK: - 持久化的 issuer

    private static let issuerDefaultsKey = "com.eulerframework.scaffold.issuer"

    /// 授权服务器的 issuer URL。持久化在 UserDefaults 中；按脚手架规范在登出后保留
    /// （详见 App-Attest-Login.md §5.1）。
    static var issuer: String {
        get { UserDefaults.standard.string(forKey: issuerDefaultsKey) ?? defaultIssuer }
        set {
            let trimmed = newValue.trimmingCharacters(in: .whitespacesAndNewlines)
            UserDefaults.standard.set(trimmed, forKey: issuerDefaultsKey)
        }
    }

    /// 去除尾部斜杠后的 issuer，可直接用于路径拼接。
    static var issuerBase: String {
        var result = issuer
        while result.hasSuffix("/") { result.removeLast() }
        return result
    }
}
