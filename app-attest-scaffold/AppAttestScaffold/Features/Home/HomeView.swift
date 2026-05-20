import SwiftUI
import Alamofire

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
    @State private var showProtectedResource = false

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
                protectedResourceSection
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
            .sheet(isPresented: $showProtectedResource) {
                ProtectedResourceSheet()
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

    /// 受保护资源调用入口。点击进入 `ProtectedResourceSheet`，可任意输入 URL 发起
    /// 携带 Bearer Token 的 GET 请求，同时隐含触发 AT 临期自动续期。
    private var protectedResourceSection: some View {
        Section {
            Button {
                showProtectedResource = true
            } label: {
                HStack {
                    Image(systemName: "network")
                        .foregroundStyle(.tint)
                    Text("请求受保护资源")
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .foregroundStyle(.primary)
            .disabled(session.phase.account == nil)
        } header: {
            Text("受保护资源")
        } footer: {
            Text("任意输入 URL 发起 GET 请求，默认为 `/user/identities`。请求前会检查 AT 是否临期，如需会自动走一轮 App Assertion 续期后再发起。")
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

/// 「受保护资源」面板的响应展示结构体。仅服务于 `ProtectedResourceSheet` 表现层，
/// 不进入 `AppSession` 指令 —— 续期效果不需要 `didAutoRefresh` 字段表述，诊断面板
/// 顶部的 `expiresAt` 倒计时会在续期后自动跳变，足以反馈“刚刚发生了一次续期”。
struct ProtectedResourceResponse: Equatable {
    let statusCode: Int
    let contentType: String?
    let body: String
    let byteCount: Int
    let elapsedMs: Int
}

/// 「受保护资源」面板。提供一个可输入任意 URL 的 GET 请求调试入口，默认使用当前
/// Account Service 的 `/user/identities`。同时作为 AT 临期自动续期能力的可视化演示：
/// 在面板顶部实时圈出当前 AT 的倒计时—— 临期发请求会触发一次续期，续期后倒计时
/// 会“跳回”完整 expires_in，无需额外的 didAutoRefresh 标签。
struct ProtectedResourceSheet: View {

    @EnvironmentObject private var session: AppSession
    @Environment(\.dismiss) private var dismiss

    @State private var urlDraft: String = Self.defaultURL()
    @State private var isLoading = false
    @State private var response: ProtectedResourceResponse?
    @State private var errorMessage: String?

    var body: some View {
        NavigationStack {
            Form {
                tokenCountdownSection

                Section {
                    TextField("https://...", text: $urlDraft, axis: .vertical)
                        .textInputAutocapitalization(.never)
                        .autocorrectionDisabled()
                        .keyboardType(.URL)
                        .font(.callout.monospaced())
                        .lineLimit(1...3)
                    Button {
                        urlDraft = Self.defaultURL()
                    } label: {
                        HStack {
                            Image(systemName: "arrow.uturn.backward")
                            Text("恢复默认 (`/user/identities`)")
                            Spacer()
                        }
                        .font(.footnote)
                    }
                    .disabled(urlDraft.trimmingCharacters(in: .whitespacesAndNewlines) == Self.defaultURL())
                } header: {
                    Text("请求 URL")
                } footer: {
                    Text("仅支持 GET。请求会携带当前访问令牌 (`Authorization: Bearer ...`)。临期会自动走一轮 App Assertion 续期，面板顶部的倒计时会随之跳回。")
                }

                Section {
                    Button {
                        Task { await fire() }
                    } label: {
                        if isLoading {
                            ProgressView()
                                .frame(maxWidth: .infinity)
                        } else {
                            Text("发起 GET 请求")
                                .fontWeight(.semibold)
                                .frame(maxWidth: .infinity)
                        }
                    }
                    .buttonStyle(.borderedProminent)
                    .controlSize(.large)
                    .disabled(!isValidURL || isLoading)
                    .listRowInsets(EdgeInsets())
                    .listRowBackground(Color.clear)
                }

                if let resp = response {
                    resultSection(resp)
                }

                if let err = errorMessage {
                    Section {
                        Text(err)
                            .font(.callout)
                            .foregroundStyle(.red)
                    } header: {
                        Text("错误")
                    } footer: {
                        Text("传输层错误仅在网络不可达等场景出现。服务返回的 4xx / 5xx 会以响应结果呈现。")
                    }
                }
            }
            .navigationTitle("请求受保护资源")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }

    // MARK: - 顶部令牌倒计时

    /// 仅读取 `SessionStore` 最新 token bundle，在顶部以 1 秒频率刷新倒计时。
    /// 业务请求发生续期后 SessionStore 会写入新 bundle，倒计时会“跳回”完整 expires_in。
    @ViewBuilder
    private var tokenCountdownSection: some View {
        Section {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let bundle = SessionStore.loadTokenBundle() ?? session.phase.tokens
                if let bundle {
                    let remaining = max(0, Int(bundle.expiresAt.timeIntervalSince(context.date)))
                    HStack {
                        Image(systemName: remaining < 60 ? "clock.badge.exclamationmark" : "clock")
                            .foregroundStyle(remaining < 60 ? .orange : .secondary)
                        Text("AT 剩余")
                        Spacer()
                        Text(formatDuration(remaining))
                            .font(.callout.monospacedDigit())
                            .foregroundStyle(remaining < 60 ? .orange : .secondary)
                    }
                } else {
                    HStack {
                        Image(systemName: "questionmark.circle")
                            .foregroundStyle(.secondary)
                        Text("未取到访问令牌")
                            .foregroundStyle(.secondary)
                    }
                }
            }
        } header: {
            Text("访问令牌状态")
        } footer: {
            Text("请求会在临期时自动续期。倒计时跳回到完整 expires_in 即表示刚发生过一次续期。")
        }
    }

    // MARK: - 响应展示

    @ViewBuilder
    private func resultSection(_ resp: ProtectedResourceResponse) -> some View {
        Section {
            HStack {
                Text("状态码")
                Spacer()
                Text("\(resp.statusCode)")
                    .font(.callout.monospacedDigit())
                    .foregroundStyle(statusColor(resp.statusCode))
            }
            HStack {
                Text("耗时")
                Spacer()
                Text("\(resp.elapsedMs) ms")
                    .font(.callout.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            HStack {
                Text("响应体大小")
                Spacer()
                Text("\(resp.byteCount) B")
                    .font(.callout.monospacedDigit())
                    .foregroundStyle(.secondary)
            }
            if let ct = resp.contentType {
                HStack {
                    Text("Content-Type")
                    Spacer()
                    Text(ct)
                        .font(.caption.monospaced())
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                        .truncationMode(.middle)
                }
            }
        } header: {
            Text("响应概要")
        }

        Section {
            ScrollView(.horizontal, showsIndicators: true) {
                Text(prettyBody(resp))
                    .font(.caption.monospaced())
                    .textSelection(.enabled)
                    .padding(.vertical, 4)
            }
        } header: {
            Text("响应体")
        } footer: {
            Text("长按可选中复制。JSON 响应已自动格式化。")
        }
    }

    // MARK: - 交互

    /// 发起一次携带 Bearer Token 的 GET 请求。本面板作为未来业务 Client 开发的范本：
    /// 不再走 `AppSession.fetchProtectedResource` 等业务出口，而是直接同持 `(auth, http)`
    /// 双视图：`session.auth.getAccessToken()` + `session.http.request(...)`。
    private func fire() async {
        guard let url = URL(string: urlDraft.trimmingCharacters(in: .whitespacesAndNewlines)) else {
            errorMessage = "URL 格式不正确"
            return
        }
        isLoading = true
        response = nil
        errorMessage = nil
        let started = Date()
        do {
            let at = try await session.auth.getAccessToken()
            let dataResponse = await session.http.request(
                url,
                method: .get,
                headers: HTTPHeaders([
                    "Authorization": "Bearer \(at)",
                    "Accept": "application/json"
                ])
            )
            .serializingData(emptyResponseCodes: [200, 204, 205])
            .response
            let elapsedMs = Int(Date().timeIntervalSince(started) * 1000)
            if let httpResponse = dataResponse.response {
                let data = (try? dataResponse.result.get()) ?? dataResponse.data ?? Data()
                response = ProtectedResourceResponse(
                    statusCode: httpResponse.statusCode,
                    contentType: httpResponse.value(forHTTPHeaderField: "Content-Type"),
                    body: String(data: data, encoding: .utf8) ?? "",
                    byteCount: data.count,
                    elapsedMs: elapsedMs
                )
            } else if let underlying = dataResponse.error?.underlyingError {
                errorMessage = (underlying as NSError).localizedDescription
            } else if let afError = dataResponse.error {
                errorMessage = afError.localizedDescription
            } else {
                errorMessage = "未知传输错误"
            }
        } catch let error as APIError {
            errorMessage = error.errorDescription ?? "请求失败"
        } catch {
            errorMessage = error.localizedDescription
        }
        isLoading = false
    }

    // MARK: - 辅助

    private var isValidURL: Bool {
        let raw = urlDraft.trimmingCharacters(in: .whitespacesAndNewlines)
        guard !raw.isEmpty,
              let url = URL(string: raw),
              let scheme = url.scheme?.lowercased(),
              scheme == "https" || scheme == "http",
              url.host != nil else {
            return false
        }
        return true
    }

    private static func defaultURL() -> String {
        return AppConfiguration.accountServiceBase + "/user/identities"
    }

    private func statusColor(_ code: Int) -> Color {
        switch code {
        case 200...299: return .green
        case 300...399: return .blue
        case 400...499: return .orange
        default: return .red
        }
    }

    /// 尝试将响应体作为 JSON 重新序列化为缩进 2 空格的可读格式。
    /// 解析失败（非 JSON）时返回原始文本，使面板同时适用于 JSON 外的接口。
    private func prettyBody(_ resp: ProtectedResourceResponse) -> String {
        guard !resp.body.isEmpty,
              let data = resp.body.data(using: .utf8),
              let obj = try? JSONSerialization.jsonObject(with: data, options: [.fragmentsAllowed]),
              let pretty = try? JSONSerialization.data(withJSONObject: obj, options: [.prettyPrinted, .sortedKeys]),
              let str = String(data: pretty, encoding: .utf8) else {
            return resp.body.isEmpty ? "<空响应体>" : resp.body
        }
        return str
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
