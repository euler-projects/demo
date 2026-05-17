import SwiftUI

/// 顶层路由。根据 `AppSession.Phase` 在登录页与主页之间切换。
///
/// 切换通过 `.animation` 包裹，使 session phase 变化（例如登录或登出）时 UI 会进行
/// 交叉淡入淡出过渡。错误以 banner 弹窗的形式呈现，绑定在 `lastError` 上 ——
/// UI 组件也可以本地处理错误，以获得更细粒度的交互体验。
struct RootView: View {

    @EnvironmentObject private var session: AppSession

    var body: some View {
        Group {
            switch session.phase {
            case .bootstrapping:
                BootstrapView()
            case .unauthenticated:
                LoginView()
            case .signedIn:
                HomeView()
            }
        }
        .animation(.default, value: session.phase)
        .alert(
            "提示",
            isPresented: .init(
                get: { session.lastError != nil },
                set: { if !$0 { session.lastError = nil } }
            ),
            actions: {
                Button("好的", role: .cancel) { session.lastError = nil }
            },
            message: {
                Text(session.lastError ?? "")
            }
        )
    }
}

/// 冷启动闪屏。刻意保持极简 —— 除非 Discovery 网络较慢，bootstrap 应在 500ms 内完成。
private struct BootstrapView: View {
    var body: some View {
        VStack(spacing: 16) {
            ProgressView()
                .controlSize(.large)
            Text("正在恢复会话…")
                .foregroundStyle(.secondary)
                .font(.callout)
        }
        .frame(maxWidth: .infinity, maxHeight: .infinity)
        .background(Color(.systemBackground))
    }
}

#Preview {
    RootView()
        .environmentObject(AppSession())
}
