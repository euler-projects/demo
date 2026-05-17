import Foundation

/// 持久化的 OAuth token 元组，直接由 `/oauth2/token` 响应解码得到。
///
/// 服务端返回的 `expires_in` 是相对 TTL，这里在解码时即转换为绝对时间 `expiresAt`，
/// 后续代码可以直接与 `Date()` 比较，无需另外记录响应到达的时间。`refreshToken` 为可选，
/// 因为匿名账号不会下发 refresh token（详见 App-Attest-Login.md §2.1）。
struct TokenBundle: Codable, Equatable {

    var accessToken: String
    var tokenType: String
    var expiresAt: Date
    var refreshToken: String?
    var refreshTokenExpiresAt: Date?
    var scope: String?
    var idToken: String?

    /// 提前刷新的安全冗余。在严格过期时间之前提前刷新，避免与正在进行的携带 AT 的请求
    /// 出现竞态。
    static let refreshLeadTime: TimeInterval = 60

    var isExpired: Bool { Date() >= expiresAt }
    var willExpireSoon: Bool { Date() >= expiresAt.addingTimeInterval(-Self.refreshLeadTime) }

    enum CodingKeys: String, CodingKey {
        case accessToken
        case tokenType
        case expiresAt
        case refreshToken
        case refreshTokenExpiresAt
        case scope
        case idToken
    }
}

/// `POST /oauth2/token` 响应的线协议格式。通过 `materialize()` 转换为 `TokenBundle`。
struct TokenResponse: Decodable {
    let accessToken: String
    let tokenType: String
    let expiresIn: Int
    let refreshToken: String?
    let refreshExpiresIn: Int?
    let scope: String?
    let idToken: String?

    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case expiresIn = "expires_in"
        case refreshToken = "refresh_token"
        case refreshExpiresIn = "refresh_expires_in"
        case scope
        case idToken = "id_token"
    }

    /// 将线协议载荷转换为待持久化的 bundle，将 TTL 锚定在 "now"。
    func materialize(now: Date = Date()) -> TokenBundle {
        let expiresAt = now.addingTimeInterval(TimeInterval(expiresIn))
        let refreshExpiresAt = refreshExpiresIn.map { now.addingTimeInterval(TimeInterval($0)) }
        return TokenBundle(
            accessToken: accessToken,
            tokenType: tokenType,
            expiresAt: expiresAt,
            refreshToken: refreshToken,
            refreshTokenExpiresAt: refreshExpiresAt,
            scope: scope,
            idToken: idToken
        )
    }
}
