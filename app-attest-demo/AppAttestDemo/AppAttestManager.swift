import DeviceCheck
import Foundation
import CryptoKit

class AppAttestManager {
    static let shared = AppAttestManager()
    
    private let service = DCAppAttestService.shared
    
    /// Key ID的Keychain存储键名
    /// ⚠️ Key ID是App Attest密钥的唯一标识符, 关联了Secure Enclave中的私钥,
    /// 必须存储在Keychain中, 因为:
    /// 1. 泄露Key ID可能被用于伪造设备身份或进行重放攻击
    /// 2. UserDefaults以明文plist存储, 越狱设备可直接读取
    private let keyIdKeychainKey = "com.app.appAttest.keyId"
    
    private init() {}
    
    /// 检查当前设备是否支持App Attest
    var isSupported: Bool {
        service.isSupported
    }
    
    /// 获取已存储的Key ID(用于Assertion流程)
    /// - Returns: 已存储的Key ID, 如果不存在则返回nil
    func getExistingKeyId() -> String? {
        return KeychainHelper.shared.read(forKey: keyIdKeychainKey)
    }

    /// 生成新的App Attest Key(用于Attestation流程)
    /// ⚠️ 每个Key只能被attest一次, 因此每次Attestation必须生成新Key
    /// Key ID不会立即写入Keychain, 需要等整个Attestation流程(含服务端验证)成功后再保存
    func generateNewKeyId() async throws -> String {
        return try await service.generateKey()
    }

    /// 将Key ID保存到Keychain(在Attestation流程完全成功后调用)
    func saveKeyId(_ keyId: String) {
        _ = KeychainHelper.shared.save(keyId, forKey: keyIdKeychainKey)
    }
    
    /// 生成Attestation Object
    /// - Parameters:
    ///   - keyId: App Attest Key Identifier
    ///   - challenge: 服务端下发的challenge字符串
    /// - Returns: Base64编码的Attestation Object
    func generateAttestation(keyId: String, challenge: String) async throws -> String {
        let challengeData = Data(challenge.utf8)
        let challengeHash = Data(SHA256.hash(data: challengeData))
        let attestation = try await service.attestKey(keyId, clientDataHash: challengeHash)
        return attestation.base64EncodedString()
    }

    /// 生成Assertion Object(用于已注册设备重新认证)
    /// - Parameters:
    ///   - keyId: App Attest Key Identifier
    ///   - challenge: 服务端下发的challenge字符串
    /// - Returns: Base64编码的Assertion Object
    func generateAssertion(keyId: String, challenge: String) async throws -> String {
        let challengeData = Data(challenge.utf8)
        let challengeHash = Data(SHA256.hash(data: challengeData))
        let assertion = try await service.generateAssertion(keyId, clientDataHash: challengeHash)
        return assertion.base64EncodedString()
    }
}
