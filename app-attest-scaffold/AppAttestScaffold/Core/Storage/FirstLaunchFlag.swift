import Foundation

/// 首次启动检测器，承担两项相关职责：
///
/// 1. **启动期清理** —— 在安装后第一次运行时清除上一次安装遗留在 Keychain 中的条目
///    （iOS 上 Keychain 条目在卸载应用后仍会保留，可能留下指向已不存在的
///    Secure Enclave key 的过期 `kid`）。
///
/// 2. **匿名注册资格闸门** —— 根据 App-Attest-Login.md §2.4，应用仅允许在首次使用时
///    提供匿名登录入口；之后即便用户登出也不再允许匿名注册（否则
///    "登出 + 登录" 会成为批量制造孤立匿名账号的途径）。
///
/// 该标志位存放在 `UserDefaults` 中（刻意而为 —— 不放 Keychain）：Keychain 在重新安装后
/// 仍然保留，而 UserDefaults 不会。这种不对称正是我们需要的：全新安装会再次被识别为
/// "首次启动"，从而强制 `bootstrapIfNeeded()` 清理任何残留 Keychain 状态，并重新启用
/// 匿名注册入口。
enum FirstLaunchFlag {

    private static let flagKey = "com.eulerframework.scaffold.hasLaunchedBefore"

    /// 当当前进程在本次安装中已至少运行过一次时返回 `true`。
    /// 任何时候 `true`，匿名注册入口都应被隐藏。
    static var hasLaunchedBefore: Bool {
        UserDefaults.standard.bool(forKey: flagKey)
    }

    /// 仅在首次启动时执行：清除 Keychain 中残留的状态并将标志位置位。
    /// 幂等 —— 可以安全地在每次启动时从 `AppAttestScaffoldApp.init()` 调用。
    static func bootstrapIfNeeded() {
        guard !hasLaunchedBefore else { return }
        try? KeychainStore.removeAll()
        UserDefaults.standard.set(true, forKey: flagKey)
    }

    /// 重置标志位（例如供开发期测试使用）。生产 UI 中不暴露。
    static func resetForTesting() {
        UserDefaults.standard.removeObject(forKey: flagKey)
    }
}
