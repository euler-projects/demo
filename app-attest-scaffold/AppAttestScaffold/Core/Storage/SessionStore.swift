import Foundation

/// 对 OAuth 会话四元组 `(accessToken, refreshToken, expiresAt, kid)` 的持久化封装。
///
/// `TokenBundle` 与 `kid` 拆成两个 Keychain 条目存储：刷新 access token 时只需要重写
/// 较小的 bundle 条目；即便 Keychain 写入中途失败，承载 Secure Enclave key 句柄的 kid
/// 也不会丢失。这一拆分对应 App-Attest-Login.md §2 的建议
/// （"建议分两个 Keychain Entry 存储"）。
enum SessionStore {

    private static let tokenAccount = "session.tokenBundle"
    private static let kidAccount = "session.kid"

    // MARK: - Token bundle

    static func loadTokenBundle() -> TokenBundle? {
        (try? KeychainStore.get(TokenBundle.self, for: tokenAccount)) ?? nil
    }

    static func saveTokenBundle(_ bundle: TokenBundle) throws {
        try KeychainStore.set(bundle, for: tokenAccount)
    }

    static func clearTokenBundle() {
        try? KeychainStore.remove(for: tokenAccount)
    }

    // MARK: - kid

    /// 当前会话已注册的 App Attest key 标识符（不透明字符串）。
    /// 设备从未完成 attestation 或登出后会为空。
    static func loadKid() -> String? {
        guard let data = try? KeychainStore.getData(for: kidAccount) else { return nil }
        return String(data: data, encoding: .utf8)
    }

    static func saveKid(_ kid: String) throws {
        guard let data = kid.data(using: .utf8) else {
            throw KeychainStore.KeychainError.dataConversion
        }
        try KeychainStore.setData(data, for: kidAccount)
    }

    static func clearKid() {
        try? KeychainStore.remove(for: kidAccount)
    }

    // MARK: - 联合操作

    /// 同时清除两个条目。用于登出，以及 `kid` 被吊销时。
    static func clearAll() {
        clearTokenBundle()
        clearKid()
    }
}
