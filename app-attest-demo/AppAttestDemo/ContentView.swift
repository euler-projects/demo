import SwiftUI

struct ContentView: View {
    @State private var logEntries: [LogEntry] = []
    @State private var isLoading = false
    @State private var loadingAction = ""
    @State private var refreshId = UUID()

    var body: some View {
        NavigationView {
            ScrollViewReader { proxy in
                ScrollView {
                    VStack(spacing: 16) {
                        actionsSection
                        tokenStateSection
                        deviceInfoSection
                        logSection
                    }
                    .padding()
                }
                .scrollDismissesKeyboard(.interactively)
                .onTapGesture {
                    UIApplication.shared.sendAction(
                        #selector(UIResponder.resignFirstResponder),
                        to: nil, from: nil, for: nil
                    )
                }
                .onChange(of: logEntries.count) { _ in
                    if let lastId = logEntries.last?.id {
                        withAnimation { proxy.scrollTo(lastId, anchor: .bottom) }
                    }
                }
            }
            .navigationTitle("App Attest Demo")
        }
    }

    // MARK: - Sections

    private var actionsSection: some View {
        GroupBox {
            VStack(spacing: 10) {
                actionButton("Fetch Challenge", icon: "questionmark.circle") {
                    let challenge = try await OAuthTokenManager.shared.fetchChallenge()
                    return "Challenge: \(challenge)"
                }
                actionButton("Attestation", icon: "checkmark.shield", hint: "初次注册") {
                    let challenge = try await OAuthTokenManager.shared.fetchChallenge()
                    let token = try await OAuthTokenManager.shared.attestAndGetToken(challenge: challenge)
                    return formatTokenResult(token)
                }
                actionButton("Assertion Renew", icon: "arrow.triangle.2.circlepath", hint: "续期") {
                    let challenge = try await OAuthTokenManager.shared.fetchChallenge()
                    let token = try await OAuthTokenManager.shared.renewWithAssertion(challenge: challenge)
                    return formatTokenResult(token)
                }
                // ⚠️ Refresh Token仅机密客户端可用, 本 Demo为公共客户端模式, 服务端不会签发refresh_token
                actionButton("Refresh Token", icon: "arrow.clockwise", hint: "仅机密客户端") {
                    let token = try await OAuthTokenManager.shared.refreshAccessToken()
                    return formatTokenResult(token)
                }
                actionButton("Get Valid Token", icon: "bolt.fill", hint: "自动策略") {
                    let accessToken = try await OAuthTokenManager.shared.getValidAccessToken()
                    return "Access Token: \(String(accessToken.prefix(20)))..."
                }
            }
            .padding(.top, 8)
        } label: {
            Label("Actions", systemImage: "play.circle")
        }
        .disabled(isLoading)
    }

    private var tokenStateSection: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 6) {
                if let accessToken = KeychainHelper.shared.read(forKey: "com.app.oauth.accessToken") {
                    infoRow("Access Token", String(accessToken.prefix(20)) + "...")
                    if let expStr = KeychainHelper.shared.read(forKey: "com.app.oauth.tokenExpiration"),
                       let expInterval = Double(expStr) {
                        let expDate = Date(timeIntervalSince1970: expInterval)
                        let remaining = Int(expDate.timeIntervalSinceNow)
                        if remaining > 0 {
                            infoRow("有效期", "剩余 \(remaining)s")
                        } else {
                            infoRow("有效期", "已过期")
                        }
                    }
                    if KeychainHelper.shared.read(forKey: "com.app.oauth.refreshToken") != nil {
                        infoRow("Refresh Token", "✅ 已存储")
                    }
                } else {
                    Text("未登录")
                        .foregroundColor(.secondary)
                }
            }
            .font(.system(.caption, design: .monospaced))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
            .id(refreshId)
        } label: {
            Label("Token State", systemImage: "lock.shield")
        }
    }

    private var deviceInfoSection: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 6) {
                infoRow("App Attest",
                        AppAttestManager.shared.isSupported ? "✅ Supported" : "❌ Not Supported")
                if let keyId = AppAttestManager.shared.getExistingKeyId() {
                    infoRow("Key ID", String(keyId.prefix(16)) + "...")
                } else {
                    infoRow("Key ID", "Not generated")
                }
                if let appId = OAuthTokenManager.appId {
                    infoRow("App ID", appId)
                } else {
                    infoRow("App ID", "⚠️ 检测失败")
                }
            }
            .font(.system(.caption, design: .monospaced))
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 8)
            .id(refreshId)
        } label: {
            Label("Device Info", systemImage: "iphone")
        }
    }

    private var logSection: some View {
        GroupBox {
            VStack(alignment: .leading, spacing: 0) {
                if isLoading {
                    HStack(spacing: 8) {
                        ProgressView()
                        Text(loadingAction + "...")
                            .foregroundColor(.secondary)
                            .font(.system(.caption))
                    }
                    .padding(.vertical, 6)
                }
                if logEntries.isEmpty && !isLoading {
                    Text("点击上方按钮开始操作")
                        .foregroundColor(.secondary)
                        .font(.system(.caption))
                        .padding(.vertical, 8)
                } else {
                    ForEach(logEntries) { entry in
                        logEntryView(entry)
                            .id(entry.id)
                    }
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
            .padding(.top, 4)
        } label: {
            HStack {
                Label("Log", systemImage: "doc.text")
                Spacer()
                if !logEntries.isEmpty {
                    Button("Clear") { logEntries.removeAll() }
                        .font(.caption)
                }
            }
        }
    }

    // MARK: - Sub Views

    private func actionButton(
        _ title: String, icon: String, hint: String? = nil,
        action: @escaping () async throws -> String
    ) -> some View {
        Button {
            performAsync(title, action: action)
        } label: {
            HStack {
                Label(title, systemImage: icon)
                if let hint = hint {
                    Spacer()
                    Text(hint)
                        .font(.caption2)
                        .foregroundColor(.secondary)
                }
            }
            .frame(maxWidth: .infinity, alignment: .leading)
        }
        .buttonStyle(.bordered)
    }

    private func infoRow(_ label: String, _ value: String) -> some View {
        HStack {
            Text(label + ":")
                .foregroundColor(.secondary)
            Text(value)
        }
    }

    private func logEntryView(_ entry: LogEntry) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            HStack(spacing: 6) {
                Text(entry.icon)
                    .font(.system(.caption))
                Text(entry.title)
                    .font(.system(.caption, design: .monospaced))
                    .bold()
                Spacer()
                if let elapsed = entry.elapsed {
                    Text("⏱\(elapsed)")
                        .font(.system(size: 10, design: .monospaced))
                        .foregroundColor(.secondary)
                }
                Text(entry.timestamp)
                    .font(.system(size: 10, design: .monospaced))
                    .foregroundColor(.secondary)
            }
            Text(entry.message)
                .font(.system(.caption2, design: .monospaced))
                .foregroundColor(entry.isError ? .red : .primary)
                .textSelection(.enabled)
            if let rawRequest = entry.rawRequest {
                debugBlock("Request", content: rawRequest)
            }
            if let rawResponse = entry.rawResponse {
                debugBlock("Response", content: rawResponse)
            }
            Divider()
        }
        .padding(.vertical, 2)
    }

    private func debugBlock(_ label: String, content: String) -> some View {
        VStack(alignment: .leading, spacing: 2) {
            Text(label)
                .font(.system(size: 10, weight: .semibold, design: .monospaced))
                .foregroundColor(.secondary)
                .padding(.top, 2)
            Text(content)
                .font(.system(size: 10, design: .monospaced))
                .foregroundColor(.secondary)
                .textSelection(.enabled)
                .frame(maxWidth: .infinity, alignment: .leading)
                .padding(.horizontal, 8)
                .padding(.vertical, 4)
                .background(Color(.systemGray6))
                .cornerRadius(4)
        }
    }

    // MARK: - Actions

    private func performAsync(_ title: String, action: @escaping () async throws -> String) {
        isLoading = true
        loadingAction = title
        let start = Date()
        Task {
            do {
                let result = try await action()
                let elapsed = formatElapsed(since: start)
                let rawRequest = OAuthTokenManager.shared.lastRequestInfo?.summary
                let rawResponse = OAuthTokenManager.shared.lastResponseInfo?.summary
                await MainActor.run {
                    appendLog(title: title, message: result, elapsed: elapsed, rawRequest: rawRequest, rawResponse: rawResponse)
                    isLoading = false
                    refreshId = UUID()
                }
            } catch {
                let elapsed = formatElapsed(since: start)
                let rawRequest = OAuthTokenManager.shared.lastRequestInfo?.summary
                let rawResponse = OAuthTokenManager.shared.lastResponseInfo?.summary
                await MainActor.run {
                    appendLog(title: title, message: error.localizedDescription, isError: true, elapsed: elapsed, rawRequest: rawRequest, rawResponse: rawResponse)
                    isLoading = false
                }
            }
        }
    }

    // MARK: - Helpers

    private func appendLog(title: String, message: String, isError: Bool = false, elapsed: String? = nil, rawRequest: String? = nil, rawResponse: String? = nil) {
        let formatter = DateFormatter()
        formatter.dateFormat = "HH:mm:ss"
        logEntries.append(LogEntry(
            title: title,
            message: message,
            timestamp: formatter.string(from: Date()),
            isError: isError,
            elapsed: elapsed,
            rawRequest: rawRequest,
            rawResponse: rawResponse
        ))
    }

    private func formatElapsed(since start: Date) -> String {
        let ms = Int(Date().timeIntervalSince(start) * 1000)
        return ms >= 1000 ? String(format: "%.2fs", Double(ms) / 1000) : "\(ms)ms"
    }

    private func formatTokenResult(_ token: OAuthTokenResponse) -> String {
        var lines = [
            "Token Type: \(token.tokenType)",
            "Expires In: \(token.expiresIn)s",
            "Access Token: \(String(token.accessToken.prefix(20)))..."
        ]
        if let scope = token.scope {
            lines.append("Scope: \(scope)")
        }
        if token.refreshToken != nil {
            lines.append("Refresh Token: ✅")
        }
        return lines.joined(separator: "\n")
    }
}

// MARK: - Log Entry Model

struct LogEntry: Identifiable {
    let id = UUID()
    let title: String
    let message: String
    let timestamp: String
    var isError: Bool = false
    var elapsed: String? = nil
    var rawRequest: String? = nil
    var rawResponse: String? = nil

    var icon: String {
        isError ? "❌" : "✅"
    }
}

#Preview {
    ContentView()
}
