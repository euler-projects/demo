import Foundation
import Security

/// 通用的 Keychain 封装，承载 `Codable` JSON 载荷。
///
/// 脚手架持久化三类条目（token bundle、当前 account、kid）。它们共用同一个 service 标识，
/// 仅以 `account` 为 key 区分 —— 这样可以保持 API 表面足够小，单 bundle 应用也无需引入
/// access group。
///
/// 所有条目以 `kSecAttrAccessibleWhenUnlockedThisDeviceOnly` 写入，因此：
/// - 只要设备处于解锁状态，应用重启后条目仍然可读。
/// - 不会被 iCloud Keychain 备份（仅留在本机）。
/// - 设备锁屏时不可读，缓解离线提取风险。
enum KeychainStore {

    /// 所有条目共用的 service 标识，按惯例与 bundle id 一致。
    static let service = "com.eulerframework.appattest.scaffold"

    enum KeychainError: Error, LocalizedError {
        case unexpectedStatus(OSStatus)
        case dataConversion

        var errorDescription: String? {
            switch self {
            case .unexpectedStatus(let status):
                return "Keychain operation failed with status \(status)"
            case .dataConversion:
                return "Failed to convert Keychain data"
            }
        }
    }

    // MARK: - Codable 封装

    /// 将一个 `Codable` 值编码为 JSON 写入 `account` 条目，覆盖任何已存在的旧值。
    static func set<T: Encodable>(_ value: T, for account: String) throws {
        let data = try JSONEncoder().encode(value)
        try setData(data, for: account)
    }

    /// 读取并解码先前由 `set(_:for:)` 写入的 JSON 值。
    /// 若 `account` 下无记录则返回 `nil`。
    static func get<T: Decodable>(_ type: T.Type, for account: String) throws -> T? {
        guard let data = try getData(for: account) else { return nil }
        return try JSONDecoder().decode(T.self, from: data)
    }

    // MARK: - 原始字节增删改查

    static func setData(_ data: Data, for account: String) throws {
        let baseQuery: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]

        // 先尝试 update；若条目不存在再 fall back 到 add。
        let updateAttributes: [String: Any] = [
            kSecValueData as String: data,
            kSecAttrAccessible as String: kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        ]
        let updateStatus = SecItemUpdate(baseQuery as CFDictionary, updateAttributes as CFDictionary)
        if updateStatus == errSecSuccess { return }
        if updateStatus != errSecItemNotFound {
            throw KeychainError.unexpectedStatus(updateStatus)
        }

        var addQuery = baseQuery
        addQuery[kSecValueData as String] = data
        addQuery[kSecAttrAccessible as String] = kSecAttrAccessibleWhenUnlockedThisDeviceOnly
        let addStatus = SecItemAdd(addQuery as CFDictionary, nil)
        guard addStatus == errSecSuccess else {
            throw KeychainError.unexpectedStatus(addStatus)
        }
    }

    static func getData(for account: String) throws -> Data? {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account,
            kSecReturnData as String: true,
            kSecMatchLimit as String: kSecMatchLimitOne
        ]
        var result: AnyObject?
        let status = SecItemCopyMatching(query as CFDictionary, &result)
        switch status {
        case errSecSuccess:
            guard let data = result as? Data else { throw KeychainError.dataConversion }
            return data
        case errSecItemNotFound:
            return nil
        default:
            throw KeychainError.unexpectedStatus(status)
        }
    }

    /// 删除单条条目。条目不存在时为 no-op。
    static func remove(for account: String) throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service,
            kSecAttrAccount as String: account
        ]
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(status)
        }
    }

    /// 删除当前 service 下的所有条目。供 `AppDataStore.bootstrapIfNeeded()` 与 `AppDataStore.clearAll()`
    /// 等全局清理场景使用。
    static func removeAll() throws {
        let query: [String: Any] = [
            kSecClass as String: kSecClassGenericPassword,
            kSecAttrService as String: service
        ]
        let status = SecItemDelete(query as CFDictionary)
        guard status == errSecSuccess || status == errSecItemNotFound else {
            throw KeychainError.unexpectedStatus(status)
        }
    }
}
