import Foundation

/// 用户账号服务（Account Service）所暴露的端点集合。
///
/// 与授权服务不同，账号服务不参与 OAuth 协议本身，端点也不发布 OIDC Discovery 文档；
/// 路径是平台契约的一部分，按 `accountServiceBase + 固定路径` 直接派生即可。
///
/// 端点契约见 [App-Attest-Login.md §1] —— 当前版本仅包含登录身份管理的
/// `/user/identities`；未来扩展（例如账号注销、隐私偏好）也应集中在此结构中维护，
/// 避免直接在 client 中拼路径。
struct AccountServiceEndpoints: Equatable {

    /// 账号服务基地址（已规范化为去尾部斜杠形式）。
    let baseURL: String

    /// `GET` —— 列出当前账号已绑定的全部登录身份。
    /// `POST` —— 在当前账号上绑定新登录身份。
    /// `DELETE /{identity_id}` —— 解绑指定登录身份。
    let identitiesEndpoint: URL

    /// 解析当前 `AppConfiguration.accountServiceBase` 对应的端点。
    /// 调用同步即可完成 —— 账号服务不依赖 Discovery。
    static func current() -> AccountServiceEndpoints {
        do {
            return try resolve(baseURL: AppConfiguration.accountServiceBase)
        } catch {
            // 配置畸形时回退为占位端点，让业务调用以可见错误失败，应用仍可运行。
            return AccountServiceEndpoints(
                baseURL: AppConfiguration.accountServiceBase,
                identitiesEndpoint: URL(string: "about:blank")!
            )
        }
    }

    /// 单元测试 / 内部使用：基于显式 base 构造端点。
    static func resolve(baseURL: String) throws -> AccountServiceEndpoints {
        let normalized = stripTrailingSlash(baseURL)
        guard let identities = URL(string: normalized + "/user/identities") else {
            throw APIError.invalidConfiguration(
                message: "无法基于账号服务基地址构造 /user/identities URL: \(baseURL)"
            )
        }
        return AccountServiceEndpoints(
            baseURL: normalized,
            identitiesEndpoint: identities
        )
    }

    private static func stripTrailingSlash(_ s: String) -> String {
        var r = s
        while r.hasSuffix("/") { r.removeLast() }
        return r
    }
}
