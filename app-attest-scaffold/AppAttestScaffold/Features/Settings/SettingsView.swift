import SwiftUI

/// 账号与安全 sheet — 由 `HomeView` 右上角头像按钮以 sheet 形式呈现。
///
/// 定位: 围绕「这个人是谁、用什么登录身份登录、如何退出」三件事, 不掺杂运行态
/// 信息(那部分在首页的服务配置 / 诊断分组里)。
/// - 账号: 只读资料(用户名 / 昵称兜底)。
/// - 登录与安全: 手机号绑定 / 解绑。Email、WeChat 按脚手架规约不在范围内。
/// - 会话: 退出登录(具有破坏性)。
struct SettingsView: View {

    @EnvironmentObject private var session: AppSession
    @Environment(\.dismiss) private var dismiss

    @State private var showBindSheet = false
    @State private var showUnbindConfirm = false
    @State private var showSignOutConfirm = false
    @State private var showWipeConfirm = false
    @State private var pendingIdentityId: String?

    var body: some View {
        NavigationStack {
            Form {
                profileSection
                securitySection
                signOutSection
                wipeAllSection
            }
            .navigationTitle("账号与安全")
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button("完成") { dismiss() }
                }
            }
            .sheet(isPresented: $showBindSheet) {
                PhoneOTPSheet(mode: .bindPhone)
                    .environmentObject(session)
            }
            .confirmationDialog(
                "确认解除绑定?",
                isPresented: $showUnbindConfirm,
                titleVisibility: .visible
            ) {
                Button("解除绑定", role: .destructive) {
                    if let id = pendingIdentityId {
                        Task { await session.unbind(identityId: id) }
                    }
                }
                Button("取消", role: .cancel) {}
            } message: {
                Text("解除绑定后, 该手机号将无法用于登录恢复。")
            }
            .confirmationDialog(
                "确认退出登录?",
                isPresented: $showSignOutConfirm,
                titleVisibility: .visible
            ) {
                Button("退出登录", role: .destructive) {
                    Task { await session.signOut() }
                }
                Button("取消", role: .cancel) {}
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
                Text("将清除所有本地数据并恢复到刚安装的状态，再次打开应用可重新匿名试用。")
            }
        }
    }

    // MARK: - 各分组 Section

    @ViewBuilder
    private var profileSection: some View {
        if let account = session.phase.account {
            Section {
                HStack(spacing: 16) {
                    AvatarView(urlString: account.profile.avatarUrl, size: 72)
                    VStack(alignment: .leading, spacing: 4) {
                        Text(account.profile.displayName)
                            .font(.title3)
                            .fontWeight(.semibold)
                        Text(session.phase.isAnonymous ? "匿名用户" : "已绑定账号")
                            .font(.caption)
                            .foregroundStyle(.secondary)
                    }
                    Spacer(minLength: 0)
                }
                .padding(.vertical, 4)
            }

            Section("账号") {
                LabeledContent("用户名", value: account.profile.username)
                LabeledContent(
                    "昵称",
                    value: account.profile.nickname ?? account.profile.username
                )
            }
        }
    }

    @ViewBuilder
    private var securitySection: some View {
        Section {
            if let phone = session.phase.account?.phoneIdentity {
                LabeledContent("手机号", value: phone.phone ?? phone.identifier)
                Button(role: .destructive) {
                    pendingIdentityId = phone.identityId
                    showUnbindConfirm = true
                } label: {
                    Text("解除绑定")
                }
            } else {
                Button {
                    showBindSheet = true
                } label: {
                    HStack {
                        Text("绑定手机号")
                        Spacer()
                        Image(systemName: "chevron.right")
                            .font(.caption)
                            .foregroundStyle(.tertiary)
                    }
                }
                .foregroundStyle(.primary)
            }
        } header: {
            Text("登录与安全")
        } footer: {
            if session.phase.account?.phoneIdentity != nil {
                Text("当前实现暂不支持直接换号。如需更换, 请先解除再重新绑定。")
            } else {
                Text("仅手机绑定可用; email、wechat 在脚手架范围之外。")
            }
        }
    }

    private var signOutSection: some View {
        Section {
            Button(role: .destructive) {
                showSignOutConfirm = true
            } label: {
                HStack {
                    Spacer()
                    Text("退出登录")
                    Spacer()
                }
            }
        } footer: {
            Text("清除访问令牌、设备密钥与本地账号缓存。登出后不可再次匿名试用，仅允许手机号登录。")
        }
    }

    private var wipeAllSection: some View {
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
            Text("清除所有本地数据并恢复到刚安装状态，下次打开应用将可重新匿名试用。")
        }
    }
}

#Preview {
    SettingsView()
        .environmentObject(AppSession())
}
