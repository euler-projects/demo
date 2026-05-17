import SwiftUI

/// 可复用的圆形头像组件。
///
/// 当前 userinfo 接口尚未返回头像 URL，因此默认渲染内置的 SF Symbol
/// （`person.crop.circle.fill`）。一旦 `urlString` 可用，视图会自动回退到 `AsyncImage`。
/// 该组件刻意不做缓存 —— AsyncImage 自带的 session 缓存对当前简单 UX 已足够；
/// 下游应用如需磁盘缓存，可自行换成 `Kingfisher` / `SDWebImageSwiftUI`。
struct AvatarView: View {

    /// 可选的远程头像 URL。当为 nil 或非法时，回退为系统符号。
    var urlString: String?
    /// 直径，单位 point。
    var size: CGFloat = 40

    var body: some View {
        Group {
            if let urlString, let url = URL(string: urlString) {
                AsyncImage(url: url) { phase in
                    switch phase {
                    case .success(let image):
                        image.resizable().scaledToFill()
                    case .failure, .empty:
                        placeholder
                    @unknown default:
                        placeholder
                    }
                }
            } else {
                placeholder
            }
        }
        .frame(width: size, height: size)
        .clipShape(Circle())
        .overlay(
            Circle().stroke(Color(.separator), lineWidth: 0.5)
        )
        .accessibilityLabel("用户头像")
    }

    private var placeholder: some View {
        Image(systemName: "person.crop.circle.fill")
            .resizable()
            .scaledToFit()
            .foregroundStyle(.secondary)
            .background(Color(.systemGray6))
    }
}

#Preview {
    HStack(spacing: 16) {
        AvatarView(urlString: nil, size: 40)
        AvatarView(urlString: nil, size: 64)
        AvatarView(urlString: nil, size: 96)
    }
    .padding()
}
