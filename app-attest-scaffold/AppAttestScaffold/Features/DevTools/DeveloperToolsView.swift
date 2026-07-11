import SwiftUI
import UIKit
import Alamofire

/// Developer Tools 专用 tint，脱离 APP 主题控制体系，使用 iOS 系统默认色。
private let devToolsTint = Color(uiColor: .systemBlue)

// MARK: - Developer Tools

struct DeveloperToolsView: View {

    @EnvironmentObject private var session: AppSession

    @State private var deviceKeyID: String?
    @State private var userSub: String?
    @State private var tokenMasked: String?
    @State private var tokenExpiresAt: Date?
    @State private var hasIdToken: Bool = false
    @State private var idTokenMasked: String?
    @State private var isRefreshing: Bool = false
    @State private var refreshResult: String?
    @State private var showWipeConfirm: Bool = false
    @State private var claimsItem: DevToolsClaimsItem?
    @State private var showIssuerEditor: Bool = false
    @State private var showProtectedResource: Bool = false
    @State private var copiedField: String?

    var body: some View {
        Form {
            serviceConfigSection
            diagnosticsSection
            protectedResourceSection
            wipeSection
        }
        .navigationTitle("Developer Tools")
        .navigationBarTitleDisplayMode(.inline)
        .onAppear { loadDiagnostics() }
        .sheet(item: $claimsItem) { item in
            DevToolsTokenClaimsSheet(title: item.title, claimsJSON: item.json)
        }
        .sheet(isPresented: $showIssuerEditor) {
            IssuerEditorSheet()
                .environmentObject(session)
        }
        .sheet(isPresented: $showProtectedResource) {
            ProtectedResourceSheet()
                .environmentObject(session)
        }
        .confirmationDialog(
            "确认抹掉所有数据?",
            isPresented: $showWipeConfirm,
            titleVisibility: .visible
        ) {
            Button("抹掉所有数据", role: .destructive) {
                Task { await session.wipeAllData() }
            }
            Button("取消", role: .cancel) {}
        } message: {
            Text("将清除所有本地数据（令牌、设备密钥、账号快照）并恢复到刚安装状态。再次打开应用可重新匿名试用。")
        }
        .tint(devToolsTint)
    }

    // MARK: - 服务配置

    private var serviceConfigSection: some View {
        Section {
            configRow(title: "Issuer", value: AppConfiguration.issuer)
            configRow(title: "Account Service", value: AppConfiguration.accountServiceBaseURL)
            Button {
                showIssuerEditor = true
            } label: {
                HStack {
                    Text("编辑配置")
                    Spacer()
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
        } header: {
            Text("服务配置")
        }
    }

    private func configRow(title: String, value: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(title)
                .font(.caption2)
                .foregroundStyle(.secondary)
            Text(value)
                .font(.system(.caption, design: .monospaced))
                .lineLimit(1)
                .truncationMode(.middle)
        }
    }

    // MARK: - 诊断

    private var diagnosticsSection: some View {
        Section {
            // 设备 Key ID
            diagRow(title: "设备 Key ID", value: deviceKeyID ?? "无", copyValue: deviceKeyID)

            // 用户 ID (sub)
            diagRow(title: "用户 ID", value: userSub ?? "无", copyValue: userSub)

            // 身份令牌 (id_token)
            if hasIdToken {
                Button {
                    showIdTokenClaims()
                } label: {
                    HStack {
                        Text("身份令牌")
                        Spacer()
                        Text(idTokenMasked ?? "无")
                            .font(.system(.caption, design: .monospaced))
                            .foregroundStyle(.secondary)
                            .lineLimit(1)
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }
                .foregroundStyle(.primary)
            }

            // 访问令牌 (脱敏)
            Button {
                showAccessTokenClaims()
            } label: {
                HStack {
                    Text("访问令牌")
                    Spacer()
                    Text(tokenMasked ?? "无")
                        .font(.system(.caption, design: .monospaced))
                        .foregroundStyle(.secondary)
                        .lineLimit(1)
                    Image(systemName: "chevron.right")
                        .font(.caption)
                        .foregroundStyle(.tertiary)
                }
            }
            .foregroundStyle(.primary)
            .disabled(tokenMasked == nil)

            // 令牌过期
            HStack {
                Text("令牌过期")
                Spacer()
                if let expiresAt = tokenExpiresAt {
                    TimelineView(.periodic(from: .now, by: 1)) { context in
                        let remaining = max(0, Int(expiresAt.timeIntervalSince(context.date)))
                        Text(formatDuration(remaining))
                            .font(.system(.caption, design: .monospaced))
                            .foregroundStyle(remaining < 60 ? .orange : .secondary)
                    }
                } else {
                    Text("无")
                        .font(.caption)
                        .foregroundStyle(.secondary)
                }
            }

            // 手动续期令牌
            Button {
                Task { await manualRefresh() }
            } label: {
                HStack {
                    Text("手动续期令牌")
                    Spacer()
                    if isRefreshing {
                        ProgressView()
                    } else {
                        Image(systemName: "arrow.clockwise")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }
            }
            .disabled(isRefreshing || deviceKeyID == nil)

            if let result = refreshResult {
                Text(result)
                    .font(.caption2)
                    .foregroundStyle(result.contains("成功") ? .green : .red)
            }
        } header: {
            Text("诊断")
        }
    }

    // MARK: - 受保护资源

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

    // MARK: - 抹掉数据

    private var wipeSection: some View {
        Section {
            Button(role: .destructive) {
                showWipeConfirm = true
            } label: {
                HStack {
                    Spacer()
                    Text("抹掉所有数据")
                    Spacer()
                }
            }
        } footer: {
            Text("清除所有本地数据并恢复到刚安装状态，再次打开应用可重新匿名试用。")
        }
    }

    // MARK: - Helper

    private func diagRow(title: String, value: String, copyValue: String?) -> some View {
        HStack {
            Text(title)
            Spacer()
            Text(value)
                .font(.system(.caption, design: .monospaced))
                .foregroundStyle(.secondary)
                .lineLimit(1)
                .truncationMode(.middle)
            if let copyValue, !copyValue.isEmpty {
                copyButton(value: copyValue, field: title)
            }
        }
    }

    private func copyButton(value: String, field: String) -> some View {
        Button {
            UIPasteboard.general.string = value
            withAnimation { copiedField = field }
            UIImpactFeedbackGenerator(style: .light).impactOccurred()
            DispatchQueue.main.asyncAfter(deadline: .now() + 1.5) {
                withAnimation { if copiedField == field { copiedField = nil } }
            }
        } label: {
            Image(systemName: copiedField == field ? "checkmark" : "doc.on.doc")
                .font(.caption)
                .foregroundStyle(copiedField == field ? .green : .secondary)
        }
        .buttonStyle(.plain)
    }

    private func loadDiagnostics() {
        guard case let .signedIn(result) = session.phase else {
            deviceKeyID = nil
            userSub = nil
            tokenMasked = nil
            tokenExpiresAt = nil
            hasIdToken = false
            idTokenMasked = nil
            return
        }
        deviceKeyID = result.account.appAttestKey?.kid
        // Decode sub from access token JWT payload
        userSub = extractSub(from: result.tokens.accessToken)
        tokenMasked = maskToken(result.tokens.accessToken)
        tokenExpiresAt = result.tokens.expiresAt
        // Check for id_token
        if let idToken = result.tokens.idToken {
            hasIdToken = true
            idTokenMasked = maskToken(idToken)
        } else {
            hasIdToken = false
            idTokenMasked = nil
        }
    }

    private func manualRefresh() async {
        isRefreshing = true
        refreshResult = nil
        await session.manualRefreshTokens()
        isRefreshing = false
        if session.lastError == nil {
            refreshResult = "续期成功 \(Date().formatted(date: .omitted, time: .standard))"
            // Reload diagnostics with fresh tokens
            loadDiagnostics()
        } else {
            refreshResult = session.lastError
        }
    }

    private func showAccessTokenClaims() {
        guard case let .signedIn(result) = session.phase else { return }
        let json = decodeJWTPayload(result.tokens.accessToken)
        claimsItem = DevToolsClaimsItem(title: "Access Token Claims", json: json)
    }

    private func showIdTokenClaims() {
        guard case let .signedIn(result) = session.phase,
              let idToken = result.tokens.idToken else { return }
        let json = decodeJWTPayload(idToken)
        claimsItem = DevToolsClaimsItem(title: "Identity Token Claims", json: json)
    }

    private func extractSub(from jwt: String) -> String? {
        let parts = jwt.split(separator: ".")
        guard parts.count >= 2 else { return nil }
        var base64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 { base64.append("=") }
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data) as? [String: Any],
              let sub = json["sub"] as? String else { return nil }
        return sub
    }

    private func decodeJWTPayload(_ jwt: String) -> String {
        let parts = jwt.split(separator: ".")
        guard parts.count >= 2 else { return "令牌格式无效" }
        var base64 = String(parts[1])
            .replacingOccurrences(of: "-", with: "+")
            .replacingOccurrences(of: "_", with: "/")
        while base64.count % 4 != 0 { base64.append("=") }
        guard let data = Data(base64Encoded: base64),
              let json = try? JSONSerialization.jsonObject(with: data),
              let pretty = try? JSONSerialization.data(withJSONObject: json, options: [.prettyPrinted, .sortedKeys]),
              let str = String(data: pretty, encoding: .utf8)
        else { return "解码失败" }
        return str
    }

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

    private func maskToken(_ token: String) -> String {
        guard token.count > 12 else { return String(repeating: "•", count: token.count) }
        let prefix = token.prefix(6)
        let suffix = token.suffix(6)
        return "\(prefix)•••\(suffix)"
    }
}

// MARK: - Token Claims Item

private struct DevToolsClaimsItem: Identifiable {
    let id = UUID()
    let title: String
    let json: String
}

// MARK: - Token Claims Sheet

private struct DevToolsTokenClaimsSheet: View {
    let title: String
    let claimsJSON: String
    @Environment(\.dismiss) private var dismiss

    var body: some View {
        NavigationStack {
            VStack(spacing: 0) {
                DevToolsJSONCodeView(json: claimsJSON)
                    .padding(16)
                Spacer()
            }
            .navigationTitle(title)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("关闭") { dismiss() }
                }
            }
        }
    }
}

// MARK: - Selectable Code Text View

private class DevToolsNonWrappingTextView: UITextView {
    override var intrinsicContentSize: CGSize {
        textContainer.size.width = CGFloat.greatestFiniteMagnitude
        layoutManager.ensureLayout(for: textContainer)
        let rect = layoutManager.usedRect(for: textContainer)
        return CGSize(
            width: ceil(rect.width) + textContainerInset.left + textContainerInset.right,
            height: ceil(rect.height) + textContainerInset.top + textContainerInset.bottom
        )
    }
}

private struct DevToolsSelectableCodeTextView: UIViewRepresentable {
    let attributedText: NSAttributedString

    func makeUIView(context: Context) -> DevToolsNonWrappingTextView {
        let textView = DevToolsNonWrappingTextView()
        textView.isEditable = false
        textView.isSelectable = true
        textView.isScrollEnabled = false
        textView.backgroundColor = .clear
        textView.textContainerInset = .zero
        textView.textContainer.lineFragmentPadding = 0
        textView.textContainer.widthTracksTextView = false
        textView.textContainer.size.width = CGFloat.greatestFiniteMagnitude
        textView.attributedText = attributedText
        textView.setContentCompressionResistancePriority(.defaultLow, for: .horizontal)
        textView.setContentHuggingPriority(.defaultLow, for: .horizontal)
        return textView
    }

    func updateUIView(_ textView: DevToolsNonWrappingTextView, context: Context) {
        textView.attributedText = attributedText
        textView.textContainer.size.width = CGFloat.greatestFiniteMagnitude
        textView.invalidateIntrinsicContentSize()
    }
}

// MARK: - 共享 JSON 代码展示组件

/// 语法高亮 JSON + 水平滚动 + 高度自适应。
private struct DevToolsJSONCodeView: View {
    let json: String

    var body: some View {
        ScrollView(.horizontal, showsIndicators: true) {
            DevToolsSelectableCodeTextView(attributedText: colorizedNSAttributedString())
                .fixedSize(horizontal: true, vertical: true)
        }
        .padding(16)
        .background(Color(.systemGray6))
        .clipShape(RoundedRectangle(cornerRadius: 10))
    }

    private func colorizedNSAttributedString() -> NSAttributedString {
        let monoFont = UIFont.monospacedSystemFont(ofSize: 12, weight: .regular)
        let result = NSMutableAttributedString()
        let lines = json.components(separatedBy: "\n")

        for (index, line) in lines.enumerated() {
            result.append(colorizeLineNS(line, font: monoFont))
            if index < lines.count - 1 {
                result.append(NSAttributedString(string: "\n", attributes: [.font: monoFont]))
            }
        }
        return result
    }

    private func colorizeLineNS(_ line: String, font: UIFont) -> NSAttributedString {
        let result = NSMutableAttributedString()
        let trimmed = line.trimmingCharacters(in: .whitespaces)
        let indent = String(line.prefix(while: { $0 == " " }))

        if !indent.isEmpty {
            result.append(NSAttributedString(string: indent, attributes: [.font: font, .foregroundColor: UIColor.label]))
        }

        if let colonRange = trimmed.range(of: " : ") ?? trimmed.range(of: ": ") {
            let key = String(trimmed[trimmed.startIndex..<colonRange.lowerBound])
            let colonStr = String(trimmed[colonRange])
            let value = String(trimmed[colonRange.upperBound...])

            result.append(NSAttributedString(string: key, attributes: [.font: font, .foregroundColor: UIColor.systemBlue]))
            result.append(NSAttributedString(string: colonStr, attributes: [.font: font, .foregroundColor: UIColor.label]))
            result.append(colorizeValueNS(value, font: font))
        } else {
            if trimmed.hasPrefix("{") || trimmed.hasPrefix("}") || trimmed.hasPrefix("[") || trimmed.hasPrefix("]") {
                result.append(NSAttributedString(string: trimmed, attributes: [.font: font, .foregroundColor: UIColor.label]))
            } else {
                result.append(colorizeValueNS(trimmed, font: font))
            }
        }
        return result
    }

    private func colorizeValueNS(_ raw: String, font: UIFont) -> NSAttributedString {
        let value = raw.hasSuffix(",") ? String(raw.dropLast()) : raw
        let comma = raw.hasSuffix(",") ? "," : ""

        let color: UIColor
        if value.hasPrefix("\"") {
            color = .systemGreen
        } else if value == "true" || value == "false" {
            color = .systemOrange
        } else if value == "null" {
            color = .secondaryLabel
        } else if Double(value) != nil {
            color = .systemPurple
        } else {
            color = .label
        }

        let result = NSMutableAttributedString(string: value, attributes: [.font: font, .foregroundColor: color])
        if !comma.isEmpty {
            result.append(NSAttributedString(string: comma, attributes: [.font: font, .foregroundColor: UIColor.label]))
        }
        return result
    }
}

// MARK: - 受保护资源面板

/// 「受保护资源」面板的响应展示结构体。
struct ProtectedResourceResponse: Equatable {
    let statusCode: Int
    let contentType: String?
    let body: String
    let byteCount: Int
    let elapsedMs: Int
}

/// 「受保护资源」面板。提供一个可输入任意 URL 的 GET 请求调试入口。
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

    @ViewBuilder
    private var tokenCountdownSection: some View {
        Section {
            TimelineView(.periodic(from: .now, by: 1)) { context in
                let bundle = AppDataStore.get(TokenBundle.self, for: AppDataStore.Keys.tokenBundle) ?? session.phase.tokens
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
    NavigationStack {
        DeveloperToolsView()
            .environmentObject(AppSession())
    }
}
