import Foundation
#if DEBUG
import os
#endif

/// 从 `/.well-known/openid-configuration` 加载的 OpenID Provider Metadata 子集。
///
/// 仅解码脚手架会用到的字段 —— 响应中其他多余字段一律忽略。Discovery 不会发布的端点
/// （`/oauth2/challenge`、`/otp/tickets`、`/user/identities` 等）由
/// `AuthorizationServerEndpoints` 在 issuer 基础上派生。
struct OIDCProviderMetadata: Decodable, Equatable {
    let issuer: String
    let tokenEndpoint: String
    let userinfoEndpoint: String?
    let jwksUri: String?

    enum CodingKeys: String, CodingKey {
        case issuer
        case tokenEndpoint = "token_endpoint"
        case userinfoEndpoint = "userinfo_endpoint"
        case jwksUri = "jwks_uri"
    }
}

/// 各 auth client 共用的端点解析结果。
///
/// 采用混合策略：
/// - 标准 OIDC 端点（`token`、`userinfo`）来自 Discovery，使部署方可以把它们迁到不同
///   路径或域名。
/// - 平台自定义端点（`challenge`、`otp/tickets`、`user/identities`）则基于 `issuer` 派生，
///   因为它们不在任何 RFC 中定义。
struct AuthorizationServerEndpoints {
    let issuer: String

    /// `POST` —— 申请一次性 challenge，用于 App Assertion / OTP 登录。
    let oauth2ChallengeEndpoint: URL
    /// `POST` —— 用 grant 凭证换取 access token。
    let oauth2TokenEndpoint: URL
    /// `GET` —— 拉取已认证用户的 userinfo（至少包含 `sub`）。
    let userinfoEndpoint: URL
    /// `POST` —— 申请一张 OTP ticket（触发 SMS / 邮件投递）。
    let otpTicketsEndpoint: URL
    /// `POST` —— 在当前账号上绑定新 factor；`GET` —— 列出当前 factor。
    let userIdentitiesEndpoint: URL

    fileprivate static func derive(from issuer: String, metadata: OIDCProviderMetadata?) throws -> AuthorizationServerEndpoints {
        let base = AuthorizationServerEndpoints.stripTrailingSlash(issuer)

        guard let challengeURL = URL(string: base + "/oauth2/challenge") else {
            throw APIError.invalidConfiguration(message: "无法基于 issuer 构造 /oauth2/challenge URL: \(issuer)")
        }
        // Token endpoint：优先采用 Discovery 给出的取值，缺失时回退到 issuer 相对路径。
        let tokenURL: URL
        if let advertised = metadata?.tokenEndpoint, let url = URL(string: advertised) {
            tokenURL = rewriteIfLoopback(url, toMatchIssuer: issuer)
        } else if let fallback = URL(string: base + "/oauth2/token") {
            tokenURL = fallback
        } else {
            throw APIError.invalidConfiguration(message: "无法定位 token_endpoint")
        }
        // Userinfo endpoint：优先采用 Discovery 给出的取值，缺失时回退到 issuer 相对路径。
        let userinfoURL: URL
        if let advertised = metadata?.userinfoEndpoint, let url = URL(string: advertised) {
            userinfoURL = rewriteIfLoopback(url, toMatchIssuer: issuer)
        } else if let fallback = URL(string: base + "/userinfo") {
            userinfoURL = fallback
        } else {
            throw APIError.invalidConfiguration(message: "无法定位 userinfo_endpoint")
        }
        guard let otpURL = URL(string: base + "/otp/tickets"),
              let identitiesURL = URL(string: base + "/user/identities") else {
            throw APIError.invalidConfiguration(message: "无法基于 issuer 构造平台自定义端点 URL")
        }
        return AuthorizationServerEndpoints(
            issuer: issuer,
            oauth2ChallengeEndpoint: challengeURL,
            oauth2TokenEndpoint: tokenURL,
            userinfoEndpoint: userinfoURL,
            otpTicketsEndpoint: otpURL,
            userIdentitiesEndpoint: identitiesURL
        )
    }

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
            category: "Discovery"
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

/// 加载并缓存当前 issuer 对应的 OpenID Provider Metadata。
///
/// 首次访问时尝试拉取 `/.well-known/openid-configuration`；若失败（离线启动、服务端短暂
/// 抖动），则回退到基于 issuer 的相对路径，使用户仍能尝试登录。`AppConfiguration.issuer`
/// 变化时缓存会被清除。
actor OIDCDiscoveryService {

    static let shared = OIDCDiscoveryService()

    private var cache: (issuer: String, endpoints: AuthorizationServerEndpoints)?

    /// 解析当前 issuer 对应的端点；按需拉取 Discovery。
    /// 任何线程都可调用 —— 并发调用方共享同一次飞行中的加载。
    func endpoints() async -> AuthorizationServerEndpoints {
        let issuer = AppConfiguration.issuerBase
        if let cached = cache, cached.issuer == issuer {
            return cached.endpoints
        }
        let metadata = try? await fetchMetadata(issuer: issuer)
        // 端点派生只会在 issuer 畸形时失败；此种情况下退回为容忍式的 issuer 相对配置 ——
        // 登录会以可见错误失败，但应用仍可继续运行，让用户能够进入设置页修改 issuer。
        let endpoints = (try? AuthorizationServerEndpoints.derive(from: issuer, metadata: metadata))
            ?? AuthorizationServerEndpoints(
                issuer: issuer,
                oauth2ChallengeEndpoint: URL(string: "about:blank")!,
                oauth2TokenEndpoint: URL(string: "about:blank")!,
                userinfoEndpoint: URL(string: "about:blank")!,
                otpTicketsEndpoint: URL(string: "about:blank")!,
                userIdentitiesEndpoint: URL(string: "about:blank")!
            )
        cache = (issuer, endpoints)
        return endpoints
    }

    /// 强制下一次调用时重新拉取。用户在设置页改完 issuer 后应当调用一次。
    func invalidate() {
        cache = nil
    }

    // MARK: - 内部实现

    private func fetchMetadata(issuer: String) async throws -> OIDCProviderMetadata {
        var base = issuer
        while base.hasSuffix("/") { base.removeLast() }
        guard let url = URL(string: base + "/.well-known/openid-configuration") else {
            throw APIError.invalidConfiguration(message: "无法基于 issuer 构造 discovery URL: \(issuer)")
        }
        let request = HTTPClient.shared.jsonRequest(url: url, method: "GET")
        return try await HTTPClient.shared.send(request, as: OIDCProviderMetadata.self)
    }
}
