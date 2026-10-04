# OAuth2 Client Registration (App Attest DYNAMIC)

面向所有 `oauth2Enabled=true` 的 App: 每个 App Attest KEY 独享一个 OAuth2 客户端, 且客户端与用户**解耦**. 注册分两步 —— 先完成 App 实例注册 (attestation, 单次), 再携带 assertion 完成 [RFC 7591][RFC-7591] 动态客户端注册.

> 存量客户端 (历史上预置的 App 级共享客户端) 仍可继续使用 [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) 中的 `app_assertion` 流程 (已废弃); 新 App 实例统一走本文的 RFC 7591 流程.

---

## 一. App 实例注册 `POST /app_attest/register`

DYNAMIC 流程的第一步是 **App 实例注册** (`POST /app_attest/register`): 以 attestation 认证 App 实例、登记其 App Attest KEY (`kid`), 不创建用户、不形成登录态.

> 完整契约 (服务发现、challenge 获取、请求/响应、幂等与重试、错误码) 见 [Apple App Attest 实例注册](App-Attest-Registration.md), 本文不再重复.

本文聚焦第二步: 携该 KEY 的 assertion 完成 [RFC 7591][RFC-7591] 动态客户端注册, 获取专属 `client_id`.

---

## 二. 动态客户端注册 `POST /oauth2/register`

**非匿名.** 面向 App 的凭据是 App Attest assertion, 不携带凭据的请求会被直接拒绝. 客户端认证方式为 `attest_appattest_client_auth`, 凭据承载与校验规则见 [Apple App Attest](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md). App Attest 数据由 **HTTP 头**承载 (与 token 端点一致); 本端点注册体为 JSON, 亦无表单参数可用. 端点实际路径以授权服务元数据的 `client_registration_endpoint` 为准 (默认 `/oauth2/register`).

> 设计对齐 IETF `draft-tschofenig-oauth-attested-dclient-reg` (用 attestation 替代 initial access token 鉴权动态注册).

### 请求头

| 请求头 | 必需 | 说明 |
|--------|------|------|
| `App-Attest-Kid` | 是 | 已通过 `/app_attest/register` 注册的 App Attest KEY 标识 |
| `App-Attest-Challenge` | 是 | 一次性挑战值 |
| `App-Attest-Assertion` | 是 | `generateAssertion()` 产物的 Base64 编码 (证明持有已注册 KEY) |

> 本端点**不接受 attestation** —— attestation 只能提交至 `/app_attest/register`.

### 请求体 (RFC 7591 JSON)

```json
{
  "client_name": "com.example.app",
  "grant_types": ["otp", "refresh_token"],
  "scope": "openid profile",
  "token_endpoint_auth_method": "attest_appattest_client_auth"
}
```

无论请求体如何声明, 服务端对动态注册的客户端强制: `token_endpoint_auth_method=attest_appattest_client_auth` 且无 `client_secret`; 追加 `refresh_token` grant. 已废弃的 `urn:ietf:params:oauth:grant-type:app_assertion` grant 不再被静默移除, 而是在持久化层被拒绝: 请求一旦携带该 grant 即注册失败, 任何入口都无法再新建带此 grant 的客户端 (仅历史存量客户端保留).

### 响应 (201)

```json
{
  "client_id": "<随机 base64url>",
  "client_id_issued_at": "2026-05-10T12:34:56Z",
  "client_name": "com.example.app",
  "grant_types": ["otp", "refresh_token"],
  "scope": "openid profile",
  "token_endpoint_auth_method": "attest_appattest_client_auth"
}
```

`client_id` 已回绑到该 KEY, 后续 assertion 客户端认证据此解析出客户端. 对同一 KEY 重复注册 (已绑定) 幂等返回既有 `client_id`.

### 错误

| HTTP | `error` | 场景 |
|------|---------|------|
| 401 | `invalid_token` | 未携带任何 App Attest 头 |
| 400 | `invalid_client_attestation` | 缺失任一必需头, challenge 无效或已消费, assertion 校验失败 |
| 400 | `unauthorized_client` | 该 KEY 所属 App 未启用 OAuth2 |
| 401 | `invalid_client` | 已绑定的 `client_id` 已不存在 |
| 400 | `invalid_request` | 请求体缺失或不是合法的 RFC 7591 JSON |

错误响应为标准 OAuth2 错误格式, 其中 `error_description` 会指明具体原因 (例如缺失的头名).

---

## 三. 时序

```mermaid
sequenceDiagram
    participant App as iOS App
    participant AS as Authorization Server

    Note over App,AS: 1. App 实例注册 (attestation, 每个 KEY 一次)
    App->>App: generateKey 生成 kid
    App->>AS: POST /app_attest/challenge
    AS-->>App: challenge
    App->>App: attestKey(kid, SHA256(challenge))
    App->>AS: POST /app_attest/register (attestation + challenge)
    AS-->>App: {kid} 仅登记 KEY, 无用户/登录态

    Note over App,AS: 2. 动态注册 per-key 客户端 (assertion)
    App->>AS: POST /oauth2/challenge
    AS-->>App: challenge2
    App->>App: generateAssertion(kid, SHA256(challenge2))
    App->>AS: POST /oauth2/register 头携带 kid+assertion+challenge2, 体为 RFC7591 JSON
    AS->>AS: 校验 assertion, 确认 App 已启用 OAuth2, 铸造 per-key client, 回绑 client_id
    AS-->>App: 201 {client_id, ...}

    Note over App,AS: 3. 之后: grant_type=otp + assertion 取 AT/RT; grant_type=refresh_token + assertion 续期
```

---

## 相关文档

- [Apple App Attest 实例注册](App-Attest-Registration.md) — 前置的 App 实例注册端点 (第一步)
- [OAuth2 Client Authentication - Attestation Based](OAuth2-Client-Authentication-%23-Attestation-Based.md) — 上层客户端认证协议
- [OAuth2 Client Authentication - Attestation Based - Apple App Attest](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) — token 端点的 assertion 用法
- [OAuth2 Token Grant - App Attest](OAuth2-Token-Grant-%23-App-Attest.md) — Token 签发与续期
- [App Attest App](Model-%23-App-Attest-App.md) — 应用模型与 `oauth2Enabled` 语义

[RFC-7591]: https://datatracker.ietf.org/doc/html/rfc7591
