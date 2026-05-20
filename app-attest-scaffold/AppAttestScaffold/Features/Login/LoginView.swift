import SwiftUI

/// 登录前的着陆页。
///
/// 该页面提供两条入口路径:
/// - **手机号登录** — 主流程, 始终可见。打开 OTP sheet, 驱动 App Attest 用法 2
///   (`grant_type=otp`)。
/// - **匿名进入** — 次要的、用户主动选择的路径。仅在首次启动时显示
///   (`SessionStore.loadKid() == nil && AccountStore.load() == nil`),
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
    /// `FirstLaunchFlag.hasLaunchedBefore` 在首次成功启动后即置为 `true`；退出登录时
    /// 不会重置，因此登出后匿名入口不再出现。只有「抹掉所有数据」才能使其恢复。
    private var allowAnonymous: Bool {
        !FirstLaunchFlag.hasLaunchedBefore
            && SessionStore.loadKid() == nil
            && AccountStore.load() == nil
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

/// 轻量级的服务配置编辑 sheet, 由 `LoginView`、`HomeView` 共同复用。
/// 同时维护授权服务 issuer 与用户账号服务 (Account Service) 基地址。
/// 新值通过 `AppSession.updateServiceConfiguration(issuer:accountServiceBaseURL:)` 写入持久化,
/// 同时使 OIDC Discovery 缓存失效, 并把当前会话登出, 以保证一致性。
struct IssuerEditorSheet: View {

    @EnvironmentObject private var session: AppSession
    @Environment(\.dismiss) private var dismiss

    @State private var issuerDraft: String = AppConfiguration.issuer
    @State private var accountServiceDraft: String = AppConfiguration.accountServiceBaseURL
    @State private var showConfirm = false
    @State private var isApplying = false

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("https://...", text: $issuerDraft)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.next)
                } header: {
                    Text("Issuer")
                } footer: {
                    Text("授权服务 issuer, 作为 OIDC Discovery 与 `/oauth2/token`、`/oauth2/challenge` 等端点的根 URL。")
                }

                Section {
                    TextField("https://...", text: $accountServiceDraft)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.done)
                    Button {
                        accountServiceDraft = trimmedIssuer
                    } label: {
                        HStack {
                            Image(systemName: "arrow.up.doc.on.clipboard")
                            Text("使用 Issuer 配置")
                            Spacer()
                        }
                        .font(.footnote)
                    }
                    .disabled(!canCopyFromIssuer)
                } header: {
                    Text("Account Service")
                } footer: {
                    Text("用户账号服务基地址, 作为 `/user/identities` 等账号身份管理接口的根 URL。\ndemo 环境与授权服务合部署, 可点击上方「使用 Issuer 配置」同步为当前 Issuer。")
                }

                Section {
                    Button {
                        showConfirm = true
                    } label: {
                        if isApplying {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        } else {
                            Text("保存")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(!hasChanges || !isValidConfiguration || isApplying)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                Section {
                    Button(role: .destructive) {
                        restoreDefaults()
                    } label: {
                        HStack {
                            Spacer()
                            Text("恢复默认")
                            Spacer()
                        }
                    }
                    .disabled(isAtDefaults || isApplying)
                } footer: {
                    Text("将两个输入框重置为默认值，仍需点击「保存」后才会生效。")
                }
            }
            .navigationTitle("服务配置")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("取消") { dismiss() }
                }
            }
            .confirmationDialog(
                "切换服务配置会清除会话",
                isPresented: $showConfirm,
                titleVisibility: .visible
            ) {
                Button("切换", role: .destructive) { apply() }
                Button("取消", role: .cancel) {}
            } message: {
                Text("现有访问令牌与设备密钥将失效, 必须重新登录。")
            }
        }
    }

    private var trimmedIssuer: String {
        issuerDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var trimmedAccountService: String {
        accountServiceDraft.trimmingCharacters(in: .whitespacesAndNewlines)
    }

    private var hasChanges: Bool {
        let issuerChanged = !trimmedIssuer.isEmpty && trimmedIssuer != AppConfiguration.issuer
        let accountChanged = !trimmedAccountService.isEmpty
            && trimmedAccountService != AppConfiguration.accountServiceBaseURL
        return issuerChanged || accountChanged
    }

    /// 当前 Issuer 输入是否可作为 Account Service 的一键填充源。
    /// 仅要求格式有效，不要求与当前 Account Service draft 不同 —— 即使相同
    /// 也允许点击，避免按钮状态频繁切换引起迷惑。
    private var canCopyFromIssuer: Bool {
        Self.isValidHTTPURL(trimmedIssuer)
    }

    /// 两个 draft 是否均与默认值一致。
    private var isAtDefaults: Bool {
        trimmedIssuer == AppConfiguration.defaultIssuer
            && trimmedAccountService == AppConfiguration.defaultAccountServiceBaseURL
    }

    /// 把两个 draft 重置为默认值。仅修改表单状态，不会自动保存，
    /// 用户仍需点击「保存」才会写入 UserDefaults 并触发登出。
    private func restoreDefaults() {
        issuerDraft = AppConfiguration.defaultIssuer
        accountServiceDraft = AppConfiguration.defaultAccountServiceBaseURL
    }

    private var isValidConfiguration: Bool {
        return Self.isValidHTTPURL(trimmedIssuer) && Self.isValidHTTPURL(trimmedAccountService)
    }

    private static func isValidHTTPURL(_ raw: String) -> Bool {
        guard !raw.isEmpty,
              let url = URL(string: raw),
              let scheme = url.scheme?.lowercased(),
              scheme == "https" || scheme == "http",
              url.host != nil else {
            return false
        }
        return true
    }

    private func apply() {
        isApplying = true
        Task {
            await session.updateServiceConfiguration(
                issuer: trimmedIssuer,
                accountServiceBaseURL: trimmedAccountService
            )
            isApplying = false
            dismiss()
        }
    }
}
