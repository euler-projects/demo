import Foundation
import Security

// MARK: - Models

struct OAuthTokenResponse: Codable {
    let accessToken: String
    let tokenType: String
    let expiresIn: Int
    let scope: String?
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case expiresIn = "expires_in"
        case scope
    }
}

struct OAuthErrorResponse: Codable {
    let error: String
    let errorDescription: String?
    
    enum CodingKeys: String, CodingKey {
        case error
        case errorDescription = "error_description"
    }
}

// MARK: - Error

enum AppAttestError: LocalizedError {
    case notSupported
    case noKeyId
    case invalidURL
    case invalidResponse
    case appIdDetectionFailed
    case serverError(statusCode: Int, error: String, description: String?)
    
    var errorDescription: String? {
        switch self {
        case .notSupported:
            return "App Attest is not supported on this device."
        case .noKeyId:
            return "No stored key ID available. Perform attestation first."
        case .invalidURL:
            return "Invalid token endpoint URL."
        case .invalidResponse:
            return "Invalid server response."
        case .appIdDetectionFailed:
            return "Failed to detect App ID (teamId.bundleId). Ensure the app is properly signed."
        case .serverError(let statusCode, let error, let description):
            return "Server error (\(statusCode)): \(error) - \(description ?? "")"
        }
    }
}

// MARK: - OAuthTokenManager

class OAuthTokenManager {
    static let shared = OAuthTokenManager()
    
    static let defaultBaseUrl = "https://as.example.com"
    static let baseUrlKey = "com.app.oauth.baseUrl"
    
    /// Server base URL (may include a path), stored in UserDefaults, configurable from the UI
    var baseUrl: String {
        get {
            UserDefaults.standard.string(forKey: Self.baseUrlKey) ?? Self.defaultBaseUrl
        }
        set {
            UserDefaults.standard.set(newValue, forKey: Self.baseUrlKey)
        }
    }
    
    /// Combine baseUrl with fixed paths, stripping trailing slashes
    private var base: String {
        var result = baseUrl
        while result.hasSuffix("/") {
            result.removeLast()
        }
        return result
    }
    private var oauth2ChallengeEndpoint: String { base + "/oauth2/challenge" }
    private var oauth2TokenEndpoint: String { base + "/oauth2/token" }
    
    /// App ID (teamId.bundleId), auto-detected at runtime via Keychain access groups.
    /// Used only for UI display and debugging — the current API does not require client_id.
    /// Client identity is established through Apple App Attest hardware-backed attestation,
    /// where the keyId is bound to this specific app instance on this specific device.
    static let appId: String? = detectAppId()
    
    /// Keychain storage keys for the access token.
    /// The access token MUST be stored in Keychain — it is equivalent to a user identity
    /// credential; if leaked, an attacker can directly impersonate the user.
    private static let accessTokenKeychainKey = "com.app.oauth.accessToken"
    private static let tokenExpirationKeychainKey = "com.app.oauth.tokenExpiration"
    
    /// ⚠️ Token is cached in memory for fast access only.
    /// Persistent storage uses Keychain — NEVER use UserDefaults or the file system.
    private var currentToken: OAuthTokenResponse?
    private var tokenExpirationDate: Date?
    
    /// Last HTTP request/response debug info (for log display in the UI)
    private(set) var lastRequestInfo: RequestInfo?
    private(set) var lastResponseInfo: ResponseInfo?
    
    /// Clear request/response debug info before each new operation to avoid stale data
    func clearLastRequestInfo() {
        lastRequestInfo = nil
        lastResponseInfo = nil
    }
    
    private init() {
        // Restore token from Keychain on launch (if available)
        loadTokenFromKeychain()
    }
    
    /// Fetch a one-time challenge for the OAuth2 Assertion flow.
    /// Endpoint: `POST /oauth2/challenge` — no authentication, no request body required.
    /// - Returns: A one-time challenge string (Base64URL-encoded, valid for 5 minutes, single-use)
    func fetchChallenge() async throws -> String {
        guard let url = URL(string: oauth2ChallengeEndpoint) else {
            throw AppAttestError.invalidURL
        }

        var request = URLRequest(url: url)
        request.httpMethod = "POST"

        lastRequestInfo = RequestInfo(request: request)

        let (data, response) = try await URLSession.shared.data(for: request)
        let httpResponse = response as? HTTPURLResponse
        let rawBody = String(data: data, encoding: .utf8) ?? "<binary data>"
        lastResponseInfo = ResponseInfo(
            statusCode: httpResponse?.statusCode ?? -1,
            body: rawBody
        )

        guard let httpResponse = httpResponse, httpResponse.statusCode == 200 else {
            throw AppAttestError.serverError(
                statusCode: httpResponse?.statusCode ?? -1,
                error: "challenge_request_failed",
                description: rawBody
            )
        }

        let json = try JSONDecoder().decode([String: String].self, from: data)
        guard let challenge = json["challenge"] else {
            throw AppAttestError.invalidResponse
        }
        return challenge
    }
    /// Get an access token using App Assertion (for registered clients).
    /// After registration (Phase 1), each time the token expires, a new token is obtained via
    /// the Assertion flow. No refresh_token is used — every request requires full attestation-based
    /// client authentication, so refresh_token provides no additional security value.
    /// - Parameter challenge: One-time challenge string obtained from `POST /oauth2/challenge`
    /// - Returns: `OAuthTokenResponse`
    func getTokenWithAssertion(challenge: String) async throws -> OAuthTokenResponse {
        let attestManager = AppAttestManager.shared

        guard attestManager.isSupported else {
            throw AppAttestError.notSupported
        }

        // 1. Retrieve the stored keyId (must have completed Attestation registration)
        guard let keyId = AppAttestManager.shared.getExistingKeyId() else {
            throw AppAttestError.noKeyId
        }

        // 2. Generate an Assertion using the Apple API
        let assertion = try await attestManager.generateAssertion(
            keyId: keyId,
            challenge: challenge
        )

        // 3. Build request parameters (kid, assertion per the latest API spec)
        let params: [String: String] = [
            "grant_type": "urn:ietf:params:oauth:grant-type:app_assertion",
            "kid": keyId,
            "assertion": assertion,
            "challenge": challenge,
            "scope": "openid"
        ]

        // 4. Send the token request
        let token = try await requestToken(params: params)
        saveToken(token)
        return token
    }

    /// Get a valid access token, automatically renewing or re-registering as needed.
    /// Strategy: Assertion for new token → fallback to Attestation re-registration (Phase 1).
    /// No refresh_token is used — every request requires full attestation-based client authentication.
    /// Note: challenges are single-use, so a fresh challenge is obtained for each attempt.
    func getValidAccessToken() async throws -> String {
        if let token = currentToken,
           let expiration = tokenExpirationDate,
           expiration.timeIntervalSinceNow > 60 {
            return token.accessToken
        }

        // 1. Primary path: use Assertion to get a token (when client is already registered)
        // ⚠️ A fresh challenge must be obtained each time — challenges are single-use
        if AppAttestManager.shared.getExistingKeyId() != nil {
            do {
                let challenge = try await fetchChallenge()
                let token = try await getTokenWithAssertion(challenge: challenge)
                return token.accessToken
            } catch {
                // Assertion failed — fall back to re-registration
            }
        }

        // 2. Fallback: re-register via App Attestation (Phase 1), then get a token via Assertion
        _ = try await AppAttestRegistrationService.shared.register()
        let challenge = try await fetchChallenge()
        let token = try await getTokenWithAssertion(challenge: challenge)
        return token.accessToken
    }
    
    // MARK: - Private
    
    /// Securely save token to both in-memory cache and Keychain
    private func saveToken(_ token: OAuthTokenResponse) {
        self.currentToken = token
        self.tokenExpirationDate = Date().addingTimeInterval(TimeInterval(token.expiresIn))
        
        // Persist to Keychain so the session survives app restarts
        _ = KeychainHelper.shared.save(token.accessToken, forKey: Self.accessTokenKeychainKey)
        let expirationString = String(self.tokenExpirationDate!.timeIntervalSince1970)
        _ = KeychainHelper.shared.save(expirationString, forKey: Self.tokenExpirationKeychainKey)
    }
    
    /// Restore token from Keychain on launch
    private func loadTokenFromKeychain() {
        guard let accessToken = KeychainHelper.shared.read(forKey: Self.accessTokenKeychainKey),
              let expirationString = KeychainHelper.shared.read(forKey: Self.tokenExpirationKeychainKey),
              let expirationInterval = Double(expirationString) else {
            return
        }
        self.tokenExpirationDate = Date(timeIntervalSince1970: expirationInterval)
        self.currentToken = OAuthTokenResponse(
            accessToken: accessToken,
            tokenType: "Bearer",
            expiresIn: Int(self.tokenExpirationDate!.timeIntervalSinceNow),
            scope: nil
        )
    }
    
    private func requestToken(params: [String: String]) async throws -> OAuthTokenResponse {
        guard let url = URL(string: oauth2TokenEndpoint) else {
            throw AppAttestError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        // When carrying Assertion parameters, add the attestation type header
        // to indicate the client authentication method
        if params["kid"] != nil && params["assertion"] != nil {
            request.setValue("apple_app_attest", forHTTPHeaderField: "OAuth-Client-Attestation-Type")
        }
        
        // Form body
        // ⚠️ Must use strict percent-encoding: .urlQueryAllowed does NOT encode +/= characters,
        // which would break form-urlencoded parsing (+ means space, = is a separator).
        var formUrlEncodingAllowed = CharacterSet.alphanumerics
        formUrlEncodingAllowed.insert(charactersIn: "-._~")
        let bodyString = params.map { "\($0.key)=\($0.value.addingPercentEncoding(withAllowedCharacters: formUrlEncodingAllowed) ?? $0.value)" }
            .joined(separator: "&")
        request.httpBody = bodyString.data(using: .utf8)
        
        lastRequestInfo = RequestInfo(request: request)
        
        let (data, response) = try await URLSession.shared.data(for: request)
        let httpResponse = response as? HTTPURLResponse
        let rawBody = String(data: data, encoding: .utf8) ?? "<binary data>"
        lastResponseInfo = ResponseInfo(
            statusCode: httpResponse?.statusCode ?? -1,
            body: rawBody
        )
        
        guard let httpResponse = httpResponse else {
            throw AppAttestError.invalidResponse
        }
        
        if httpResponse.statusCode == 200 {
            return try JSONDecoder().decode(OAuthTokenResponse.self, from: data)
        } else {
            let errorResponse = try? JSONDecoder().decode(OAuthErrorResponse.self, from: data)
            throw AppAttestError.serverError(
                statusCode: httpResponse.statusCode,
                error: errorResponse?.error ?? "unknown",
                description: errorResponse?.errorDescription
            )
        }
    }
}

// MARK: - Request / Response Info

struct RequestInfo {
    let method: String
    let url: String
    let headers: [String: String]
    let body: String?
    
    init(request: URLRequest) {
        self.method = request.httpMethod ?? "GET"
        self.url = request.url?.absoluteString ?? ""
        var h: [String: String] = [:]
        request.allHTTPHeaderFields?.forEach { h[$0.key] = $0.value }
        self.headers = h
        if let data = request.httpBody {
            self.body = String(data: data, encoding: .utf8)
        } else {
            self.body = nil
        }
    }
    
    var summary: String {
        var lines = ["\(method) \(url)"]
        for (key, value) in headers.sorted(by: { $0.key < $1.key }) {
            lines.append("\(key): \(value)")
        }
        if let body = body {
            lines.append("")
            lines.append(body)
        }
        return lines.joined(separator: "\n")
    }
}

struct ResponseInfo {
    let statusCode: Int
    let body: String
    
    var summary: String {
        "HTTP \(statusCode)\n\(body)"
    }
}

// MARK: - App ID Detection

private extension OAuthTokenManager {
    /// Detect App ID via Keychain access groups.
    /// The default Keychain access group format is "<TeamID>.<BundleID>",
    /// which can be used directly as the OAuth2 client_id.
    static func detectAppId() -> String? {
        let tempKey = "com.app.appIdDetection"
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: tempKey,
            kSecValueData as String: Data("x".utf8),
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecReturnAttributes as String: true
        ]
        SecItemDelete(query as CFDictionary)
        var result: AnyObject?
        let status = SecItemAdd(query as CFDictionary, &result)
        defer { SecItemDelete(query as CFDictionary) }

        guard status == errSecSuccess,
              let dict = result as? [String: Any],
              let accessGroup = dict[kSecAttrAccessGroup as String] as? String else {
            return nil
        }
        return accessGroup
    }
}
