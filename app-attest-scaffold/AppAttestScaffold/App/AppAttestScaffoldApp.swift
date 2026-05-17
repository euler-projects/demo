import SwiftUI

/// 应用入口。
///
/// 职责：
/// - 在任何 UI 渲染之前完成首次启动的清理工作（清除上一次安装残留在 Keychain 中的条目）。
/// - 构造单例 `AppSession` 并注入到 SwiftUI 环境中。
/// - 挂载 `RootView`，由它根据当前 `Phase` 在登录页与主页之间路由。
@main
struct AppAttestScaffoldApp: App {

    @StateObject private var session: AppSession

    init() {
        // 顺序至关重要：必须先清理 Keychain 残留，再构建 session，避免 session 在 `bootstrap()`
        // 中读取到过期的 token。
        FirstLaunchFlag.bootstrapIfNeeded()
        _session = StateObject(wrappedValue: AppSession())
    }

    var body: some Scene {
        WindowGroup {
            RootView()
                .environmentObject(session)
                .task { await session.bootstrap() }
        }
    }
}
