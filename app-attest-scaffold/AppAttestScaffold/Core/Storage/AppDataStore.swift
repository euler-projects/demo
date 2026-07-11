import Foundation

/// 所有持久化数据的唯一访问入口。
///
/// 数据由两个维度定性：
/// - **Lifecycle**: `.session`（退出登录时清除）| `.persistent`（仅"抹掉所有数据"时清除）
/// - **Secret**: 机密数据强制 Keychain；非机密数据默认 UserDefaults，也可显式指定 Keychain。
///
/// 业务模块可使用任意 `String` 作为 key 自由扩展存储，无需修改 AppDataStore 本身。
/// 预置 key 常量收录在 `AppDataStore.Keys` 中。
enum AppDataStore {

    // MARK: - 类型定义

    /// 数据生命周期
    enum Lifecycle: String, Codable {
        /// 退出登录时清除
        case session
        /// 仅"抹掉所有数据"时清除
        case persistent
    }

    /// 存储介质
    enum Storage: String, Codable {
        case keychain
        case userDefaults
    }

    // MARK: - 预置 Key 常量

    enum Keys {
        static let tokenBundle = "session.tokenBundle"
        static let kid = "session.kid"
        static let account = "cache.account"
        static let issuer = "config.issuer"
        static let accountServiceBase = "config.accountServiceBase"
        static let hasLaunchedBefore = "flags.hasLaunchedBefore"
    }

    // MARK: - Generic CRUD

    /// 写入数据。secret == true 强制 Keychain；secret == false 默认 UserDefaults。
    static func set<T: Encodable>(_ value: T, for key: String, lifecycle: Lifecycle, secret: Bool) throws {
        let storage: Storage = secret ? .keychain : .userDefaults
        try set(value, for: key, lifecycle: lifecycle, secret: secret, storage: storage)
    }

    /// 写入数据并显式指定存储介质。用于非机密但需要 Keychain 的场景。
    /// secret == true 时 storage 参数被忽略（始终 Keychain）。
    static func set<T: Encodable>(_ value: T, for key: String, lifecycle: Lifecycle, secret: Bool, storage: Storage) throws {
        let resolvedStorage: Storage = secret ? .keychain : storage
        // 更新 registry
        let entry = RegistryEntry(lifecycle: lifecycle, secret: secret, storage: resolvedStorage)
        registrySet(key: key, entry: entry)
        // 写入数据
        switch resolvedStorage {
        case .keychain:
            try KeychainStore.set(value, for: key)
        case .userDefaults:
            let data = try JSONEncoder().encode(value)
            defaults.set(data, forKey: dataDefaultsKey(key))
        }
    }

    /// 读取数据。返回 nil 表示未存储过。
    static func get<T: Decodable>(_ type: T.Type, for key: String) -> T? {
        guard let entry = registryGet(key: key) else { return nil }
        switch entry.storage {
        case .keychain:
            return (try? KeychainStore.get(type, for: key)) ?? nil
        case .userDefaults:
            guard let data = defaults.data(forKey: dataDefaultsKey(key)) else { return nil }
            return try? JSONDecoder().decode(type, from: data)
        }
    }

    /// 读取数据，未存储时返回调用方指定的默认值。
    static func get<T: Decodable>(_ type: T.Type, for key: String, default defaultValue: T) -> T {
        get(type, for: key) ?? defaultValue
    }

    /// 删除单条数据。
    static func remove(for key: String) {
        guard let entry = registryGet(key: key) else { return }
        switch entry.storage {
        case .keychain:
            try? KeychainStore.remove(for: key)
        case .userDefaults:
            defaults.removeObject(forKey: dataDefaultsKey(key))
        }
        registryRemove(key: key)
    }

    // MARK: - Coordinated Clear

    /// 退出登录 —— 清除所有 lifecycle == .session 的条目。
    static func clearSession() {
        let registry = registryLoadAll()
        for (key, entry) in registry where entry.lifecycle == .session {
            switch entry.storage {
            case .keychain:
                try? KeychainStore.remove(for: key)
            case .userDefaults:
                defaults.removeObject(forKey: dataDefaultsKey(key))
            }
            registryRemove(key: key)
        }
    }

    /// 抹掉所有数据 —— 清除全部条目 + KeychainStore.removeAll() 兜底。
    static func clearAll() {
        let registry = registryLoadAll()
        for (key, entry) in registry {
            switch entry.storage {
            case .keychain:
                try? KeychainStore.remove(for: key)
            case .userDefaults:
                defaults.removeObject(forKey: dataDefaultsKey(key))
            }
        }
        // 清空 registry
        defaults.removeObject(forKey: registryDefaultsKey)
        // 兜底清除 Keychain 中任何可能的残留
        try? KeychainStore.removeAll()
    }

    // MARK: - Bootstrap

    /// 首次启动清理：清除上一次安装残留在 Keychain 中的条目并标记已启动。
    /// 幂等 —— 可安全地在每次启动时调用。
    static func bootstrapIfNeeded() {
        let launched: Bool = get(Bool.self, for: Keys.hasLaunchedBefore, default: false)
        guard !launched else { return }
        try? KeychainStore.removeAll()
        // 清空可能残留的 registry（上一次安装遗留在 UserDefaults 中的 registry 条目
        // 指向的 Keychain 数据已被清除，registry 本身也应重置）。
        defaults.removeObject(forKey: registryDefaultsKey)
        try? set(true, for: Keys.hasLaunchedBefore, lifecycle: .persistent, secret: false)
    }

    // MARK: - 内部实现

    private static let defaults = UserDefaults.standard
    private static let registryDefaultsKey = "com.eulerframework.scaffold.datastore.registry"

    /// 为 UserDefaults 中的数据 key 添加前缀，避免与 registry key 或其他 key 冲突。
    private static func dataDefaultsKey(_ key: String) -> String {
        "com.eulerframework.scaffold.data.\(key)"
    }

    /// Registry entry 元数据
    private struct RegistryEntry: Codable {
        let lifecycle: Lifecycle
        let secret: Bool
        let storage: Storage
    }

    private static func registryLoadAll() -> [String: RegistryEntry] {
        guard let data = defaults.data(forKey: registryDefaultsKey) else { return [:] }
        return (try? JSONDecoder().decode([String: RegistryEntry].self, from: data)) ?? [:]
    }

    private static func registrySaveAll(_ registry: [String: RegistryEntry]) {
        if let data = try? JSONEncoder().encode(registry) {
            defaults.set(data, forKey: registryDefaultsKey)
        }
    }

    private static func registryGet(key: String) -> RegistryEntry? {
        registryLoadAll()[key]
    }

    private static func registrySet(key: String, entry: RegistryEntry) {
        var registry = registryLoadAll()
        registry[key] = entry
        registrySaveAll(registry)
    }

    private static func registryRemove(key: String) {
        var registry = registryLoadAll()
        registry.removeValue(forKey: key)
        registrySaveAll(registry)
    }
}
