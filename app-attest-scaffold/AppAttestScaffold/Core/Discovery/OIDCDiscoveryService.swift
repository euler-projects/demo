import Foundation
import Alamofire

/// 从 `/.well-known/openid-configuration` 加载的 OpenID Provider Metadata 子集。
///
/// 仅声明脚手架真正会消费的字段；响应中其他字段一律忽略。授权服务侧 Discovery 不会发布的
/// 端点（`/oauth2/challenge`、`/otp/tickets`）以及账号服务端点（`/user/identities`）均
/// 不在此结构中体现，分别由 `AuthorizationServerEndpoints` / `AccountServiceEndpoints`
/// 负责派生。
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

/// 加载并按 issuer 缓存 OpenID Provider Metadata。
///
/// 仅承担"协议层"职责：标准 OIDC `/.well-known/openid-configuration` 的拉取与缓存，
/// 不知道授权服务上的扩展端点（如 `/oauth2/challenge`），更不知道用户账号服务的存在。
/// 端点派生与缓存由 `AuthorizationEndpointsProvider` 完成；本服务被它持有作为 metadata
/// 的来源。首次访问时尝试拉取 Discovery，失败时返回 nil（让端点解析逻辑自行回退到
/// 基于 issuer 的惯例路径），保证离线启动 / 服务端短暂抖动时仍可登录。
actor OIDCDiscoveryService {

    /// 全局共享实例。在生产代码中由 `AuthorizationEndpointsProvider` 持有；测试中可直接
    /// 构造独立实例并注入 mock `Alamofire.Session`。
    static let shared = OIDCDiscoveryService()

    private let http: Session
    private var cache: (issuer: String, metadata: OIDCProviderMetadata?)?

    init(http: Session = .default) {
        self.http = http
    }

    /// 解析并缓存指定 issuer 对应的 metadata。
    /// - Returns: 成功则返回解码后的 metadata；网络 / 解析失败时返回 nil（不抛错）。
    ///   失败结果同样会被缓存以避免在离线场景下反复重试 —— 调用方 `invalidate()` 即可
    ///   触发下一次重新拉取。
    func metadata(for issuer: String) async -> OIDCProviderMetadata? {
        if let cache, cache.issuer == issuer {
            return cache.metadata
        }
        let fetched = try? await fetchMetadata(issuer: issuer)
        cache = (issuer, fetched)
        return fetched
    }

    /// 强制下一次调用时重新拉取。issuer 变更或诊断入口手动触发时调用。
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
        do {
            return try await http.request(
                url,
                method: .get,
                headers: HTTPHeaders(["Accept": "application/json"])
            )
            .validate()
            .serializingDecodable(OIDCProviderMetadata.self)
            .value
        } catch {
            throw APIError.network(message: error.localizedDescription)
        }
    }
}
