import SwiftUI
import Alamofire

/// 登录后的主着陆页。
///
/// 定位是「设备/服务的运行态视图」: 招呼语 + Developer Tools 入口。
/// 服务配置(issuer 切换)统一收纳进 Developer Tools；账号资料与身份绑定
/// 属于「账号与安全」, 在 `SettingsView` 中维护, 此处不再重复展示。
///
/// `.refreshable` 直接复用 `AppSession.bootstrap()`, 因为它已经覆盖了令牌刷新与
/// identities 对齐。
struct HomeView: View {

    @EnvironmentObject private var session: AppSession

    @State private var showSettings = false

    var body: some View {
        NavigationStack {
            Form {
                titleSection
                greetingSection
                devToolsSection
            }
            .toolbar(.hidden, for: .navigationBar)
            .contentMargins(.top, 0, for: .scrollContent)
            .refreshable {
                await session.bootstrap()
            }
            .sheet(isPresented: $showSettings) {
                SettingsView()
                    .environmentObject(session)
            }
        }
    }

    // MARK: - 各分组 Section

    /// 首行: 大标题 + 头像按钮。头像点击进入「账号与安全」。
    private var titleSection: some View {
        Section {
            HStack(alignment: .center) {
                Text("首页")
                    .font(.largeTitle.bold())
                Spacer()
                Button {
                    showSettings = true
                } label: {
                    AvatarView(
                        urlString: session.phase.account?.profile.avatarUrl,
                        size: 32
                    )
                }
                .accessibilityLabel("打开账号与安全")
            }
            .listRowBackground(Color.clear)
            .listRowInsets(EdgeInsets(top: 10, leading: 0, bottom: 0, trailing: 0))
            .listRowSeparator(.hidden)
        }
    }

    @ViewBuilder
    private var greetingSection: some View {
        if let account = session.phase.account {
            Section {
                HStack(spacing: 16) {
                    AvatarView(urlString: account.profile.avatarUrl, size: 56)
                    VStack(alignment: .leading, spacing: 4) {
                        Text("你好, \(account.profile.displayName)")
                            .font(.headline)
                        Text(session.phase.isAnonymous ? "匿名用户" : "已绑定账号")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 4)
            }
        }
    }

    /// Developer Tools 入口。仅当 id_token 中包含 developer tag 时显示。
    @ViewBuilder
    private var devToolsSection: some View {
        if session.phase.isDeveloper {
            Section {
                NavigationLink {
                    DeveloperToolsView()
                        .environmentObject(session)
                } label: {
                    HStack {
                        Image(systemName: "wrench.and.screwdriver")
                            .foregroundStyle(.tint)
                        Text("Developer Tools")
                        Spacer()
                    }
                }
            } footer: {
                Text("诊断信息、受保护资源调试、令牌管理与数据抹除。")
            }
        }
    }
}

#Preview {
    HomeView()
        .environmentObject(AppSession())
}
