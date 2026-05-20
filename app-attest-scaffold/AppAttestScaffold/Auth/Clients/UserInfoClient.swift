import Foundation

/// `GET /userinfo` 的客户端封装。
///
/// 当前服务端契约下，响应包含 `sub` claim（作为 `username` 使用），可能附带
/// `nickname` / `picture`；脚手架将后两者视作可选，缺失时回退为 `username` + 内置
/// SF Symbol（详见 App-Attest-Login.md §2.2）。
final class UserInfoClient {

    private let http: HTTPClient
    private let endpoints: AuthorizationEndpointsProvider

    init(
        http: HTTPClient = .shared,
        endpoints: AuthorizationEndpointsProvider = .shared
    ) {
        self.http = http
        self.endpoints = endpoints
    }

    /// 用 `accessToken` 拉取当前用户的 userinfo profile。
    func fetch(accessToken: String) async throws -> Account.Profile {
        let resolved = await endpoints.endpoints()
        let request = http.jsonRequest(
            url: resolved.userinfoEndpoint,
            method: "GET",
            bearerToken: accessToken
        )
        let payload = try await http.send(request, as: UserInfoPayload.self)
        return Account.Profile(
            username: payload.preferredUsername ?? payload.sub,
            nickname: payload.nickname,
            avatarUrl: payload.picture
        )
    }
}

/// OIDC 标准 userinfo 响应的子集。当前后端 `nickname` 与 `picture` 均为可选字段，
/// 这里允许任一缺失。
private struct UserInfoPayload: Decodable {
    let sub: String
    let preferredUsername: String?
    let nickname: String?
    let picture: String?

    enum CodingKeys: String, CodingKey {
        case sub
        case preferredUsername = "preferred_username"
        case nickname
        case picture
    }
}
