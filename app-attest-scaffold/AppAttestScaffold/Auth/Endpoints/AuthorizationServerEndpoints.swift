import Foundation
#if DEBUG
import os
#endif

/// 授权服务（OAuth 2.1 Authorization Server）所暴露的端点集合。
///
/// 解析策略采用 OIDC Discovery + 静态派生混合：
/// - **OIDC 标准端点** (`token`、`userinfo`) 优先采用 Discovery 公布的取值；缺失时基于
///   issuer 回退到惯例路径，保证服务端尚未暴露 Discovery 时仍可登录。
/// - **平台扩展端点** (`oauth2/challenge`、`otp/tickets`) 不在任何 RFC 中定义，始终基于
///   issuer 派生。
///
/// 该结构不涉及用户账号服务，所有 `/user/...` 路径见 `AccountServiceEndpoints`。
struct AuthorizationServerEndpoints: Equatable {

    /// 当前生效的 issuer（已规范化为去尾部斜杠形式）。
    let issuer: String
    /// `POST` —— 申请一次性 challenge，用于 App Assertion / OTP 登录。
    let challengeEndpoint: URL
    /// `POST` —— 用 grant 凭证换取 access token。
    let tokenEndpoint: URL
    /// `GET` —— 拉取已认证用户的 userinfo（至少包含 `sub`）。
    let userinfoEndpoint: URL
    /// `POST` —— 申请一张 OTP ticket（触发 SMS / 邮件投递）。
    let otpTicketsEndpoint: URL

    /// 单元测试 / 内部使用：直接基于 issuer 与可选 metadata 构造端点。
    /// 生产代码请通过 `AuthorizationEndpointsProvider.endpoints()` 取用，以便利用缓存。
    static func derive(
        issuer: String,
        metadata: OIDCProviderMetadata?
    ) throws -> AuthorizationServerEndpoints {
        let base = stripTrailingSlash(issuer)
        guard let challenge = URL(string: base + "/oauth2/challenge") else {
            throw APIError.invalidConfiguration(message: "无法基于 issuer 构造 /oauth2/challenge URL: \(issuer)")
        }
        let token: URL
        if let advertised = metadata?.tokenEndpoint, let url = URL(string: advertised) {
            token = rewriteIfLoopback(url, toMatchIssuer: issuer)
        } else if let fallback = URL(string: base + "/oauth2/token") {
            token = fallback
        } else {
            throw APIError.invalidConfiguration(message: "无法定位 token_endpoint")
        }
        let userinfo: URL
        if let advertised = metadata?.userinfoEndpoint, let url = URL(string: advertised) {
            userinfo = rewriteIfLoopback(url, toMatchIssuer: issuer)
        } else if let fallback = URL(string: base + "/userinfo") {
            userinfo = fallback
        } else {
            throw APIError.invalidConfiguration(message: "无法定位 userinfo_endpoint")
        }
        guard let otp = URL(string: base + "/otp/tickets") else {
            throw APIError.invalidConfiguration(message: "无法基于 issuer 构造 /otp/tickets URL")
        }
        return AuthorizationServerEndpoints(
            issuer: base,
            challengeEndpoint: challenge,
            tokenEndpoint: token,
            userinfoEndpoint: userinfo,
            otpTicketsEndpoint: otp
        )
    }

    // MARK: - 内部工具

    /// 便于开发联调的回退策略：当 OIDC Discovery 公布的端点指向 loopback host
    /// （`localhost` / `127.0.0.1`），但客户端配置的 issuer 指向真实 host 时，重写
    /// scheme/host/port 使端点在设备上可达。
    ///
    /// 背景：Spring Authorization Server 在生成 issuer claim 时直接采用 `application.yml`
    /// 中配置的 `issuer: http://localhost:8080`，与请求实际打到的 host 无关。在真机 iPhone
    /// 上意味着 Discovery 公布的所有端点会指向设备自身的 loopback 地址而不可达。该重写仅在
    /// "公布的 host 是 loopback **且** 配置的 issuer 不是 loopback" 时生效，因此不会影响
    /// 生产部署（issuer = 真实域名）。
    fileprivate static func rewriteIfLoopback(_ url: URL, toMatchIssuer issuer: String) -> URL {
        let loopbackHosts: Set<String> = ["localhost", "127.0.0.1", "::1", "0.0.0.0"]
        guard let host = url.host?.lowercased(), loopbackHosts.contains(host) else {
            return url
        }
        guard let issuerComponents = URLComponents(string: issuer),
              let issuerHost = issuerComponents.host?.lowercased(),
              !loopbackHosts.contains(issuerHost) else {
            return url
        }
        guard var components = URLComponents(url: url, resolvingAgainstBaseURL: false) else {
            return url
        }
        components.scheme = issuerComponents.scheme
        components.host = issuerComponents.host
        components.port = issuerComponents.port
        let rewritten = components.url ?? url
        #if DEBUG
        Logger(
            subsystem: Bundle.main.bundleIdentifier ?? "com.eulerframework.appattest.scaffold",
            category: "AuthEndpoints"
        ).debug("重写 Discovery loopback endpoint: \(url.absoluteString, privacy: .public) -> \(rewritten.absoluteString, privacy: .public)")
        #endif
        return rewritten
    }

    private static func stripTrailingSlash(_ s: String) -> String {
        var r = s
        while r.hasSuffix("/") { r.removeLast() }
        return r
    }
}

/// 异步取用 `AuthorizationServerEndpoints` 的入口，对外暴露 actor 以保证并发安全。
///
/// 缓存策略：以当前 `AppConfiguration.issuer` 为 key 缓存一次解析结果；issuer 变化或配置
/// 页面手动触发 `invalidate()` 时丢弃缓存。Discovery 拉取由 `OIDCDiscoveryService`
/// 内部完成，本 provider 仅做端点派生与缓存。
actor AuthorizationEndpointsProvider {

    /// 全局共享实例。授权服务的 client 默认通过 `.shared` 取端点，便于跨调用共享 metadata 缓存。
    /// 测试中可以构造独立实例并注入 mock discovery。
    static let shared = AuthorizationEndpointsProvider()

    private let discovery: OIDCDiscoveryService
    private var cached: AuthorizationServerEndpoints?

    init(discovery: OIDCDiscoveryService = .shared) {
        self.discovery = discovery
    }

    /// 解析当前 issuer 对应的端点；按需走一次 Discovery 拉取。
    ///
    /// 任何线程都可调用 —— actor 隔离保证并发安全。issuer 畸形或 Discovery 与 fallback
    /// 路径都构造失败时退回为占位 `about:blank` 端点，让登录调用以可见错误失败，但应用仍可
    /// 继续运行（用户可进入设置页修改 issuer）。
    func endpoints() async -> AuthorizationServerEndpoints {
        let issuer = AppConfiguration.issuer
        if let cached, cached.issuer == AuthorizationServerEndpoints.normalizedIssuer(issuer) {
            return cached
        }
        let metadata = await discovery.metadata(for: issuer)
        let resolved = (try? AuthorizationServerEndpoints.derive(issuer: issuer, metadata: metadata))
            ?? AuthorizationServerEndpoints(
                issuer: AuthorizationServerEndpoints.normalizedIssuer(issuer),
                challengeEndpoint: URL(string: "about:blank")!,
                tokenEndpoint: URL(string: "about:blank")!,
                userinfoEndpoint: URL(string: "about:blank")!,
                otpTicketsEndpoint: URL(string: "about:blank")!
            )
        cached = resolved
        return resolved
    }

    /// 用户在设置页改完 issuer 之后应调用一次。
    func invalidate() async {
        cached = nil
        await discovery.invalidate()
    }
}

private extension AuthorizationServerEndpoints {
    static func normalizedIssuer(_ issuer: String) -> String {
        stripTrailingSlash(issuer)
    }
}
