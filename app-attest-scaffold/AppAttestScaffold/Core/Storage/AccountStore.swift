import Foundation

/// 基于 Keychain 的最近一次 `Account` 快照缓存。
///
/// 脚手架将 `Account` 与会话凭证存放在一起，以便冷启动时主页可以立即渲染，
/// 而不必等待一次 `/userinfo` 往返。`save` 是幂等的 ——
/// 每当有新的身份数据到达，`AppSession` / `AuthorizationFacade` 会重写该条目。
enum AccountStore {

    private static let account = "account.last"

    static func load() -> Account? {
        (try? KeychainStore.get(Account.self, for: account)) ?? nil
    }

    static func save(_ value: Account) throws {
        try KeychainStore.set(value, for: account)
    }

    static func clear() {
        try? KeychainStore.remove(for: account)
    }
}
