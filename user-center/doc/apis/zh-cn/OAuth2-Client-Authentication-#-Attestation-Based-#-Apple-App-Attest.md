# Attestation Based Client Authentication (Apple App Attest)

当请求头 `OAuth-Client-Attestation-Type: apple_app_attest` 时, 使用 [Apple App Attest](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) 作为客户端证明的 PoP 载体, 替代草案中的标准 PoP JWT.

## 使用场景

作为客户端认证时, Apple App Attest 支持独立认证与增强认证两种模式 (详见[父文档](OAuth2-Client-Authentication-%23-Attestation-Based.md#概述)). 此外, 它也可配合 `urn:ietf:params:oauth:grant-type:app_assertion` 作为独立的 Grant Type 直接签发 Token, 完整流程参见 [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md).

---

## 速览

客户端仅需使用 Apple 的三个 API:

| API | 调用时机 | 产物 | 提交至 |
|-----|---------|------|--------|
| `generateKey()` | App 首次运行, 或需要更换密钥时 | `keyId` (即后续请求所需的 `kid`) | **不提交**, 存入 Keychain |
| `attestKey(_:clientDataHash:)` | App 实例注册时, 每个 KEY 一次 | `attestation` | `POST /app_attest/register` |
| `generateAssertion(_:clientDataHash:)` | 此后每次需要证明持有该 KEY 时 | `assertion` | `POST /oauth2/register`、`POST /oauth2/token` |

> `generateKey()` 返回的 `keyId` 与服务端在 App 实例注册后所持有的 `kid` 是同一个值. 客户端只需持久化本地 `keyId`, 无需依赖注册接口的响应内容.

**App Attest 数据在 OAuth2 端点一律通过请求头承载** (`OAuth-Client-Attestation-*`), 请求体只放 `grant_type` 与该 grant 自身的参数. App 实例注册端点 `POST /app_attest/register` 是唯一例外, 它使用表单参数, 详见步骤 2.

---

## 前置条件: 确认客户端类型

客户端类型由服务端配置, 接入前需与服务端确认. 该配置决定客户端需要执行的步骤:

| 类型 | 客户端需执行的步骤 |
|------|------------------|
| **STATIC** | App 实例注册 → 申请 Token. 无需获知 `client_id`, 服务端可依据 App 身份自行解析 |
| **DYNAMIC** | App 实例注册 → 注册客户端 (获取该 KEY 专属的 `client_id`) → 申请 Token |

> **App 实例注册一律先走 `POST /app_attest/register`**, OAuth2 端点只接受 `assertion`. 在 token 端点提交 `attestation` 的用法已废弃, 详见下文〈已废弃的用法〉.

---

## 推荐流程

### 步骤 1: 获取 Challenge

生成 `attestation` / `assertion` 前需先获取一次性 challenge: 一次性、约 5 分钟有效, 提交给 Apple 的是其 SHA-256 hash、提交给服务端的是原始字符串. **两个业务域各有自己的 challenge 端点, 须用对**:

- **App 实例注册**(生成 `attestation`): `POST /app_attest/challenge` —— 见 [Apple App Attest 服务发现](App-Attest-Discovery.md).
- **OAuth 客户端认证**(生成 `assertion`, 用于 `/oauth2/register` 与 `/oauth2/token`): `POST /oauth2/challenge` —— 即 OAuth AS 元数据的标准 `challenge_endpoint`.

> 二者实现同源, 但 path 与归属域不同. App 实例注册端点契约见 [Apple App Attest 实例注册](App-Attest-Registration.md). token 流程中**每次请求都需重新获取 challenge 并重新生成 assertion**, 不可复用; challenge 过期或已使用会返回 `invalid_client_attestation`, 重新获取后重试即可.

### 步骤 2: App 实例注册 (两种类型均需执行)

首次接入需完成 **App 实例注册**: `generateKey()` → 获取 challenge → `attestKey()` → `POST /app_attest/register` (表单参数), 登记该 App 实例的 App Attest KEY (`kid`). attestation 每个 KEY 正常只提交一次, 此后一律用 assertion.

> 完整流程与契约 (请求/响应、幂等与重试、错误码) 见 [Apple App Attest 实例注册](App-Attest-Registration.md).

### 步骤 3: 注册客户端 (仅 DYNAMIC)

DYNAMIC 类型需携 assertion 请求 `POST /oauth2/register` (RFC 7591), 获取该 KEY 专属的 `client_id` 并持久化; **STATIC 类型跳过本步骤.** assertion 由 `generateAssertion(keyId, SHA256(challenge))` 生成, App Attest 数据经请求头 (`OAuth-Client-Attestation-*`) 承载, 注册体为 RFC 7591 JSON.

> 完整契约 (请求头、请求体、服务端强制项、响应、错误码) 见 [OAuth2 Client Registration - App Attest DYNAMIC](OAuth2-Client-Registration-%23-App-Attest-Dynamic.md).

### 步骤 4: 申请 Token

再次获取一个新的 challenge(步骤 1 的 `/oauth2/challenge`)并生成 assertion, 提交至 Token 端点. 请求头见下方示例与〈`/oauth2/token` 请求速查〉.

```swift
let clientDataHash = Data(SHA256.hash(data: challenge.data(using: .utf8)!))
let assertion = try await DCAppAttestService.shared.generateAssertion(keyId, clientDataHash: clientDataHash)

let body: [String: String] = [
    "grant_type": "<grant_type>",
    // ... 其余参数按对应 grant type 的文档补充
]
// App Attest 数据全部走请求头, 不进请求体
```

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {keyId}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {base64}

grant_type={grant_type}&...
```

**Response (200):**

```json
{"access_token": "...", "token_type": "Bearer", "expires_in": 3599}
```

流程:

```
iOS App                Apple             Authorization Server
  |                                               |
  |  POST /oauth2/challenge                       |
  |---------------------------------------------->|
  |  {"attestation_challenge": "..."}             |
  |<----------------------------------------------|
  |                                               |
  |  generateAssertion(keyId, SHA256(challenge))  |
  |--------------------->|                        |
  |  Assertion Object    |                        |
  |<---------------------|                        |
  |                                               |
  |  POST /oauth2/token                           |
  |  headers: -Kid, -Challenge, -Assertion        |
  |---------------------------------------------->|
  |                      |   Verify assertion     |
  |  {access_token, ...}                          |
  |<----------------------------------------------|
```

Token 过期后重复本步骤即可. 每次请求均需使用新的 challenge 与新的 assertion, assertion 不可复用.

> **关于 `grant_type`**: 应选择用户级 Grant Type (如 OTP、授权码) 或 `refresh_token`. assertion 只证明客户端身份, 不证明用户身份, 因此必须叠加能确定用户的因素.

---

## `/oauth2/token` 请求速查

**请求头:**

| 头 | 必需 | 说明 |
|----|------|------|
| `OAuth-Client-Attestation-Type` | 是 | 固定 `apple_app_attest` |
| `OAuth-Client-Attestation-Kid` | 是 | `generateKey()` 返回的 keyId |
| `OAuth-Client-Attestation-Challenge` | 是 | challenge 原始字符串 (**非** hash) |
| `OAuth-Client-Attestation-Assertion` | 是 | Base64 编码的 Assertion Object |
| `Content-Type` | 是 | `application/x-www-form-urlencoded` |

**请求体:** 仅 `grant_type` 与该 grant 自身要求的参数 (按对应 grant type 文档). `client_id` 可选: STATIC 无需提交, DYNAMIC 若提交则必须与步骤 3 所获取的值一致.

---

## 已废弃的用法

以下用法仅为已发布的旧客户端保留, **不要用于新接入**:

| 已废弃用法 | 说明与替代 |
|-----------|-----------|
| 表单参数承载 | 将 `kid` / `challenge` / `assertion` 作为请求体表单参数提交. 改用请求头 |
| 在 token 端点提交 `attestation` | 跳过 App 实例注册, 一次请求完成注册 + 认证 + 签发. 仅 STATIC 可用; DYNAMIC 返回 `unauthorized_client` (但 App Attest KEY 已登记成功, **无需重新 `attestKey()`**, 直接继续步骤 3). 改用步骤 2 的注册端点 |
| `attestation` 与 `assertion` 同传 | 冗余: App 实例注册本身已完成客户端认证, 附加 assertion 不提升安全强度 |
| `urn:ietf:params:oauth:grant-type:app_assertion` | 仅凭 assertion 续期, 依赖此前 attestation 请求建立的设备与用户关联; 关联缺失返回 `invalid_grant`, 且该 grant 会忽略请求携带的任何登录因素. 改用用户级 Grant Type 或 `refresh_token`, 详见 [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) |

> 表单参数承载与请求头承载**不可混用**: 只要出现 `OAuth-Client-Attestation-Assertion` 头, 服务端即整体按请求头读取, 忽略全部 App Attest 表单参数.

---

## 错误码与处理

| 错误码 | HTTP | 含义 | 客户端处理 |
|--------|------|------|-----------|
| `invalid_request` | 400 | 请求头缺失 (`-Kid` / `-Challenge` / `-Assertion` 任一) | 检查请求头 |
| `invalid_client_attestation` | 400 | challenge 过期或已被使用, 或 assertion 未通过验证 | 重新获取 challenge 并重新生成 assertion 后重试 |
| `unauthorized_client` | 400 | 当前 App 类型不允许该用法: DYNAMIC 尚未完成客户端注册, 或 App 未开通 OAuth2 | DYNAMIC 需先完成步骤 3 |
| `invalid_client` | 401 | `kid` 未注册, 或客户端不存在 | 重新执行步骤 2; 若仍失败请联系服务端 |
| `invalid_grant` | 400 | 使用已废弃的 `app_assertion` Grant Type, 但该 `kid` 没有对应的用户关联 | 改用用户级 Grant Type 或 `refresh_token` |

---

## 注意事项

- **`keyId` 必须存入 Keychain**, 不得使用 `UserDefaults` 或明文存储. 丢失后需重新执行 `generateKey()` 并重新注册
- **attestation 每个 KEY 正常仅提交一次**, 此后统一使用 assertion
- **assertion 不证明用户身份**: 申请 Token 必须叠加用户级 Grant Type 或 `refresh_token`
- **App 实例注册具备幂等性**: 若注册响应丢失 (如网络中断), 重新获取 challenge、重新执行 `attestKey()` 并再次提交即可, 服务端会返回同一份注册结果, 不会重复登记
- **被截获的 attestation 无法重放**: 其 nonce 绑定的是已消费的那一次 challenge
- **重新执行 `generateKey()` 等同于更换设备**: 会产生新的 `keyId` 与新的 attestation, 必须重新执行注册流程; 原 `keyId` 关联的服务端数据不会自动迁移
- **assertion 由 Secure Enclave 签名, 毫秒级完成**: 日常获取与续期 Token 应优先使用 assertion, 避免重复执行 attestation

---

## 附: 与 IETF 草案的差异

[draft-ietf-oauth-attestation-based-client-auth] 的标准做法需要一个 Client Attester 后端, 将 Apple 的证明转译为标准 JWT, 并要求客户端**额外**维护一对软件密钥用于签署 PoP JWT. 本实现通过 `OAuth-Client-Attestation-Type: apple_app_attest` 使服务端直接接受 Apple 原生格式. 对客户端而言意味着:

- 仅需 Secure Enclave 中的**一对**密钥, 无需额外生成密钥对
- 无需先访问中间端点换取 JWT, 减少一次网络往返
- 每次请求均由硬件签名 (assertion), 硬件绑定贯穿整个生命周期, 而非仅限于注册阶段

草案 Section 4 明确允许不存在 Client Attester 后端的变体, Section 5 亦为额外的 PoP 机制预留了扩展点, 因此本实现符合草案框架. 动态注册端点以 assertion 替代 initial access token 的做法, 对齐 IETF `draft-tschofenig-oauth-attested-dclient-reg`.

---

## 相关文档

- [Apple App Attest 服务发现](App-Attest-Discovery.md) — 端点发现
- [Apple App Attest 实例注册](App-Attest-Registration.md) — App 实例注册端点 (步骤 1-2 完整契约)
- [OAuth2 Attestation-Based Client Authentication](OAuth2-Client-Authentication-%23-Attestation-Based.md) — 上层协议
- [OAuth2 Client Registration - App Attest DYNAMIC](OAuth2-Client-Registration-%23-App-Attest-Dynamic.md) — OAuth2 客户端动态注册契约
- [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) — 已废弃 Grant Type 的兼容说明
- [Establishing Your App's Integrity](https://developer.apple.com/documentation/devicecheck/establishing-your-app-s-integrity) — Apple 官方文档
- [draft-ietf-oauth-attestation-based-client-auth-11](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-11) — IETF 草案

[draft-ietf-oauth-attestation-based-client-auth]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-11
