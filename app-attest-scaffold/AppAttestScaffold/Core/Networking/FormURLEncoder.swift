import Foundation

/// 针对 `application/x-www-form-urlencoded` 请求体的严格百分号编码工具。
///
/// `CharacterSet.urlQueryAllowed` 过于宽松：它不会编码 `+` 和 `=`，导致表单解析器
/// 把 `+` 误读为空格、把 `=` 误读为 key/value 分隔符。因此此处使用按 RFC 3986 §2.3
/// 定义的自定义 unreserved 字符集（`ALPHA / DIGIT / -._~`），其余字符（包括
/// `+`、`=`、`&`、`%` 以及任何 base64 填充字符）一律百分号编码。
enum FormURLEncoder {

    /// RFC 3986 unreserved 字符集。
    private static let unreserved: CharacterSet = {
        var set = CharacterSet.alphanumerics
        set.insert(charactersIn: "-._~")
        return set
    }()

    /// 对单个值进行百分号编码，可放入 form-urlencoded 请求体或 query string。
    static func encode(_ value: String) -> String {
        value.addingPercentEncoding(withAllowedCharacters: unreserved) ?? value
    }

    /// 将一组有序的 `(key, value)` 对编码为 `key1=v1&key2=v2` 形式的请求体。
    /// 保留插入顺序，使请求体在多次运行间保持稳定（便于调试，也便于下游对防重放
    /// nonce 进行可预测的哈希计算）。
    static func encode(_ pairs: [(String, String)]) -> String {
        pairs
            .map { "\(encode($0.0))=\(encode($0.1))" }
            .joined(separator: "&")
    }

    /// 字典形式的便捷封装。Key 会先排序以保证输出稳定。
    static func encode(_ params: [String: String]) -> String {
        encode(params.sorted { $0.key < $1.key }.map { ($0.key, $0.value) })
    }
}
