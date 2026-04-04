import Foundation
import Security

/// Keychain安全存储工具
/// 所有敏感数据(密钥、凭证、Token等)必须存储在Keychain中, 原因:
/// 1. Keychain数据经过硬件加密, 即使设备越狱也难以直接读取
/// 2. Keychain数据在APP卸载后仍可保留(根据配置), 适合存储长期凭证
/// 3. 支持访问控制策略, 可要求生物识别后才能访问
class KeychainHelper {
    static let shared = KeychainHelper()
    private init() {}
    
    func save(_ data: String, forKey key: String) -> Bool {
        guard let data = data.data(using: .utf8) else { return false }
        
        // 先删除已有项, 避免重复
        delete(forKey: key)
        
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            // kSecAttrAccessible: 设备解锁后可访问, 且不会迁移到新设备
            // 这防止了备份恢复到其他设备后凭证泄露
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly,
            kSecValueData as String: data
        ]
        return SecItemAdd(query as CFDictionary, nil) == errSecSuccess
    }
    
    func read(forKey key: String) -> String? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        guard status == errSecSuccess, let data = result as? Data else { return nil }
        return String(data: data, encoding: .utf8)
    }
    
    @discardableResult
    func delete(forKey key: String) -> Bool {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrAccount as String: key
        ]
        return SecItemDelete(query as CFDictionary) == errSecSuccess
    }
}
