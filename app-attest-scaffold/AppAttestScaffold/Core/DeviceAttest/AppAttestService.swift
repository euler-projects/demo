import DeviceCheck
import Foundation
import CryptoKit

/// 对 Apple `DCAppAttestService` 的异步封装。
///
/// 每个 App Attest key 都由 Secure Enclave 生成，并与应用身份（`teamId.bundleId`）以及
/// 物理设备本身强绑定，无法导出或迁移。按 Apple 文档：
/// - `generateKey()` 返回一个新的 `keyId`，每个 key 仅可被 attestation **一次**。
/// - `attestKey()` 消耗刚生成的 key，返回 Attestation Object。
/// - `generateAssertion()` 用于已 attest 的 key，承载常规登录态续签。
///
/// 脚手架不在此处持久化 `keyId`；持久化由 `AppDataStore` 在服务端接受 attestation
/// 之后负责。
final class AppAttestService {

    static let shared = AppAttestService()

    private let service: DCAppAttestService

    init(service: DCAppAttestService = .shared) {
        self.service = service
    }

    /// 真机 iOS 设备且支持 App Attest 时返回 `true`。iOS 模拟器以及早于 Apple silicon
    /// attestation 支持的硬件返回 `false`。
    var isSupported: Bool { service.isSupported }

    /// 在 Secure Enclave 中生成一个全新的 App Attest key。
    /// 返回的 `kid` 是不透明字符串；调用方在使用前**必须**先完成 attestation。
    func generateKey() async throws -> String {
        guard isSupported else { throw APIError.appAttestNotSupported }
        return try await service.generateKey()
    }

    /// 为 `kid` 生成一份与 `challenge` SHA-256 摘要绑定的 Attestation Object。
    /// - Returns: 标准 Base64 编码的 attestation blob，可直接提交给 `/oauth2/token`
    ///            （服务端期望标准 Base64，而非 URL-safe Base64）。
    func attest(kid: String, challenge: String) async throws -> String {
        guard isSupported else { throw APIError.appAttestNotSupported }
        let clientDataHash = Data(SHA256.hash(data: Data(challenge.utf8)))
        let attestation = try await service.attestKey(kid, clientDataHash: clientDataHash)
        return attestation.base64EncodedString()
    }

    /// 为 `kid` 生成一份与 `challenge` SHA-256 摘要绑定的 Assertion Object。
    /// 用于 attestation 完成后的常规 token 刷新。
    /// - Returns: 标准 Base64 编码的 assertion blob。
    func assert(kid: String, challenge: String) async throws -> String {
        guard isSupported else { throw APIError.appAttestNotSupported }
        let clientDataHash = Data(SHA256.hash(data: Data(challenge.utf8)))
        let assertion = try await service.generateAssertion(kid, clientDataHash: clientDataHash)
        return assertion.base64EncodedString()
    }
}
