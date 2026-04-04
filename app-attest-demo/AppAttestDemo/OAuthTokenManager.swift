import Foundation

// MARK: - Models

struct OAuthTokenResponse: Codable {
    let accessToken: String
    let tokenType: String
    let refreshToken: String?
    let expiresIn: Int
    let scope: String?
    
    enum CodingKeys: String, CodingKey {
        case accessToken = "access_token"
        case tokenType = "token_type"
        case refreshToken = "refresh_token"
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
    case noRefreshToken
    case invalidURL
    case invalidResponse
    case clientCredentialsNotConfigured
    case serverError(statusCode: Int, error: String, description: String?)
    
    var errorDescription: String? {
        switch self {
        case .notSupported:
            return "App Attest is not supported on this device."
        case .noKeyId:
            return "No stored key ID available. Perform attestation first."
        case .noRefreshToken:
            return "No refresh token available."
        case .invalidURL:
            return "Invalid token endpoint URL."
        case .invalidResponse:
            return "Invalid server response."
        case .clientCredentialsNotConfigured:
            return "Client credentials not configured. Call configureClientCredentials() first."
        case .serverError(let statusCode, let error, let description):
            return "Server error (\(statusCode)): \(error) - \(description ?? "")"
        }
    }
}

// MARK: - OAuthTokenManager

class OAuthTokenManager {
    static let shared = OAuthTokenManager()
    
    private let tokenEndpoint = "http://192.168.50.40:8080/oauth2/token"
    private let challengeEndpoint = "http://192.168.50.40:8080/oauth2/challenge"
    
    /// ⚠️ 安全警告: Client ID和Client Secret是OAuth2客户端凭证
    /// **绝对不要**在源码中硬编码这些值, 原因:
    /// 1. 源码可被反编译(class-dump、Hopper等), 硬编码的字符串可被直接提取
    /// 2. 泄露的Client Secret可被用于伪造客户端身份, 冒充合法APP请求Token
    /// 3. 一旦泄露需要服务端轮换密钥, 并强制所有用户更新APP
    ///
    /// 正确做法: 在APP首次配置时将凭证安全地写入Keychain, 后续从Keychain读取
    /// 凭证来源可以是: 服务端下发、编译期注入(如Xcode Build Configuration + 混淆)等
    private static let clientIdKeychainKey = "com.app.oauth.clientId"
    private static let clientSecretKeychainKey = "com.app.oauth.clientSecret"
    
    /// Token在Keychain中的存储键名
    /// Access Token和Refresh Token必须存储在Keychain中, 因为:
    /// 1. Token等同于用户身份凭证, 泄露后攻击者可直接冒充用户调用API
    /// 2. Refresh Token有效期长(7天), 泄露风险窗口大
    private static let accessTokenKeychainKey = "com.app.oauth.accessToken"
    private static let refreshTokenKeychainKey = "com.app.oauth.refreshToken"
    private static let tokenExpirationKeychainKey = "com.app.oauth.tokenExpiration"
    
    /// ⚠️ Token仅在内存中缓存用于快速访问
    /// 持久化存储使用Keychain, 切勿使用UserDefaults或文件系统
    private var currentToken: OAuthTokenResponse?
    private var tokenExpirationDate: Date?
    
    /// 最近一次HTTP请求的调试信息(供日志展示)
    private(set) var lastRequestInfo: RequestInfo?
    private(set) var lastResponseInfo: ResponseInfo?
    
    private init() {
        // 启动时从Keychain恢复Token(如果存在)
        loadTokenFromKeychain()
    }
    
    /// 配置客户端凭证(应在APP首次启动或配置时调用)
    /// - Parameters:
    ///   - clientId: OAuth2 Client ID
    ///   - clientSecret: OAuth2 Client Secret
    func configureClientCredentials(clientId: String, clientSecret: String) {
        _ = KeychainHelper.shared.save(clientId, forKey: Self.clientIdKeychainKey)
        _ = KeychainHelper.shared.save(clientSecret, forKey: Self.clientSecretKeychainKey)
    }
    
    /// 从服务端获取一次性challenge
    /// - Returns: challenge字符串
    func fetchChallenge() async throws -> String {
        guard let url = URL(string: challengeEndpoint) else {
            throw AppAttestError.invalidURL
        }
        
        guard let clientId = KeychainHelper.shared.read(forKey: Self.clientIdKeychainKey),
              let clientSecret = KeychainHelper.shared.read(forKey: Self.clientSecretKeychainKey) else {
            throw AppAttestError.clientCredentialsNotConfigured
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        let credentials = "\(clientId):\(clientSecret)"
        let base64Credentials = Data(credentials.utf8).base64EncodedString()
        request.setValue("Basic \(base64Credentials)", forHTTPHeaderField: "Authorization")
        
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
    
    /// 使用Apple App Attest获取OAuth Token(初次注册 - Attestation)
    /// - Parameter challenge: 服务端下发的challenge字符串
    /// - Returns: OAuthTokenResponse
    func authenticateWithAppAttestAttestation(challenge: String) async throws -> OAuthTokenResponse {
        let attestManager = AppAttestManager.shared
        
        guard attestManager.isSupported else {
            throw AppAttestError.notSupported
        }
        
        // 1. 每次Attestation必须生成新Key
        // ⚠️ 一个Key只能被attest一次, 复用已attest的Key会报 com.apple.devicecheck.error
        let keyId = try await attestManager.generateNewKeyId()
        
        // 2. 生成Attestation
        let attestation = try await attestManager.generateAttestation(
            keyId: keyId,
            challenge: challenge
        )
        
        // 3. 构建请求参数
        let params: [String: String] = [
            "grant_type": "apple_app_attest_attestation",
            "key_id": keyId,
            "attestation": attestation,
            "challenge": challenge
        ]
        
        // 4. 发送Token请求
        let token = try await requestToken(params: params)
        
        // 5. 服务端验证成功后才将Key ID存入Keychain, 供后续Assertion流程使用
        attestManager.saveKeyId(keyId)
        
        saveToken(token)
        return token
    }

    /// 使用Apple App Attest Assertion获取OAuth Token(已注册设备重新认证)
    /// - Parameter challenge: 服务端下发的challenge字符串
    /// - Returns: OAuthTokenResponse
    func authenticateWithAppAttestAssertion(challenge: String) async throws -> OAuthTokenResponse {
        let attestManager = AppAttestManager.shared

        guard attestManager.isSupported else {
            throw AppAttestError.notSupported
        }

        // 1. 获取已存储的Key ID(必须已通过Attestation注册)
        guard let keyId = AppAttestManager.shared.getExistingKeyId() else {
            throw AppAttestError.noKeyId
        }

        // 2. 生成Assertion
        let assertion = try await attestManager.generateAssertion(
            keyId: keyId,
            challenge: challenge
        )

        // 3. 构建请求参数
        let params: [String: String] = [
            "grant_type": "apple_app_attest_assertion",
            "key_id": keyId,
            "assertion_data": assertion,
            "challenge": challenge
        ]

        // 4. 发送Token请求
        let token = try await requestToken(params: params)
        saveToken(token)
        return token
    }

    /// 使用Refresh Token续期
    func refreshAccessToken() async throws -> OAuthTokenResponse {
        guard let refreshToken = currentToken?.refreshToken else {
            throw AppAttestError.noRefreshToken
        }
        
        let params: [String: String] = [
            "grant_type": "refresh_token",
            "refresh_token": refreshToken
        ]
        
        let token = try await requestToken(params: params)
        saveToken(token)
        return token
    }
    
    /// 获取有效的Access Token, 必要时自动续期或重新认证
    /// 优先尝试Refresh Token续期, 其次尝试Assertion重新认证, 最后回退到Attestation
    /// 注意: challenge是一次性的, 每次认证尝试需要获取新的challenge, 因此本方法内部按需获取
    func getValidAccessToken() async throws -> String {
        if let token = currentToken,
           let expiration = tokenExpirationDate,
           expiration.timeIntervalSinceNow > 60 {
            return token.accessToken
        }
        
        // 尝试用Refresh Token续期(不需要challenge)
        if currentToken?.refreshToken != nil {
            do {
                let token = try await refreshAccessToken()
                return token.accessToken
            } catch {
                // Refresh Token也失效了, 尝试Assertion或Attestation重新认证
            }
        }

        // 尝试用Assertion重新认证(如果已有Key ID)
        // ⚠️ 每次认证都必须获取新的challenge, challenge是一次性的, 使用后立即失效
        if AppAttestManager.shared.getExistingKeyId() != nil {
            do {
                let challenge = try await fetchChallenge()
                let token = try await authenticateWithAppAttestAssertion(challenge: challenge)
                return token.accessToken
            } catch {
                // Assertion失败, 回退到Attestation
            }
        }

        // 最后回退到Attestation(初次注册), 需要获取新的challenge
        let challenge = try await fetchChallenge()
        let token = try await authenticateWithAppAttestAttestation(challenge: challenge)
        return token.accessToken
    }
    
    // MARK: - Private
    
    /// 将Token安全地保存到内存缓存和Keychain
    private func saveToken(_ token: OAuthTokenResponse) {
        self.currentToken = token
        self.tokenExpirationDate = Date().addingTimeInterval(TimeInterval(token.expiresIn))
        
        // 持久化到Keychain, 以便APP重启后恢复会话
        _ = KeychainHelper.shared.save(token.accessToken, forKey: Self.accessTokenKeychainKey)
        if let refreshToken = token.refreshToken {
            _ = KeychainHelper.shared.save(refreshToken, forKey: Self.refreshTokenKeychainKey)
        }
        let expirationString = String(self.tokenExpirationDate!.timeIntervalSince1970)
        _ = KeychainHelper.shared.save(expirationString, forKey: Self.tokenExpirationKeychainKey)
    }
    
    /// 从Keychain恢复Token
    private func loadTokenFromKeychain() {
        guard let accessToken = KeychainHelper.shared.read(forKey: Self.accessTokenKeychainKey),
              let expirationString = KeychainHelper.shared.read(forKey: Self.tokenExpirationKeychainKey),
              let expirationInterval = Double(expirationString) else {
            return
        }
        let refreshToken = KeychainHelper.shared.read(forKey: Self.refreshTokenKeychainKey)
        self.tokenExpirationDate = Date(timeIntervalSince1970: expirationInterval)
        self.currentToken = OAuthTokenResponse(
            accessToken: accessToken,
            tokenType: "Bearer",
            refreshToken: refreshToken,
            expiresIn: Int(self.tokenExpirationDate!.timeIntervalSinceNow),
            scope: nil
        )
    }
    
    private func requestToken(params: [String: String]) async throws -> OAuthTokenResponse {
        guard let url = URL(string: tokenEndpoint) else {
            throw AppAttestError.invalidURL
        }
        
        // 从Keychain读取客户端凭证
        guard let clientId = KeychainHelper.shared.read(forKey: Self.clientIdKeychainKey),
              let clientSecret = KeychainHelper.shared.read(forKey: Self.clientSecretKeychainKey) else {
            throw AppAttestError.clientCredentialsNotConfigured
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        // Client Basic Auth
        let credentials = "\(clientId):\(clientSecret)"
        let credentialsData = Data(credentials.utf8)
        let base64Credentials = credentialsData.base64EncodedString()
        request.setValue("Basic \(base64Credentials)", forHTTPHeaderField: "Authorization")
        
        // Form body
        // ⚠️ 必须使用严格的字符集进行percent-encoding
        // .urlQueryAllowed不会编码+/=等字符, 而Base64编码的attestation/assertion包含这些字符
        // +在form-urlencoded中代表空格, =是key-value分隔符, 不编码会导致服务端解析错误
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
