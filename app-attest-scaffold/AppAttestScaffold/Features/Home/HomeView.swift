import SwiftUI

/// 登录后的主着陆页。
///
/// 定位是「设备/服务的运行态视图」: 招呼语 + 服务配置(issuer 切换)
/// + 诊断(kid / 访问令牌 / 手动续期)。账号资料与身份绑定属于「账号与安全」, 在
/// `SettingsView` 中维护, 此处不再重复展示。
///
/// `.refreshable` 直接复用 `AppSession.bootstrap()`, 因为它已经覆盖了令牌刷新与
/// identities 对齐。
struct HomeView: View {

    @EnvironmentObject private var session: AppSession

    @State private var showSettings = false
    @State private var showIssuerEditor = false

    /// 复制反馈: 点击复制后短暂显示对勾, 然后恢复图标。
    @State private var copiedField: String?

    @State private var isRefreshingTokens = false
    /// 手动续期后的一次性提示 ("已于 xx:xx 续期成功")。仅诊断用, 不走 alert
    /// 避免打断调试节奏; 失败走 `session.lastError` 统一渠道。
    @State private var refreshSuccessAt: Date?

    var body: some View {
        NavigationStack {
            Form {
                titleSection
                greetingSection
                serviceSection
                diagnosticsSection
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
            .sheet(isPresented: $showIssuerEditor) {
                IssuerEditorSheet()
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

    /// 服务配置: 只读展示 issuer 与账号服务基地址, 点击打开 IssuerEditorSheet 修改。
    private var serviceSection: some View {
        Section {
            Button {
                showIssuerEditor = true
            } label: {
                VStack(alignment: .leading, spacing: 8) {
                    HStack(alignment: .firstTextBaseline) {
                        Text("Issuer")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                            .frame(width: 96, alignment: .leading)
                        Text(AppConfiguration.issuer)
                            .font(.callout.monospaced())
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                    HStack(alignment: .firstTextBaseline) {
                        Text("Account Service")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                            .frame(width: 96, alignment: .leading)
                        Text(AppConfiguration.accountServiceBaseURL)
                            .font(.callout.monospaced())
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                            .truncationMode(.middle)
                            .frame(maxWidth: .infinity, alignment: .leading)
                    }
                }
                .frame(maxWidth: .infinity, alignment: .leading)
            }
            .buttonStyle(.plain)
            .contentShape(Rectangle())
        } header: {
            Text("服务配置")
        } footer: {
            Text("Issuer 作为授权服务与 OIDC Discovery 的根 URL; Account Service 作为 `/user/identities` 等账号身份管理接口的根 URL。点击可修改, 修改后会清除当前会话。")
        }
    }

    @ViewBuilder
    private var diagnosticsSection: some View {
        Section {
            if let kid = session.phase.account?.appAttestKey?.kid {
                HStack {
                    Text("设备 KeyID")
                    Spacer()
                    Text(kid)
                        .font(.caption.monospaced())
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: 160, alignment: .trailing)
                    copyButton(value: kid, field: "kid")
                }
            }
            if let tokens = session.phase.tokens {
                HStack {
                    Text("访问令牌")
                    Spacer()
                    Text(tokens.accessToken)
                        .font(.caption.monospaced())
                        .lineLimit(1)
                        .truncationMode(.tail)
                        .frame(maxWidth: 180, alignment: .trailing)
                    copyButton(value: tokens.accessToken, field: "token")
                }
                HStack {
                    Text("令牌过期")
                    Spacer()
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        let remaining = max(0, Int(tokens.expiresAt.timeIntervalSince(context.date)))
                        Text(formatDuration(remaining))
                            .font(.caption.monospacedDigit())
                            .foregroundStyle(remaining < 60 ? .orange : .secondary)
                    }
                }
            }
            Button {
                Task { await manualRefresh() }
            } label: {
                HStack {
                    Text("手动续期令牌")
                    Spacer()
                    if isRefreshingTokens {
                        ProgressView()
                    } else {
                        Image(systemName: "arrow.clockwise")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }
            }
            .disabled(isRefreshingTokens || session.phase.account == nil)
        } header: {
            Text("诊断")
        } footer: {
            VStack(alignment: .leading, spacing: 4) {
                Text("仅供调试。生产构建可移除该分组。")
                if let at = refreshSuccessAt {
                    Text("已于 \(at.formatted(date: .omitted, time: .standard)) 续期成功。")
                        .foregroundStyle(.green)
                }
                if let err = session.lastError, !isRefreshingTokens, refreshSuccessAt == nil {
                    Text(err)
                        .foregroundStyle(.red)
                }
            }
        }
    }

    // MARK: - 工具方法

    /// 调用 `AppSession.manualRefreshTokens()` 并维护 loading / 提示状态。
    /// 成功后记录时间戳供 footer 展示; 失败依赖 `session.lastError` 走常规错误提示通道。
    private func manualRefresh() async {
        isRefreshingTokens = true
        refreshSuccessAt = nil
        await session.manualRefreshTokens()
        isRefreshingTokens = false
        if session.lastError == nil {
            refreshSuccessAt = Date()
        }
    }

    /// 复制按钮: 点击后图标变为对勾 1.5 秒后恢复, 配合触感反馈。
    private func copyButton(value: String, field: String) -> some View {
        Button {
            UIPasteboard.general.string = value
            withAnimation { copiedField = field }
            let generator = UIImpactFeedbackGenerator(style: .light)
            generator.impactOccurred()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                withAnimation {
                    if copiedField == field { copiedField = nil }
                }
            }
        } label: {
            Image(systemName: copiedField == field ? "checkmark" : "doc.on.doc")
                .font(.caption)
                .foregroundStyle(copiedField == field ? .green : .secondary)
        }
        .buttonStyle(.plain)
    }

    /// 将秒数格式化为“XX天XX时XX分XX秒”可读形式, 省略值为 0 的高位。
    private func formatDuration(_ totalSeconds: Int) -> String {
        let d = totalSeconds / 86400
        let h = (totalSeconds % 86400) / 3600
        let m = (totalSeconds % 3600) / 60
        let s = totalSeconds % 60
        var parts: [String] = []
        if d > 0 { parts.append("\(d)天") }
        if h > 0 { parts.append("\(h)时") }
        if m > 0 { parts.append("\(m)分") }
        parts.append("\(s)秒")
        return parts.joined()
    }
}

#Preview {
    HomeView()
        .environmentObject(AppSession())
}
