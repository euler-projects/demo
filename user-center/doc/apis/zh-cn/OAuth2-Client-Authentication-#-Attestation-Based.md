# OAuth2 Attestation-Based Client Authentication

基于 [draft-ietf-oauth-attestation-based-client-auth-08] 实现, 认证方式标识为 `attest_jwt_client_auth`.

## 概述

Attestation-Based Client Authentication 允许客户端通过设备证明 (Client Attestation) 和持有证明 (Proof-of-Possession) 向授权服务器证明自身身份. 本实现支持两种使用模式:

1. **独立认证** — `attest_jwt_client_auth` 作为唯一的客户端认证方式, 适用于原生 App 等无法安全存储 client_secret 的场景.
2. **增强认证** — 在标准认证 (如 `client_secret_basic`, PKCE) 基础上叠加设备证明, 为已认证客户端提供额外的安全信号.

## 请求头

| 请求头 | 必需 | 说明 |
|--------|------|------|
| `OAuth-Client-Attestation` | 条件 | Client Attestation JWT (草案 §5.1). JWT 类型下, 若公钥未变更且 PoP JWT 头部携带已注册的 `kid`, 可省略 |
| `OAuth-Client-Attestation-PoP` | 是 (JWT 类型) | PoP JWT (草案 §5.2), 用于证明客户端实例持有与 Attestation 对应的私钥 |
| `OAuth-Client-Attestation-Type` | 否 | **本实现扩展**. 指定 PoP 载体类型, 缺省为 `jwt`. 可选值见下文 |

### `OAuth-Client-Attestation-Type` 扩展

草案仅定义了基于 JWT 的 PoP 机制. 本实现通过自定义头 `OAuth-Client-Attestation-Type` 扩展了 PoP 载体类型, 以支持平台原生证明方案:

| 值 | 说明 |
|----|------|
| `jwt` | 标准 PoP JWT (草案 §5.2). 默认值 |
| `apple_app_attest` | 使用 Apple App Attest 作为 PoP 载体. 参数通过请求体传递, 详见 [Apple App Attest 子文档](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) |

## JWT 类型 (`jwt`)

### PoP JWT 结构

**Header:**

| 参数 | 必需 | 说明 |
|------|------|------|
| `typ` | 是 | 必须为 `oauth-client-attestation-pop+jwt` |
| `alg` | 是 | 签名算法 (如 `ES256`) |
| `kid` | 条件 | 密钥标识. 省略 `OAuth-Client-Attestation` 头时必需, 用于服务端查找已注册的公钥 |

**Claims:**

| 声明 | 必需 | 说明 |
|------|------|------|
| `aud` | 是 | 授权服务器的 Issuer URL |
| `iat` | 是 | 签发时间. 有效窗口 5 分钟, 允许 30 秒时钟偏移 |
| `jti` | 是 | 唯一标识, 用于防重放 |
| `challenge` | 是 | 从 Challenge 端点获取的一次性挑战值, 使用后失效 |

### 请求示例 (独立认证)

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-PoP: eyJhbGciOiJFUzI1NiIsInR5cCI6Im9hdXRoLWNsaWVudC1hdHRlc3RhdGlvbi1wb3Arand0Iiwia2lkIjoiLi4uIn0.eyJhdWQiOiJodHRwczovL2FzLmV4YW1wbGUuY29tIiwiaWF0IjoxNzIxMDAwMDAwLCJqdGkiOiJ1bmlxdWUtaWQiLCJjaGFsbGVuZ2UiOiJhYmMxMjMifQ.signature

grant_type=urn:ietf:params:oauth:grant-type:app_assertion&scope=openid
```

> 此示例省略了 `OAuth-Client-Attestation` 头, 依赖 PoP JWT `kid` 查找已注册公钥.

### 请求示例 (增强认证)

```http
POST /oauth2/token
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-PoP: <PoP JWT>

grant_type=refresh_token&refresh_token=...
```

> 标准认证 (`client_secret_basic`) 完成后, 服务端额外验证 PoP 数据. 验证失败将拒绝请求.

## Apple App Attest 类型 (`apple_app_attest`)

当 `OAuth-Client-Attestation-Type: apple_app_attest` 时, 使用 Apple App Attest 框架作为 PoP 载体. 所有参数通过请求体传递:

| 参数 | 必需 | 说明 |
|------|------|------|
| `kid` | 是 | App Attest 密钥标识 |
| `challenge` | 是 | 一次性挑战值 |
| `attestation` | 条件 | 首次注册时提交的 Attestation 数据 (Base64). 提交后不再需要 |
| `assertion` | 条件 | 后续请求的 Assertion 数据 (Base64). 与 `attestation` 二选一 |

### 请求示例

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest

grant_type=urn:ietf:params:oauth:grant-type:app_assertion&kid=<key-id>&challenge=<challenge>&assertion=<base64-assertion>&scope=openid
```

> 详细流程参考: [Apple App Attest 子文档](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md)

## 认证流程

### 独立认证

```
Client                                    Authorization Server
  |                                                |
  |  POST /oauth2/token                            |
  |  Headers: OAuth-Client-Attestation-Type,       |
  |           OAuth-Client-Attestation(-PoP)       |
  |  Body: grant_type, scope, ...                  |
  | ---------------------------------------------> |
  |                                                |
  |  1. Converter extracts attestation data        |
  |  2. Provider verifies PoP, resolves client_id  |
  |  3. Lookup RegisteredClient, check auth method |
  |  4. Validate client_id consistency if present  |
  |  5. Issue Access Token                         |
  |                                                |
  | <--------------------------------------------- |
  |  200 { access_token, token_type, ... }         |
```

### 增强认证

```
Client                                    Authorization Server
  |                                                |
  |  POST /oauth2/token                            |
  |  Authorization: Basic / client_secret_post     |
  |  Headers: OAuth-Client-Attestation-Type,       |
  |           OAuth-Client-Attestation(-PoP)       |
  |  Body: grant_type, ...                         |
  | ---------------------------------------------> |
  |                                                |
  |  1. Standard client authentication completes   |
  |  2. AttestationFilter detects attestation hdrs |
  |  3. Reuse Converter + Provider to verify PoP   |
  |  4. Verify PoP client_id matches standard auth |
  |  5. Attach attestation context, continue       |
  |                                                |
  | <--------------------------------------------- |
  |  200 { access_token, token_type, ... }         |
```

## `client_id` 一致性校验

依据草案 §6.3: 若请求体显式携带 `client_id` 参数, 授权服务器 **必须** 校验其与 Client Attestation 中解析出的 `client_id` 一致. 增强认证模式下同样会校验 PoP 解析出的 `client_id` 与标准认证已确认的 `client_id` 一致.

## 错误码

| 错误码 | HTTP 状态码 | 场景 |
|--------|-------------|------|
| `invalid_client_attestation` | 400 | PoP 验证失败 (签名、时间窗口、challenge、jti 重放等) |
| `invalid_client` | 401 | client_id 不匹配或客户端不存在 |
| `unauthorized_client` | 400 | 客户端未配置 `attest_jwt_client_auth` 认证方式 |

## 安全考量

* PoP JWT 有效期限制为 5 分钟, `jti` 防重放, `challenge` 一次性消费 — 三层防护防止重放攻击.
* 增强认证模式下, 标准认证与设备证明 **双重校验**, 任一失败即拒绝.
* Apple App Attest 方案利用 Secure Enclave 硬件绑定私钥, 无法导出.

## 参考

* [draft-ietf-oauth-attestation-based-client-auth-08] — OAuth 2.0 Attestation-Based Client Authentication
* [RFC6749] — The OAuth 2.0 Authorization Framework

[draft-ietf-oauth-attestation-based-client-auth-08]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08
[RFC6749]: https://datatracker.ietf.org/doc/html/rfc6749
