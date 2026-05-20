import SwiftUI

/// 6 位 OTP 输入控件 — 渲染为 6 个独立的圆角方块。
///
/// 内部用一个隐藏的 TextField 承接键盘事件、剪贴板粘贴与 iOS 短信验证码自动填充
/// (`textContentType = .oneTimeCode`)。上层 `HStack` 用 6 个圆角方块展示每一位数字,
/// 当前光标所在的方块边框高亮, 让用户清楚下一位的落点。
///
/// 这种"隐藏输入 + 可视方块"的写法在 iOS 上比"每位一个真实 TextField"
/// 在系统自动填充与粘贴体验上明显更优, 同时保留了"分体方块"的现代视觉。
///
/// **焦点控制** — 焦点状态由父级统一持有(通过 `@FocusState.Binding` 注入)。
/// 多步骤页面(例如手机号 → 验证码两步)可以让两个 TextField 共存于 `ZStack`,
/// 通过修改父级的焦点枚举无缝切换焦点, 避免键盘"先收起再弹出"的视觉抖动。
struct OTPCodeField: View {

    @Binding var code: String
    var length: Int = 6
    /// 输入达到 `length` 位时回调。父视图据此触发自动校验/登录。
    var onComplete: ((String) -> Void)?
    /// 来自父级的焦点 binding。写入 `true` 会让隐藏输入框获得焦点
    /// (键盘弹出); 写入 `false` 会让出焦点。
    @FocusState.Binding var focused: Bool

    var body: some View {
        ZStack {
            // 真实的输入框, 几乎不可见但仍接收焦点与键盘事件。
            // 不使用 `.opacity(0)`, 否则部分 SwiftUI 版本会跳过命中测试与自动填充。
            TextField("", text: $code)
                .keyboardType(.numberPad)
                .textContentType(.oneTimeCode)
                .focused($focused)
                .opacity(0.001)
                .frame(maxWidth: .infinity)
                .onChange(of: code) { _, newValue in
                    let filtered = newValue.filter(\.isNumber)
                    let clipped = String(filtered.prefix(length))
                    // 完成回调先同步触发, 让父级能立即关键盘并跑校验,
                    // 即使我们随后异步回写 `code` 也不会让用户感觉到延迟。
                    if clipped.count == length {
                        focused = false
                        onComplete?(clipped)
                    }
                    // 回写自身 binding 推迟到下一个 main runloop tick:
                    // 在 `onChange` 内同步覆盖自身 binding, 会与 UITextField
                    // 的输入事务在同一帧内竞争, 在 iOS 17/18 上偶发"卡顿后蹦出
                    // 几个没按过的数字" — 缓冲事件晚到一拍所致。
                    if clipped != newValue {
                        Task { @MainActor in
                            if code == newValue {
                                code = clipped
                            }
                        }
                    }
                }

            HStack(spacing: 10) {
                ForEach(0..<length, id: \.self) { index in
                    cell(at: index)
                }
            }
            // 方块仅作展示, 点击交给外层 `onTapGesture` 统一接管,
            // 避免点单个方块时焦点跳来跳去。
            .allowsHitTesting(false)
        }
        .contentShape(Rectangle())
        .onTapGesture { focused = true }
    }

    /// 渲染单个方块。空槽显示空白, 已填位显示数字, 当前光标位高亮边框。
    private func cell(at index: Int) -> some View {
        let chars = Array(code)
        let digit: String = index < chars.count ? String(chars[index]) : ""
        let isActive = focused && index == chars.count
        let isFilled = index < chars.count

        return Text(digit)
            .font(.system(.title, design: .rounded).weight(.semibold).monospacedDigit())
            .foregroundStyle(.primary)
            .frame(width: 44, height: 54)
            .background(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .fill(Color(.systemGray6))
            )
            .overlay(
                RoundedRectangle(cornerRadius: 10, style: .continuous)
                    .strokeBorder(
                        isActive
                            ? Color.accentColor
                            : (isFilled ? Color.secondary.opacity(0.35) : Color.clear),
                        lineWidth: isActive ? 2 : 1
                    )
            )
            .animation(.easeInOut(duration: 0.12), value: isActive)
            .animation(.easeInOut(duration: 0.12), value: isFilled)
    }
}

/// 让 `OTPCodeField` 在独立预览中可用的小型容器, 持有真正的 `@FocusState`。
private struct OTPCodeFieldPreviewHost: View {
    @State private var code = ""
    @FocusState private var focused: Bool

    var body: some View {
        VStack(spacing: 24) {
            OTPCodeField(code: $code, onComplete: { value in
                print("complete: \(value)")
            }, focused: $focused)
            Text("Code: \(code)")
                .font(.footnote)
                .foregroundStyle(.secondary)
        }
        .padding()
        .onAppear { focused = true }
    }
}

#Preview {
    OTPCodeFieldPreviewHost()
}
