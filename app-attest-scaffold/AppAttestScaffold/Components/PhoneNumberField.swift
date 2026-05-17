import SwiftUI

/// E.164 风格的手机号输入控件。
///
/// 控件刻意拆成三层关注点, 让视觉布局、数字格式化与有效性判定可以各自独立演进:
///
/// 1. **视觉** — 区号(`+86`)与本地数字共享同一个圆角背景, 视觉上是一个完整字段。
///    区号当前不可编辑(脚手架仅服务中国大陆); 未来引入国家选择器时可在此处替换,
///    其它部分无需改动。
/// 2. **显示格式化** — 本地数字按国家惯例分组(中国: `138 0013 8000`)以便阅读,
///    并使用等宽数字字体, 让数字落在固定网格上。
/// 3. **有效性** — 通过 `isValid` binding 抛出, 让父级 sheet 可以驱动"发送 OTP"按钮,
///    无需重复实现长度 / 前缀规则。绑定的 `e164` 值要么为空, 要么是规范化的 E.164 字符串
///    (`+8613...`) — 已剥离前导零, 不含空格与分隔符。
///
/// **焦点控制** — 焦点状态由父级统一持有(通过 `@FocusState.Binding` 注入)。
/// 多步骤页面(例如手机号 → 验证码两步)可以让两个 TextField 共存于 `ZStack`,
/// 通过修改父级的焦点枚举无缝切换焦点, 避免键盘"先收起再弹出"的视觉抖动。
///
/// **国际化备注** — 当需要支持更多地区时, 把 `E164Phone.Country.china` 常量替换(或扩展
/// `E164Phone.Country`)为由 `PhoneNumberKit` (https://github.com/marmelroy/PhoneNumberKit)
/// 或其他 libphonenumber 移植实现驱动的列表即可。`Country` 结构体是上层调用者唯一依赖的接缝。
struct PhoneNumberField: View {

    /// 规范化的 E.164 号码, 例如 `+8613800138000`。本地数字为空时该值为空字符串。
    @Binding var e164: String
    /// 当本地数字能解析为当前国家的有效号码时为 `true`。
    @Binding var isValid: Bool
    /// 来自父级的焦点 binding。父级用 `@FocusState` 驱动, 写入 `true` 会让本字段获得焦点
    /// (键盘弹出); 写入 `false` 会让出焦点。
    @FocusState.Binding var focused: Bool

    @State private var display: String = ""

    /// 当前国家。脚手架硬编码为 CN; 视图其它位置都不直接引用该常量,
    /// 因此换成选择器只需要改这一处。
    private let country: E164Phone.Country = .china

    var body: some View {
        HStack(spacing: 0) {
            Text("+\(country.dialCode)")
                .font(.system(.title3, design: .rounded).weight(.semibold).monospacedDigit())
                .foregroundStyle(.primary)
                .padding(.leading, 14)
                .padding(.vertical, 12)
                .accessibilityLabel("国家区号")

            Divider()
                .frame(height: 22)
                .padding(.horizontal, 10)

            TextField(country.placeholder, text: $display)
                .font(.system(.title3, design: .rounded).weight(.semibold).monospacedDigit())
                .keyboardType(.numberPad)
                .textContentType(.telephoneNumber)
                .focused($focused)
                .padding(.vertical, 12)
                .padding(.trailing, 12)
                .onChange(of: display) { newValue in
                    applyDisplayChange(newValue)
                }

            if isValid {
                Image(systemName: "checkmark.circle.fill")
                    .foregroundStyle(.green)
                    .padding(.trailing, 12)
                    .transition(.opacity)
            }
        }
        .background(
            RoundedRectangle(cornerRadius: 10, style: .continuous)
                .fill(Color(.systemGray6))
        )
        .animation(.easeInOut(duration: 0.15), value: isValid)
        .onAppear { hydrate() }
    }

    /// 在用户改动可见文本后, 重新推导 `display`、`e164` 与 `isValid`。
    /// 每次按键都会触发, 复杂度为输入长度的 O(n)。
    private func applyDisplayChange(_ newValue: String) {
        // 先剔除所有非数字字符, 再裁剪到当前国家的最大长度,
        // 这样即使粘贴超长字符串也不会短暂出现非法状态。
        let digits = String(newValue.filter(\.isNumber).prefix(country.maxLength))
        let formatted = country.format(digits)
        if formatted != newValue {
            display = formatted
        }
        e164 = digits.isEmpty ? "" : "+\(country.dialCode)\(digits)"
        isValid = country.isValid(digits)
    }

    /// 根据已有的 `e164` 值还原可见文本(例如 OTP sheet 重新打开时)。
    /// 不再触发自动聚焦; 焦点完全由父级根据多步骤流程的当前阶段驱动。
    private func hydrate() {
        let prefix = "+\(country.dialCode)"
        if e164.hasPrefix(prefix) {
            let digits = String(e164.dropFirst(prefix.count))
            display = country.format(digits)
            isValid = country.isValid(digits)
        }
    }
}

/// 单个国家的 E.164 元数据 + 格式化 + 校验逻辑。
///
/// 用闭包形式特意保持最小接口, 这样以后切换到真正的 libphonenumber 实现时,
/// 可以由元数据表或 `PhoneNumberKit.PhoneNumberUtility` 实例填充, 而不必改动
/// `PhoneNumberField` 或其调用方。
enum E164Phone {

    struct Country {
        /// ISO 3166-1 alpha-2 区域码, 例如 `"CN"`。当前仅作信息展示。
        let regionCode: String
        /// 不带前导 `+` 的国家区号, 例如 `"86"`。
        let dialCode: String
        /// 输入控件接受的本地有效数字最大长度。
        let maxLength: Int
        /// 字段为空时显示的占位提示。
        let placeholder: String
        /// 判定一串数字在语法上是否构成有效本地号码的谓词。
        let isValid: (_ digits: String) -> Bool
        /// 把 `digits` 按当前国家的惯例分组成展示形式。必须是输入的纯函数。
        let format: (_ digits: String) -> String

        /// 中国大陆(+86)。手机号为 11 位且以 `1` 开头(自 2010 年起第二位在 `3-9`,
        /// 这里不强校验, 以便对未来新号段保持兼容)。展示分组采用 `3-4-4`,
        /// 例如 `138 0013 8000`。
        static let china = Country(
            regionCode: "CN",
            dialCode: "86",
            maxLength: 11,
            placeholder: "138 0013 8000",
            isValid: { digits in
                digits.count == 11 && digits.first == "1" && digits.allSatisfy(\.isNumber)
            },
            format: { digits in
                let chars = Array(digits)
                switch chars.count {
                case 0:
                    return ""
                case 1...3:
                    return digits
                case 4...7:
                    return "\(String(chars[0..<3])) \(String(chars[3..<chars.count]))"
                default:
                    let head = String(chars[0..<3])
                    let mid = String(chars[3..<7])
                    let tail = String(chars[7..<min(11, chars.count)])
                    return "\(head) \(mid) \(tail)"
                }
            }
        )
    }
}

/// 让 `PhoneNumberField` 在独立预览中可用的小型容器, 持有真正的 `@FocusState`。
private struct PhoneNumberFieldPreviewHost: View {
    @State private var e164 = ""
    @State private var isValid = false
    @FocusState private var focused: Bool

    var body: some View {
        Form {
            PhoneNumberField(e164: $e164, isValid: $isValid, focused: $focused)
            Text("E.164: \(e164)")
                .font(.footnote)
                .foregroundStyle(.secondary)
            Text("isValid: \(String(isValid))")
                .font(.footnote)
                .foregroundStyle(isValid ? .green : .secondary)
        }
    }
}

#Preview {
    PhoneNumberFieldPreviewHost()
}
