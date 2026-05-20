import Foundation
import Alamofire

/// `POST /oauth2/challenge` 与 `POST /oauth2/token` 的客户端封装。
///
/// 脚手架支持两种 grant type：
/// - `urn:ietf:params:oauth:grant-type:app_assertion`
///     - 携带 `attestation` → 首次匿名设备注册。
///     - 携带 `assertion`   → 已注册 kid 的常规 AT 刷新。
/// - `otp`
///     - 携带 `attestation` + `otp_ticket` + `otp` → 标准登录（带设备注册）。
///
/// 所有请求体均为 `application/x-www-form-urlencoded`；attestation / assertion blob 已经过
/// 标准 Base64 编码，会再次进行百分号编码（否则 `+`、`=`、`/` 会破坏请求体 ——
/// 详见 [FormURLEncoder.swift](file:///Users/cfrost/Documents/code/GitHub/euler-projects/demo/app-attest-scaffold/AppAttestScaffold/Core/Networking/FormURLEncoder.swift)）。
final class OAuthClient {

    private let http: Session
    private let endpoints: AuthorizationEndpointsProvider

    init(
        http: Session,
        endpoints: AuthorizationEndpointsProvider = .shared
    ) {
        self.http = http
        self.endpoints = endpoints
    }

    // MARK: - Challenge

    /// 从服务端获取一次性 challenge。按规范 challenge 仅可使用一次，且约 5 分钟过期 ——
    /// 切勿跨请求缓存。
    func fetchChallenge() async throws -> String {
        let resolved = await endpoints.endpoints()
        // 服务端允许空请求体；按规范我们仍以 POST 发起。
        let request = OAuthRequestBuilder.formRequest(url: resolved.challengeEndpoint, pairs: [])
        let payload: ChallengeResponse = try await OAuthTransport.send(request, on: http)
        return payload.challenge
    }

    // MARK: - 匿名注册（App Attestation 用法 1）

    /// 首次匿名登录。需要一个全新 attest 过的 kid。
    func anonymousRegister(
        kid: String,
        attestationBase64: String,
        challenge: String,
        scope: String = AppConfiguration.defaultScope
    ) async throws -> TokenBundle {
        let resolved = await endpoints.endpoints()
        let pairs: [(String, String)] = [
            ("grant_type", AppConfiguration.appAssertionGrantType),
            ("kid", kid),
            ("attestation", attestationBase64),
            ("challenge", challenge),
            ("scope", scope)
        ]
        let request = OAuthRequestBuilder.formRequest(
            url: resolved.tokenEndpoint,
            pairs: pairs,
            extraHeaders: ["OAuth-Client-Attestation-Type": AppConfiguration.attestationType]
        )
        let response: TokenResponse = try await OAuthTransport.send(request, on: http)
        return response.materialize()
    }

    // MARK: - App Assertion 刷新（App Assertion 用法 1）

    /// 已注册 kid 的常规 AT 刷新。服务端规范: 凡 `grant_type=app_assertion` 的请求
    /// （不论携带 `attestation` 还是 `assertion`）都必须带 `OAuth-Client-Attestation-Type`
    /// 请求头, 服务端据此路由到对应的 attestation provider。
    /// 参见 doc/apis/zh-cn/App-Attest-Login.md §6 刷新令牌。
    func refreshWithAssertion(
        kid: String,
        assertionBase64: String,
        challenge: String,
        scope: String = AppConfiguration.defaultScope
    ) async throws -> TokenBundle {
        let resolved = await endpoints.endpoints()
        let pairs: [(String, String)] = [
            ("grant_type", AppConfiguration.appAssertionGrantType),
            ("kid", kid),
            ("assertion", assertionBase64),
            ("challenge", challenge),
            ("scope", scope)
        ]
        let request = OAuthRequestBuilder.formRequest(
            url: resolved.tokenEndpoint,
            pairs: pairs,
            extraHeaders: ["OAuth-Client-Attestation-Type": AppConfiguration.attestationType]
        )
        let response: TokenResponse = try await OAuthTransport.send(request, on: http)
        return response.materialize()
    }

    // MARK: - 标准 OTP 登录（App Attestation 用法 2）

    /// 一次完成 OTP 校验 + 设备注册的标准登录。
    /// 用户从匿名升级为真实账号、或在新设备上登录时使用此方法。
    func signInWithOTP(
        otpTicket: String,
        otp: String,
        kid: String,
        attestationBase64: String,
        challenge: String,
        scope: String = AppConfiguration.defaultScope
    ) async throws -> TokenBundle {
        let resolved = await endpoints.endpoints()
        let pairs: [(String, String)] = [
            ("grant_type", AppConfiguration.otpGrantType),
            ("otp_ticket", otpTicket),
            ("otp", otp),
            ("kid", kid),
            ("attestation", attestationBase64),
            ("challenge", challenge),
            ("scope", scope)
        ]
        let request = OAuthRequestBuilder.formRequest(
            url: resolved.tokenEndpoint,
            pairs: pairs,
            extraHeaders: ["OAuth-Client-Attestation-Type": AppConfiguration.attestationType]
        )
        let response: TokenResponse = try await OAuthTransport.send(request, on: http)
        return response.materialize()
    }
}

private struct ChallengeResponse: Decodable {
    let challenge: String
    let expiresIn: Int?

    enum CodingKeys: String, CodingKey {
        case challenge
        case expiresIn = "expires_in"
    }
}
