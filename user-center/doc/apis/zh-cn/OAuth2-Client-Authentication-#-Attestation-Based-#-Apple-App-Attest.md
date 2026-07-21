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

## 设计决策: 为什么采用非标扩展

### 草案标准流程

[draft-ietf-oauth-attestation-based-client-auth] 的架构假设是: 平台级 attestation 发生在 Client Instance 与 **Client Attester (后端)** 之间, Client Attester 验证平台证明后转译为标准 `oauth-client-attestation+jwt` 签发给客户端. 授权服务器只看到标准 JWT:

```
iOS App            Client Attester          Authorization Server
  |                      |                        |
  |  attestKey (Apple)   |                        |
  |--------------------->|                        |
  |                      |  Verify Apple CBOR     |
  |                      |  Sign standard JWT     |
  |  OAuth-Client-Attestation (JWT)               |
  |<---------------------|                        |
  |                                               |
  |  Sign PoP JWT with separate key               |
  |                                               |
  |  POST /oauth2/token                           |
  |  OAuth-Client-Attestation: <JWT>              |
  |  OAuth-Client-Attestation-PoP: <PoP JWT>      |
  |---------------------------------------------->|
```

### 标准流程的问题

若严格遵循上述架构, 存在以下限制:

1. **必须放弃 Apple Assertion 能力** — 草案要求 PoP JWT 由 `cnf` 中声明的公钥签名. 但 Secure Enclave 密钥只能通过 `generateAssertion()` 生成 Apple 特定格式签名 (CBOR), 不能签任意 JWT payload. 客户端需要**额外生成一个独立密钥对**来签 PoP JWT, 平台硬件绑定的安全优势在 PoP 阶段丢失.

2. **多一次网络请求** — 客户端必须先调用 Client Attester 端点换取 JWT, 再向授权服务器请求 Token. 对于移动网络环境增加延迟.

3. **密钥管理复杂度翻倍** — 设备需管理两个密钥: Secure Enclave 密钥 (用于 attestation) + 独立密钥 (用于签 PoP JWT). 后者不在硬件保护中, 安全性降级.

4. **Attestation 退化为一次性注册** — Apple 的 Assertion 能力 (每次请求由硬件签名) 无法融入标准 PoP JWT 流程, 后续所有验证仅依赖软件密钥.

### 本实现的选择

通过自定义 `OAuth-Client-Attestation-Type: apple_app_attest` 头, 让授权服务器直接理解 Apple 的原生证明格式:

| 对比 | 标准方案 | 本实现 |
|------|---------|--------|
| 额外网络请求 | 是 (Client Attester) | 否 |
| Assertion 持续验证 | 不可用 | 每次请求均由 Secure Enclave 签名 |
| 密钥对数量 | 2 个 | 1 个 (Secure Enclave) |
| 全链路硬件绑定 | 仅注册阶段 | 全生命周期 |
| 标准合规性 | 完全 | 自行扩展 |
| Client Attester 后端 | 需要 | 不需要 |

### 合规性说明

草案 Section 4 明确允许这种变体:

> "This specification is designed to be flexible and can be implemented even in scenarios where the client does not have a backend serving as a Client Attester. In such cases, each Client Instance is responsible for performing the functions typically handled by the Client Attester on its own."

草案同时定义了 IANA "OAuth Client Attestation Proof-of-Possession Methods" 注册表, 并在 Section 5 预留了扩展点:

> "Other specifications or profiles may define additional proof of possession mechanisms for use with the Client Attestation."

本实现的 `apple_app_attest` PoP 类型符合该扩展机制的精神, 尽管尚未注册为正式的 PoP Method.

---

## 相关文档

- [OAuth2 Attestation-Based Client Authentication](OAuth2-Client-Authentication-%23-Attestation-Based.md) — 上层协议
- [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) — 独立 Grant Type 完整流程与时序图
- [Establishing Your App's Integrity](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) — Apple 官方文档
- [draft-ietf-oauth-attestation-based-client-auth-08](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08) — IETF 草案

[draft-ietf-oauth-attestation-based-client-auth]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08
