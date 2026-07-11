import SwiftUI

/// 轻量级的服务配置编辑 sheet, 由 `LoginView`、`DeveloperToolsView` 共同复用。
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

    private enum Field: Hashable {
        case issuer
        case accountService
    }
    @FocusState private var focusedField: Field?

    var body: some View {
        NavigationStack {
            Form {
                Section {
                    TextField("https://...", text: $issuerDraft)
                        .focused($focusedField, equals: .issuer)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.next)
                        .onSubmit { focusedField = .accountService }
                } header: {
                    Text("Issuer")
                } footer: {
                    Text("授权服务 issuer, 作为 OIDC Discovery 与 `/oauth2/token`、`/oauth2/challenge` 等端点的根 URL。")
                }
                .listRowBackground(
                    RoundedRectangle(cornerRadius: 10)
                        .fill(Color(.secondarySystemGroupedBackground))
                )
                .contentShape(Rectangle())
                .onTapGesture { focusedField = .issuer }

                Section {
                    TextField("https://...", text: $accountServiceDraft)
                        .focused($focusedField, equals: .accountService)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .submitLabel(.done)
                        .onSubmit { focusedField = nil }
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
