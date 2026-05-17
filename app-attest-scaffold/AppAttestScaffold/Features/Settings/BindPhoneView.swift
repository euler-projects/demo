import SwiftUI

/// 通用的两步式手机号 OTP sheet, 同时被**登录**(LoginView)与**绑定手机号**(Settings)复用。
///
/// 现代化的两步页面:
/// 1. **第一步 — 手机号** — 输入手机号, 主按钮"发送验证码"。发送成功后切换到第二步。
/// 2. **第二步 — 验证码** — 6 个独立的数字方块。填满 6 位自动触发校验/登录, 不再有"提交"按钮。
///    倒计时结束后顶部出现"重新发送"链接, 左上角箭头返回上一步修改手机号。
///
/// 两条流程的 UI 完全一致, 区别仅在 (1) OTP 由哪个 `AuthService` 方法消费, 以及
/// (2) 申领 ticket 时声明的 `purpose` 不同(脚手架这里都留作 `nil`)。
///
/// **键盘稳定性** — 两个 step 同时存在于 `ZStack` 中, 通过透明度切换可见性,
/// 两个 TextField 始终在视图层级中。发送验证码后, 焦点从手机号字段直接转移到
/// OTP 字段, iOS 会保持键盘不动 — 避免了传统 `Group/switch` 方案中键盘先收起
/// 再弹出的抽动。
struct PhoneOTPSheet: View {

    enum Mode: Equatable {
        /// 登录前的标准登录(`grant_type=otp`)。
        case signIn
        /// 匿名账号 → 实名账号的绑定(`POST /user/identities`)。
        case bindPhone
    }

    /// 两步流程的当前阶段。
    private enum Step {
        case phone
        case code
    }

    let mode: Mode

    @EnvironmentObject private var session: AppSession
    @Environment(\.dismiss) private var dismiss

    @State private var step: Step = .phone
    /// 手机号字段的焦点状态。与 `otpFocused` 在同一 runloop tick 切换时,
    /// iOS 会把键盘平滑过渡到另一个字段。
    @FocusState private var phoneFocused: Bool
    /// 验证码字段的焦点状态。
    @FocusState private var otpFocused: Bool
    @State private var phoneE164: String = ""
    @State private var isPhoneValid: Bool = false
    @State private var otpCode: String = ""
    @State private var ticket: OTPTicket?
    @State private var countdown: Int = 0
    @State private var localError: String?
    @State private var isSending = false
    @State private var isSubmitting = false
    @State private var countdownTimer: Timer?

    var body: some View {
        NavigationStack {
            // 两个 step 都保留在层级中, 只用 opacity 切换可见性 —
            // 这是让键盘在手机号 → 验证码之间不闪烁的关键。
            ZStack {
                phoneStep
                    .opacity(step == .phone ? 1 : 0)
                    .allowsHitTesting(step == .phone)
                codeStep
                    .opacity(step == .code ? 1 : 0)
                    .allowsHitTesting(step == .code)
            }
            .animation(.easeInOut(duration: 0.2), value: step)
            .navigationTitle(navigationTitle)
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .topBarLeading) {
                    if step == .code {
                        Button {
                            // 返回上一步: 先把焦点转回手机号字段(键盘不收起),
                            // 再切换 step 与清理验证码。
                            phoneFocused = true
                            step = .phone
                            otpCode = ""
                            localError = nil
                        } label: {
                            Image(systemName: "chevron.left")
                        }
                        .accessibilityLabel("上一步")
                    } else {
                        Button("取消") { dismiss() }
                    }
                }
            }
            .onAppear {
                // sheet 入场动画期间设置 `phoneFocused` 会被忽略。
                // 这里留一个较短的延迟, 只为“首次出场”这一次需要。
                // 后续 step 切换不再需要任何延迟, 因为两个 TextField 均已在层级中。
                DispatchQueue.main.asyncAfter(deadline: .now() + 0.25) {
                    phoneFocused = true
                }
            }
        }
        // 单一 detent 锁定 sheet 高度 — 避免键盘弹出时 iOS 自动"升档"到更高的 detent
        // 导致按钮上方出现大片留白。两步页面的内容(标题 + 单一输入控件 + 主按钮)
        // 实际占用很少, 锁定在 fraction(0.42) 即可。错误反馈如果超出, 考虑提升文案精简
        // 而不是推高 detent。
        .presentationDetents([.fraction(0.42)])
        .presentationDragIndicator(.visible)
        .onDisappear { countdownTimer?.invalidate() }
    }

    private var navigationTitle: String {
        switch (mode, step) {
        case (.signIn, .phone):    return "手机号登录"
        case (.signIn, .code):     return "输入验证码"
        case (.bindPhone, .phone): return "绑定手机号"
        case (.bindPhone, .code):  return "输入验证码"
        }
    }

    // MARK: - 第一步: 手机号

    private var phoneStep: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text(mode == .signIn ? "请输入手机号" : "请输入要绑定的手机号")
                    .font(.title2.weight(.semibold))
                Text("我们将发送 6 位验证码到该号码。")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }

            PhoneNumberField(
                e164: $phoneE164,
                isValid: $isPhoneValid,
                focused: $phoneFocused
            )

            if let localError {
                Text(localError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }

            Button(action: sendOTP) {
                HStack {
                    Spacer()
                    if isSending {
                        ProgressView()
                    } else {
                        Text("发送验证码").fontWeight(.semibold)
                    }
                    Spacer()
                }
            }
            .buttonStyle(.borderedProminent)
            .controlSize(.large)
            .disabled(!canSendOTP)

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - 第二步: 验证码

    private var codeStep: some View {
        VStack(alignment: .leading, spacing: 24) {
            VStack(alignment: .leading, spacing: 8) {
                Text("输入 6 位验证码")
                    .font(.title2.weight(.semibold))
                Text("已发送至 \(maskedPhone)")
                    .font(.callout)
                    .foregroundStyle(.secondary)
            }

            OTPCodeField(code: $otpCode, onComplete: { _ in
                submit()
            }, focused: $otpFocused)

            HStack(spacing: 4) {
                Text("没收到? ")
                    .font(.footnote)
                    .foregroundStyle(.secondary)
                if countdown > 0 {
                    Text("\(countdown)s 后可重发")
                        .font(.footnote)
                        .foregroundStyle(.tertiary)
                } else if isSending {
                    ProgressView().controlSize(.small)
                } else {
                    Button("重新发送", action: sendOTP)
                        .font(.footnote)
                }
            }

            if isSubmitting {
                HStack(spacing: 8) {
                    ProgressView().controlSize(.small)
                    Text("校验中…")
                        .font(.footnote)
                        .foregroundStyle(.secondary)
                }
            }

            if let localError {
                Text(localError)
                    .font(.footnote)
                    .foregroundStyle(.red)
            }

            Spacer()
        }
        .padding(.horizontal, 20)
        .padding(.top, 24)
        .frame(maxWidth: .infinity, alignment: .leading)
    }

    // MARK: - 派生状态

    private var canSendOTP: Bool {
        !isSending && countdown == 0 && isPhoneValid
    }

    /// 第二步标题区展示用的脱敏号码: `+86 138 **** 0000`。
    /// 仅识别 +86 前缀的 11 位号码; 其它形态原样返回。
    private var maskedPhone: String {
        guard phoneE164.hasPrefix("+86") else { return phoneE164 }
        let digits = phoneE164.dropFirst(3)
        guard digits.count == 11 else { return phoneE164 }
        let head = digits.prefix(3)
        let tail = digits.suffix(4)
        return "+86 \(head) **** \(tail)"
    }

    // MARK: - 动作

    private func sendOTP() {
        localError = nil
        isSending = true
        Task {
            do {
                let issued = try await session.sendPhoneOTP(
                    phone: phoneE164,
                    purpose: nil
                )
                ticket = issued
                startCountdown(seconds: max(issued.retryAfter, 30))
                otpCode = ""
                // 先把焦点转给 OTP 字段(键盘平滑过渡不收起), 再切换 step。
                // 两个 TextField 都在 ZStack 中, 不会这个顺序导致任何闪烁。
                otpFocused = true
                step = .code
            } catch let error as APIError {
                localError = error.errorDescription
            } catch {
                localError = error.localizedDescription
            }
            isSending = false
        }
    }

    private func submit() {
        guard let ticket, otpCode.count == 6, !isSubmitting else { return }
        localError = nil
        isSubmitting = true
        Task {
            do {
                switch mode {
                case .signIn:
                    try await session.signInWithPhoneOTP(
                        recipient: phoneE164,
                        ticket: ticket.otpTicket,
                        otp: otpCode
                    )
                case .bindPhone:
                    try await session.bindPhone(ticket: ticket.otpTicket, otp: otpCode)
                }
                // 成功 — 关闭 sheet。
                dismiss()
            } catch {
                // 失败 — 清空验证码, 行内提示, 焦点回到 OTP 字段让用户重输。
                otpCode = ""
                localError = Self.friendlyError(error)
                otpFocused = true
            }
            isSubmitting = false
        }
    }

    /// 将 API 错误映射为用户友好的行内提示文案。
    private static func friendlyError(_ error: Error) -> String {
        if let apiError = error as? APIError {
            switch apiError {
            case .oauth(let code, _, _) where code == "invalid_grant":
                return "验证码错误, 请重新输入"
            case .factorOccupied:
                return apiError.errorDescription ?? "该手机号已绑定其他账号"
            default:
                return apiError.errorDescription ?? error.localizedDescription
            }
        }
        return error.localizedDescription
    }

    private func startCountdown(seconds: Int) {
        countdown = seconds
        countdownTimer?.invalidate()
        countdownTimer = Timer.scheduledTimer(withTimeInterval: 1, repeats: true) { timer in
            DispatchQueue.main.async {
                if countdown > 0 {
                    countdown -= 1
                } else {
                    timer.invalidate()
                }
            }
        }
    }
}

/// "设置 → 绑定手机号"的便捷入口。具体逻辑都委托给 `PhoneOTPSheet`,
/// 让绑定流程与登录流程保持同步演进。
struct BindPhoneView: View {
    var body: some View {
        PhoneOTPSheet(mode: .bindPhone)
    }
}

#Preview {
    PhoneOTPSheet(mode: .signIn)
        .environmentObject(AppSession())
}
