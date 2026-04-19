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

struct AttestRegistrationResponse: Codable {
    let keyId: String
    let username: String
    
    enum CodingKeys: String, CodingKey {
        case keyId = "key_id"
        case username
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
    
    /// 服务地址(支持带路径), 存储在UserDefaults中, 可在页面上配置
    var baseUrl: String {
        get {
            UserDefaults.standard.string(forKey: Self.baseUrlKey) ?? Self.defaultBaseUrl
        }
        set {
            UserDefaults.standard.set(newValue, forKey: Self.baseUrlKey)
        }
    }
    
    /// 拼接baseUrl与固定路径, 自动处理末尾斜杠
    private var base: String {
        baseUrl.hasSuffix("/") ? baseUrl : baseUrl + "/"
    }
    private var tokenEndpoint: String { base + "oauth2/token" }
    private var challengeEndpoint: String { base + "oauth2/challenge" }
    private var attestChallengeEndpoint: String { base + "device/challenge" }
    private var registerEndpoint: String { base + "device/attest" }
    
    /// App ID (teamId.bundleId), 运行时通过Keychain访问组自动检测
    /// 仅用于UI展示和调试, 当前API不需要传递client_id
    /// 设备身份通过Apple App Attest硬件级证明来保证, 服务端将其作为Device Attest的一种实现
    static let appId: String? = detectAppId()
    
    /// Token在Keychain中的存储键名
    /// Access Token必须存储在Keychain中, 因为Token等同于用户身份凭证, 泄露后攻击者可直接冒充用户调用API
    private static let accessTokenKeychainKey = "com.app.oauth.accessToken"
    private static let tokenExpirationKeychainKey = "com.app.oauth.tokenExpiration"
    
    /// ⚠️ Token仅在内存中缓存用于快速访问
    /// 持久化存储使用Keychain, 切勿使用UserDefaults或文件系统
    private var currentToken: OAuthTokenResponse?
    private var tokenExpirationDate: Date?
    
    /// 最近一次HTTP请求的调试信息(供日志展示)
    private(set) var lastRequestInfo: RequestInfo?
    private(set) var lastResponseInfo: ResponseInfo?
    
    /// 清除请求/响应调试信息, 在每次新操作前调用以避免显示残留数据
    func clearLastRequestInfo() {
        lastRequestInfo = nil
        lastResponseInfo = nil
    }
    
    private init() {
        // 启动时从Keychain恢复Token(如果存在)
        loadTokenFromKeychain()
    }
    
    /// 从服务端获取一次性challenge(用于Token Assertion流程)
    /// 对应接口: POST /oauth2/challenge, 无需认证, 无需请求体
    /// - Returns: challenge字符串
    func fetchChallenge() async throws -> String {
        return try await fetchChallengeFrom(endpoint: challengeEndpoint)
    }

    /// 从服务端获取一次性challenge(用于设备注册Attestation流程)
    /// 对应接口: POST /device/challenge, 无需认证, 无需请求体
    /// - Returns: challenge字符串
    func fetchAttestChallenge() async throws -> String {
        return try await fetchChallengeFrom(endpoint: attestChallengeEndpoint)
    }

    /// 通用challenge获取方法
    private func fetchChallengeFrom(endpoint: String) async throws -> String {
        guard let url = URL(string: endpoint) else {
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
    
    /// 设备注册(Attestation流程)
    /// 仅在首次使用时执行一次, 注册成功后后续直接使用Assertion获取Token
    /// 完整流程: 获取attest challenge → 生成密钥 → 生成attestation → 提交注册
    /// - Returns: AttestRegistrationResponse
    func registerDevice() async throws -> AttestRegistrationResponse {
        let attestManager = AppAttestManager.shared
        
        guard attestManager.isSupported else {
            throw AppAttestError.notSupported
        }
        
        // 1. 获取注册用的challenge
        let challenge = try await fetchAttestChallenge()
        
        // 2. 每次Attestation必须生成新Key
        // ⚠️ 一个Key只能被attest一次, 复用已attest的Key会报 com.apple.devicecheck.error
        let keyId = try await attestManager.generateNewKeyId()
        
        // 3. 生成Attestation
        let attestation = try await attestManager.generateAttestation(
            keyId: keyId,
            challenge: challenge
        )
        
        // 4. 提交注册请求
        guard let url = URL(string: registerEndpoint) else {
            throw AppAttestError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        let params: [String: String] = [
            "key_id": keyId,
            "attestation": attestation,
            "challenge": challenge
        ]
        
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
            let result = try JSONDecoder().decode(AttestRegistrationResponse.self, from: data)
            // 5. 服务端验证成功后才将Key ID存入Keychain, 供后续Assertion获取Token使用
            attestManager.saveKeyId(keyId)
            return result
        } else {
            let errorResponse = try? JSONDecoder().decode(OAuthErrorResponse.self, from: data)
            throw AppAttestError.serverError(
                statusCode: httpResponse.statusCode,
                error: errorResponse?.error ?? "registration_failed",
                description: errorResponse?.errorDescription
            )
        }
    }

    /// 使用Device Assertion获取Token(已注册设备)
    /// 设备注册成功后, 每次Token过期时通过Assertion重新获取Token
    /// 不使用refresh_token, 因为每次请求都需要完整的attestation验证, refresh_token无安全增益
    /// - Parameter challenge: 服务端下发的challenge字符串(来自 /oauth2/challenge)
    /// - Returns: OAuthTokenResponse
    func getTokenWithAssertion(challenge: String) async throws -> OAuthTokenResponse {
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

        // 3. 构建请求参数(参数名按最新API文档: kid, assertion)
        let params: [String: String] = [
            "grant_type": "urn:ietf:params:oauth:grant-type:device_assertion",
            "kid": keyId,
            "assertion": assertion,
            "challenge": challenge,
            "scope": "openid"
        ]

        // 4. 发送Token请求
        let token = try await requestToken(params: params)
        saveToken(token)
        return token
    }

    /// 获取有效的Access Token, 必要时自动续期或重新注册
    /// 续期策略: Assertion获取新Token > Attestation重新注册
    /// 不使用refresh_token, 因为每次请求都需要完整的attestation验证, refresh_token无安全增益
    /// 注意: challenge是一次性的, 每次认证尝试需要获取新的challenge, 因此本方法内部按需获取
    func getValidAccessToken() async throws -> String {
        if let token = currentToken,
           let expiration = tokenExpirationDate,
           expiration.timeIntervalSinceNow > 60 {
            return token.accessToken
        }

        // 1. 使用Assertion获取Token(设备已注册时的主要手段)
        // ⚠️ 每次都必须获取新的challenge, challenge是一次性的, 使用后立即失效
        if AppAttestManager.shared.getExistingKeyId() != nil {
            do {
                let challenge = try await fetchChallenge()
                let token = try await getTokenWithAssertion(challenge: challenge)
                return token.accessToken
            } catch {
                // Assertion失败, 尝试重新注册
            }
        }

        // 2. 最后回退到Attestation重新注册, 然后通过Assertion获取Token
        _ = try await registerDevice()
        let challenge = try await fetchChallenge()
        let token = try await getTokenWithAssertion(challenge: challenge)
        return token.accessToken
    }
    
    // MARK: - Private
    
    /// 将Token安全地保存到内存缓存和Keychain
    private func saveToken(_ token: OAuthTokenResponse) {
        self.currentToken = token
        self.tokenExpirationDate = Date().addingTimeInterval(TimeInterval(token.expiresIn))
        
        // 持久化到Keychain, 以便APP重启后恢复会话
        _ = KeychainHelper.shared.save(token.accessToken, forKey: Self.accessTokenKeychainKey)
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
        self.tokenExpirationDate = Date(timeIntervalSince1970: expirationInterval)
        self.currentToken = OAuthTokenResponse(
            accessToken: accessToken,
            tokenType: "Bearer",
            expiresIn: Int(self.tokenExpirationDate!.timeIntervalSinceNow),
            scope: nil
        )
    }
    
    private func requestToken(params: [String: String]) async throws -> OAuthTokenResponse {
        guard let url = URL(string: tokenEndpoint) else {
            throw AppAttestError.invalidURL
        }
        
        var request = URLRequest(url: url)
        request.httpMethod = "POST"
        request.setValue("application/x-www-form-urlencoded", forHTTPHeaderField: "Content-Type")
        
        // 携带Assertion参数时, 添加PoP-Type头标识客户端证明方式
        if params["kid"] != nil && params["assertion"] != nil {
            request.setValue("app-attest", forHTTPHeaderField: "OAuth-Client-Attestation-Type")
        }
        
        // Form body
        // ⚠️ 必须使用严格的字符集进行percent-encoding
        // .urlQueryAllowed不会编码+/=等字符, 而Base64编码的assertion包含这些字符
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

// MARK: - App ID Detection

private extension OAuthTokenManager {
    /// 通过Keychain访问组检测App ID
    /// Keychain默认访问组格式即为 "<TeamID>.<BundleID>", 可直接用作OAuth2 client_id
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
