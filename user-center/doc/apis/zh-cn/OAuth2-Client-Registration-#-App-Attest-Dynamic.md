# OAuth2 Client Registration (App Attest DYNAMIC)

面向 `oauth2ClientType=DYNAMIC` 的 App: 每个设备 KEY 独享一个 OAuth2 客户端, 且客户端与用户**解耦**. 注册分两步 —— 先注册设备 KEY (attestation, 单次), 再携带 assertion 完成 [RFC 7591][RFC-7591] 动态客户端注册.

> STATIC App 无需本文流程: 其 `clientId` 由服务端按 `base64url(SHA-256(appId))` 预置, 设备直接用 assertion 走 [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md). 两种模型的差异见 [App Attest App](Model-%23-App-Attest-App.md#oauth2clienttype-枚举值).

---

## 一. 设备 KEY 注册 `POST /app_attest/register`

匿名端点; 校验 Apple App Attest attestation 并登记设备 KEY. **不创建用户, 不形成登录态.** attestation 每个 KEY 正常只提交一次 (重新 `generateKey()` 会产生新的 KEY 与新的 attestation), 注册成功后一律使用 assertion.

> 本端点**幂等**: 若首次响应丢失, 客户端可用新 challenge 重新 `attestKey` 再次提交, 服务端完整校验通过后返回**既有**注册 (不重复登记, 也不报错). 被截获的 attestation 无法重放 —— 其 nonce 绑定已消费的一次性 challenge.

### 请求 (`application/x-www-form-urlencoded`)

| 参数 | 类型 | 必需 | 说明 |
|------|------|------|------|
| `attestation` | string | 是 | `attestKey()` 产物的 Base64 编码 |
| `challenge` | string | 是 | 从 challenge 端点获取的一次性挑战值 (原始值, 非 hash) |

> `kid` **无需上传**: 服务端直接从 attestation 中解析得到, 并在响应中返回. challenge 从授权服务的 `POST /oauth2/challenge` 获取 (与 `token_endpoint` 同源).

```http
POST /app_attest/register
Content-Type: application/x-www-form-urlencoded

attestation={base64}&challenge={challenge}
```

### 响应 (200)

```json
{"kid": "<Key Identifier>"}
```

> 响应中的 `kid` 与客户端 `generateKey()` 返回的 `keyId` **完全一致**. 客户端只需持久化本地 `keyId`, 无需存储或依赖本响应; 该字段仅用于确认与问题排查.

### 错误

| HTTP | `error` | 场景 |
|------|---------|------|
| 400 | `invalid_request` | 缺少 `attestation` / `challenge` |
| 401 | `registration_failed` | challenge 无效或过期, 或 attestation 校验失败 (证书链 / nonce / AAGUID / 计数器非 0 / RP ID 未匹配任何已登记 App 等) |

---

## 二. 动态客户端注册 `POST /oauth2/register`

**非匿名.** 面向 App 的凭据是 App Attest assertion, 不携带凭据的请求会被直接拒绝. 复用 [OAuth 2.0 Attestation-Based Client Authentication](OAuth2-Client-Authentication-%23-Attestation-Based.md) 的 `attest_jwt_client_auth` 消息族. App Attest 数据由 **HTTP 头**承载 (与 token 端点一致); 本端点注册体为 JSON, 亦无表单参数可用. 端点实际路径以授权服务元数据的 `client_registration_endpoint` 为准 (默认 `/oauth2/register`).

> 设计对齐 IETF `draft-tschofenig-oauth-attested-dclient-reg` (用 attestation 替代 initial access token 鉴权动态注册).

### 请求头

| 请求头 | 必需 | 说明 |
|--------|------|------|
| `OAuth-Client-Attestation-Type` | 是 | 固定 `apple_app_attest` |
| `OAuth-Client-Attestation-Kid` | 是 | 已通过 `/app_attest/register` 注册的设备 KEY 标识 |
| `OAuth-Client-Attestation-Challenge` | 是 | 一次性挑战值 |
| `OAuth-Client-Attestation-Assertion` | 是 | `generateAssertion()` 产物的 Base64 编码 (证明持有已注册 KEY) |

> 本端点**不接受 attestation** —— DYNAMIC 类型的 attestation 只能提交至 `/app_attest/register`.

### 请求体 (RFC 7591 JSON)

```json
{
  "client_name": "com.example.app",
  "grant_types": ["otp", "refresh_token"],
  "scope": "openid profile",
  "token_endpoint_auth_method": "attest_jwt_client_auth"
}
```

无论请求体如何声明, 服务端对 DYNAMIC 客户端强制: `token_endpoint_auth_method=attest_jwt_client_auth` 且无 `client_secret`; 移除 `urn:ietf:params:oauth:grant-type:app_assertion` grant; 追加 `refresh_token` grant.

### 响应 (201)

```json
{
  "client_id": "<随机 base64url>",
  "client_id_issued_at": "2026-05-10T12:34:56Z",
  "client_name": "com.example.app",
  "grant_types": ["otp", "refresh_token"],
  "scope": "openid profile",
  "token_endpoint_auth_method": "attest_jwt_client_auth"
}
```

`client_id` 已回绑到该 KEY, 后续 assertion 客户端认证据此解析出客户端. 对同一 KEY 重复注册 (已绑定) 幂等返回既有 `client_id`.

### 错误

| HTTP | `error` | 场景 |
|------|---------|------|
| 401 | `invalid_token` | 未携带任何 App Attest 头 |
| 400 | `invalid_client_attestation` | 缺失任一必需头, `OAuth-Client-Attestation-Type` 非 `apple_app_attest`, challenge 无效或已消费, assertion 校验失败 |
| 400 | `unauthorized_client` | 该 KEY 所属 App 未启用 OAuth2 或非 DYNAMIC 类型 |
| 401 | `invalid_client` | 已绑定的 `client_id` 已不存在 |
| 400 | `invalid_request` | 请求体缺失或不是合法的 RFC 7591 JSON |

错误响应为标准 OAuth2 错误格式, 其中 `error_description` 会指明具体原因 (例如缺失的头名).

---

## 三. 时序

```mermaid
sequenceDiagram
    participant App as iOS App
    participant AS as Authorization Server

    Note over App,AS: 1. 注册设备 KEY (attestation, 每个 KEY 一次)
    App->>App: generateKey 生成 kid
    App->>AS: POST /oauth2/challenge
    AS-->>App: challenge
    App->>App: attestKey(kid, SHA256(challenge))
    App->>AS: POST /app_attest/register (attestation + challenge)
    AS-->>App: {kid} 仅登记 KEY, 无用户/登录态

    Note over App,AS: 2. 动态注册 per-key 客户端 (assertion)
    App->>AS: POST /oauth2/challenge
    AS-->>App: challenge2
    App->>App: generateAssertion(kid, SHA256(challenge2))
    App->>AS: POST /oauth2/register 头携带 kid+assertion+challenge2, 体为 RFC7591 JSON
    AS->>AS: 校验 assertion, 确认 App 为 DYNAMIC, 铸造 per-key client, 回绑 client_id
    AS-->>App: 201 {client_id, ...}

    Note over App,AS: 3. 之后: grant_type=otp + assertion 取 AT/RT; grant_type=refresh_token + assertion 续期
```

---

## 相关文档

- [OAuth2 Client Authentication - Attestation Based](OAuth2-Client-Authentication-%23-Attestation-Based.md) — 上层客户端认证协议
- [OAuth2 Client Authentication - Attestation Based - Apple App Attest](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) — token 端点的 assertion 用法
- [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) — Token 签发与续期
- [App Attest App](Model-%23-App-Attest-App.md) — `oauth2ClientType` 语义

[RFC-7591]: https://datatracker.ietf.org/doc/html/rfc7591
