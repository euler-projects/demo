import SwiftUI

/// 登录前的着陆页。
///
/// 该页面提供两条入口路径:
/// - **手机号登录** — 主流程, 始终可见。打开 OTP sheet, 驱动 App Attest 用法 2
///   (`grant_type=otp`)。
/// - **匿名进入** — 次要的、用户主动选择的路径。仅在首次启动时显示
///   (`AppDataStore.get(String.self, for: .kid) == nil && AppDataStore.get(Account.self, for: .account) == nil`),
///   触发 App Attest 用法 1 (`anonymousSignIn`)。渲染为主 CTA 下方的低强度文字链接,
///   让真正已有账号的用户默认落在手机号登录路径上。
///
/// 视图刻意保持极简; 真实生产部署通常会在外面包一层品牌元素与隐私政策声明。
struct LoginView: View {

    @EnvironmentObject private var session: AppSession
    @State private var showOTPSheet = false
    @State private var showIssuerSheet = false
    @State private var isWorking = false

    var body: some View {
        NavigationStack {
            ZStack {
                Color(.systemBackground).ignoresSafeArea()

                VStack(spacing: 32) {
                    Spacer()

                    VStack(spacing: 12) {
                        Image(systemName: "lock.shield.fill")
                            .resizable()
                            .scaledToFit()
                            .frame(width: 72, height: 72)
                            .foregroundStyle(.tint)
                        Text("App Attest 脚手架")
                            .font(.title.weight(.semibold))
                        Text("基于 Apple App Attest 的安全登录示例")
                            .font(.callout)
                            .foregroundStyle(.secondary)
                            .multilineTextAlignment(.center)
                    }

                    Spacer()

                    VStack(spacing: 16) {
                        // 主 CTA — 手机号登录。OTP sheet 自身负责驱动 App Attest 用法 2;
                        // 本视图仅持有触发入口。
                        Button(action: { showOTPSheet = true }) {
                            Label("手机号登录", systemImage: "phone.fill")
                                .frame(maxWidth: .infinity)
                        }
                        .buttonStyle(.borderedProminent)
                        .controlSize(.large)
                        .disabled(isWorking)

                        // 次要路径 — 匿名进入。渲染为低强度的文字链接, 仅当本次安装从未绑定过
                        // 任何账号时才出现(首次启动 flag 在 bootstrap 时翻转, 因此这里改用
                        // 持久化状态作为闸门)。
                        if allowAnonymous {
                            Button(action: anonymousTap) {
                                if isWorking {
                                    ProgressView()
                                } else {
                                    Text("或 匿名进入体验")
                                        .font(.footnote)
                                        .foregroundStyle(.secondary)
                                }
                            }
                            .buttonStyle(.plain)
                            .disabled(isWorking)
                            .accessibilityLabel("匿名进入")
                        }
                    }
                    .padding(.horizontal, 24)

                    Button {
                        showIssuerSheet = true
                    } label: {
                        Text("Issuer: \(AppConfiguration.issuer)")
                            .font(.caption2)
                            .foregroundStyle(.tertiary)
                            .underline()
                    }
                    .padding(.bottom, 8)
                }
            }
            .navigationTitle("欢迎")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button {
                        showIssuerSheet = true
                    } label: {
                        Image(systemName: "gearshape")
                    }
                    .accessibilityLabel("服务配置")
                }
            }
            .sheet(isPresented: $showOTPSheet) {
                PhoneOTPSheet(mode: .signIn)
            }
            .sheet(isPresented: $showIssuerSheet) {
                IssuerEditorSheet()
                    .environmentObject(session)
            }
        }
    }

    /// 仅当「首次启动标志位未翻转」且「无持久化 kid / 账号」时才允许匿名登录。
    /// 与 App-Attest-Login.md §2.4("仅限首次使用")的语义对齐。
    ///
    /// `hasLaunchedBefore` 在首次成功启动后即置为 `true`；退出登录时
    /// 不会重置，因此登出后匿名入口不再出现。只有「抹掉所有数据」才能使其恢复。
    private var allowAnonymous: Bool {
        !AppDataStore.get(Bool.self, for: AppDataStore.Keys.hasLaunchedBefore, default: false)
            && AppDataStore.get(String.self, for: AppDataStore.Keys.kid) == nil
            && AppDataStore.get(Account.self, for: AppDataStore.Keys.account) == nil
    }

    private func anonymousTap() {
        isWorking = true
        Task {
            await session.anonymousSignIn()
            isWorking = false
        }
    }
}

#Preview {
    LoginView()
        .environmentObject(AppSession())
}

