# Attestation Based Client Authentication (Apple App Attest)

当请求头 `OAuth-Client-Attestation-Type: apple_app_attest` 时, 使用 [Apple App Attest](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) 作为客户端证明的 PoP 载体, 替代草案中的标准 PoP JWT.

## 概述

Apple App Attest 利用设备 Secure Enclave 生成不可导出的密钥对, 通过 Apple 签发的硬件绑定证书链证明 App 运行在合法设备上. 本实现将其作为 [Attestation-Based Client Authentication](OAuth2-Client-Authentication-%23-Attestation-Based.md) 的一种 PoP 类型:

- **Attestation** — 首次注册, 提交公钥及 Apple 签发的证书链
- **Assertion** — 后续验证, 使用已注册的私钥签名 challenge

### 使用场景

作为客户端认证时, Apple App Attest 支持独立认证与增强认证两种模式 (详见[父文档](OAuth2-Client-Authentication-%23-Attestation-Based.md#概述)). 此外, 它也可配合 `urn:ietf:params:oauth:grant-type:app_assertion` 作为独立的 Grant Type 直接签发 Token, 完整流程参见 [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md).

---

## Challenge 流程

所有 Apple App Attest 验证均需要一个一次性 challenge, 用于防止重放攻击.

### 获取 Challenge

```http
POST /oauth2/challenge
```

无需认证, 无需请求体.

**Response (200):**

```json
{"challenge": "dGhpcyBpcyBhIHJhbmRvbSBjaGFsbGVuZ2U"}
```

### Challenge 约束

- 一次性消费: 验证成功后立即失效
- 有效期: 5 分钟
- 绑定关系: challenge 在 `attestKey` / `generateAssertion` 调用时作为 `clientDataHash` 的输入

### clientDataHash 计算

Apple API 要求传入 `clientDataHash` 而非原始 challenge. 计算方式:

```swift
let clientDataHash = Data(SHA256.hash(data: challenge.data(using: .utf8)!))
```

请求 `/oauth2/token` 时传递的是**原始 challenge 值** (非 hash), 服务端独立计算 hash 并验证.

---

## 请求参数

所有参数通过 `application/x-www-form-urlencoded` 请求体传递:

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `kid` | string | 是 | `DCAppAttestService.generateKey()` 生成的 Key Identifier |
| `challenge` | string | 是 | 从 `/oauth2/challenge` 获取的原始值 |
| `attestation` | string | 条件 | Base64 编码的 Attestation Object. 首次注册时必需 |
| `assertion` | string | 条件 | Base64 编码的 Assertion Object. 后续验证时必需 |

> 若同时携带 `attestation` 与 `assertion`, 服务端仅处理 `attestation`, `assertion` 被忽略.

---

## 首次注册 (Attestation)

仅在 App 首次运行或重新生成 Key 时执行一次.

### iOS 代码

```swift
import DeviceCheck
import CryptoKit

// 1. Generate key (persist keyId to Keychain)
let keyId = try await DCAppAttestService.shared.generateKey()

// 2. Fetch challenge
let challenge = try await fetchChallenge() // POST /oauth2/challenge

// 3. Attest key
let clientDataHash = Data(SHA256.hash(data: challenge.data(using: .utf8)!))
let attestation = try await DCAppAttestService.shared.attestKey(keyId, clientDataHash: clientDataHash)

// 4. Request token
let body = [
    "grant_type": "<grant_type>",
    "kid": keyId,
    "challenge": challenge,
    "attestation": attestation.base64EncodedString(),
    // ... other grant-specific parameters
]
// POST /oauth2/token with header OAuth-Client-Attestation-Type: apple_app_attest
```

### HTTP 请求

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest

grant_type={grant_type}&kid={keyId}&challenge={challenge}&attestation={base64}&...
```

### 流程

```
iOS App                Apple             Authorization Server
  |                      |                        |
  |  generateKey()       |                        |
  |--------------------->|                        |
  |  keyId               |                        |
  |<---------------------|                        |
  |                                               |
  |  POST /oauth2/challenge                       |
  |---------------------------------------------->|
  |  {"challenge": "..."}                         |
  |<----------------------------------------------|
  |                                               |
  |  attestKey(keyId, SHA256(challenge))          |
  |--------------------->|                        |
  |  Attestation Object  |                        |
  |<---------------------|                        |
  |                                               |
  |  POST /oauth2/token                           |
  |  (kid + challenge + attestation)              |
  |---------------------------------------------->|
  |                      |   Verify attestation   |
  |                      |   Store public key     |
  |                      |   Resolve client_id    |
  |  {access_token, ...}                          |
  |<----------------------------------------------|
```

---

## 后续验证 (Assertion)

首次注册成功后, 每次需要证明客户端身份时执行.

### iOS 代码

```swift
// 1. Fetch challenge
let challenge = try await fetchChallenge() // POST /oauth2/challenge

// 2. Generate assertion
let clientDataHash = Data(SHA256.hash(data: challenge.data(using: .utf8)!))
let assertion = try await DCAppAttestService.shared.generateAssertion(keyId, clientDataHash: clientDataHash)

// 3. Request token
let body = [
    "grant_type": "<grant_type>",
    "kid": keyId,
    "challenge": challenge,
    "assertion": assertion.base64EncodedString(),
    // ... other grant-specific parameters
]
// POST /oauth2/token with header OAuth-Client-Attestation-Type: apple_app_attest
```

### HTTP 请求

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest

grant_type={grant_type}&kid={keyId}&challenge={challenge}&assertion={base64}&...
```

### 流程

```
iOS App                Apple             Authorization Server
  |                                               |
  |  POST /oauth2/challenge                       |
  |---------------------------------------------->|
  |  {"challenge": "..."}                         |
  |<----------------------------------------------|
  |                                               |
  |  generateAssertion(keyId, SHA256(challenge))  |
  |--------------------->|                        |
  |  Assertion Object    |                        |
  |<---------------------|                        |
  |                                               |
  |  POST /oauth2/token                           |
  |  (kid + challenge + assertion)                |
  |---------------------------------------------->|
  |                      |   Verify assertion     |
  |                      |   Check sign counter   |
  |                      |   Resolve client_id    |
  |  {access_token, ...}                          |
  |<----------------------------------------------|
```

---

## 错误码

| 错误码 | HTTP | 场景 |
|--------|------|------|
| `invalid_client_attestation` | 400 | challenge 无效/过期, Attestation/Assertion 验证失败 |
| `invalid_client` | 401 | `kid` 未注册, `client_id` 不匹配, 客户端不存在 |

## 安全须知

- `kid` 必须使用 **Keychain** 存储, 切勿使用 `UserDefaults` 或明文存储
- Attestation 首次注册成功后无需重复提交, 后续仅使用 Assertion
- 重新执行 Attestation 会产生新的设备注册, 原关联用户数据不可复用
- 该 Grant Type 不签发 `refresh_token`: 每次请求均需完整的 challenge + assertion 验证, refresh token 无法提供额外安全价值

## 相关文档

- [OAuth2 Attestation-Based Client Authentication](OAuth2-Client-Authentication-%23-Attestation-Based.md) — 上层协议
- [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) — 独立 Grant Type 完整流程与时序图
- [Establishing Your App's Integrity](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) — Apple 官方文档
- [draft-ietf-oauth-attestation-based-client-auth-08](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08) — IETF 草案
