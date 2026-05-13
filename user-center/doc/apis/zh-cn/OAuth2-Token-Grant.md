# OAuth2 Token Grant

本文档描述 `/oauth2/token` 的通用约定、支持的 Grant Type、客户端认证方式、Scope 与各类 Token 的语义. 具体 grant type 的请求/响应细节请见对应子文档.

> 本 OAuth2 认证服务基于 [RFC 6749 (OAuth 2.0 Authorization Framework)][rfc6749] 与 [OIDC Core 1.0][oidc-core], 在设计上大量参考了 [OAuth 2.1 草案][oauth2.1-draft] (如强制 PKCE、移除 implicit / password grant 等安全加固), 并在此之上扩展了 `wechat_authorization_code` 与 `urn:ietf:params:oauth:grant-type:app_assertion` 等自定义 grant type, 以及 `attest_jwt_client_auth` 客户端认证方式 ([draft-ietf-oauth-attestation-based-client-auth-08][attestation-draft]).

---

## 请求与响应

所有 Grant Type 都通过 Token 端点签发 Access Token (以及视情况下的 ID Token / Refresh Token).

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: <客户端认证, 见客户端认证方式>

grant_type=<grant_type>&scope=<scope>&...
```

### 通用请求参数

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `grant_type` | 是 | 见 [支持的 Grant Type](#支持的-grant-type) |
| `scope` | 否 | 申请的权限范围, 多个用空格分隔; 不传则使用客户端配置的全部 scope |
| ... | — | 其余参数因 grant type 而异, 详见各子文档 |

### 通用成功响应 (200) {#response}

```json
{
    "access_token": "eyJ...",
    "token_type": "Bearer",
    "expires_in": 299,
    "refresh_token": "...",
    "id_token": "eyJ...",
    "scope": "openid profile"
}
```

> `refresh_token` 与 `id_token` 是否返回受 grant type 与 scope 影响, 详见 [Token 说明](#token-说明).

### 通用错误响应 (4xx)

```json
{"error": "invalid_client", "error_description": "..."}
```

错误码遵循 [RFC 6749 §5.2][rfc6749-5.2]: `invalid_request`, `invalid_client`, `invalid_grant`, `unauthorized_client`, `unsupported_grant_type`, `invalid_scope`.

---

## 支持的 Grant Type

| `grant_type` | 说明 | 子文档 |
| --- | --- | --- |
| `authorization_code` | 标准 OAuth2 / OIDC 授权码流程, 推荐配合 PKCE | _待补充_ |
| `refresh_token` | 使用 Refresh Token 续期 | _待补充_ |
| `client_credentials` | 客户端凭证, 仅获取客户端级 Token | _待补充_ |
| `password` | 用户名 + 密码 (内部用途, 不建议对外暴露) | _待补充_ |
| `wechat_authorization_code` | 微信扫码登录 (自定义) | _待补充_ |
| `urn:ietf:params:oauth:grant-type:app_assertion` | 基于 Apple App Attest 的设备认证 Grant (自定义) | [App Attest](OAuth2-Token-Grant-%23-App-Attest.md) |

> `authorization_code` / `refresh_token` / `client_credentials` 严格遵循 [RFC 6749][rfc6749] 与 [OIDC Core 1.0][oidc-core] 定义的标准行为, 用法可直接参考对应 RFC.

---

## 客户端认证方式

OAuth2 Client 在 `/oauth2/token` 上的认证方式由其 `tokenEndpointAuthMethod` 字段决定 (完整枚举参见 [OAuth2 Client 模型 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-%E6%9E%9A%E4%B8%BE%E5%80%BC)). 目前 user-center 中重点使用的有以下四种:

### `none` — 公共客户端

公共客户端 (Public Client) 不持有 client secret, 不在 `/oauth2/token` 上做客户端身份验证. 由于无法区分合法客户端与攻击者, **必须** 至少叠加以下增强手段之一:

* 使用 `authorization_code` + PKCE (`code_challenge` / `code_verifier`);
* 使用 `attest_jwt_client_auth` 作为附加的客户端证明 (`OAuth-Client-Attestation-*` 头).

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&client_id=default&code=...&code_verifier=...&redirect_uri=...
```

### `client_secret_basic` — HTTP Basic

通过 `Authorization: Basic base64(client_id:client_secret)` 头部传递客户端凭据 ([RFC 6749 §2.3.1][rfc6749-2.3.1]). 机密客户端推荐使用.

请求示例:

```http
POST /oauth2/token
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=...
```

### `client_secret_post` — 请求体携带

通过请求体 `client_id` / `client_secret` 参数传递客户端凭据. 仅在无法使用 `client_secret_basic` 时使用, 安全性弱于 Basic 方式 (易出现在访问日志中).

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=...&client_id=admin&client_secret=...
```

### `attest_jwt_client_auth` — 基于设备证明的客户端认证

定义见 [draft-ietf-oauth-attestation-based-client-auth-08 §13.4](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08#section-13.4). 客户端通过 `OAuth-Client-Attestation` / `OAuth-Client-Attestation-PoP` 等请求头携带设备证明与 PoP 数据, 可作为独立认证方式, 也可叠加在其他标准认证方式之上作为安全增强.

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation: <Client Attestation JWT>
OAuth-Client-Attestation-PoP: <PoP JWT>

grant_type=authorization_code&client_id=default&code=...&code_verifier=...
```

> 详细设计 (请求头语义、PoP 类型与载体、独立 / 增强两种使用场景、各认证方式与 PKCE 的组合约束) 较为复杂, 具体使用方式参考: [Attestation Based Client Auth 子文档](OAuth2-Token-Grant-%23-Attestation-Based-Client-Auth.md) (_待补充_).

---

## Scope 说明

`scope` 控制 Access Token 的权限范围, 以及 ID Token / UserInfo 中可见的 Claim 子集. 客户端只能申请其 `scopes` 白名单内的值, 越权会返回 `invalid_scope`.

### OpenID Connect 标准 Scope

| Scope | 含义 | 影响 |
| ---| --- | --- |
| `openid` | OIDC 必需 | **决定是否签发 `id_token`**; 未申请 `openid` 时即使 grant type 支持 OIDC 也不会签发 ID Token |
| `profile` | 用户基础资料 | UserInfo / ID Token 中追加: `nickname`, `avatar_url` |

### 自定义 Scope

| Scope | 含义 |
| --- | --- |
| `authorities` | Euler 扩展, 申请后 ID Token / UserInfo 中追加 `authorities` claim, 携带用户的权限集合 |

---

## Token 说明

### Access Token

* **用途**: 调用受保护资源时通过 `Authorization: Bearer <access_token>` 头携带.
* **格式** (由客户端 `accessTokenFormat` 决定):
  * `self-contained` (默认): JWT, 资源服务器可本地验签 (验签密钥从授权服务器 JWK Set 端点 `/oauth2/jwks` 获取).
  * `reference`: 不透明令牌, 资源服务器需通过 introspection 端点验证.
* **TTL**: 由客户端 `accessTokenTimeToLive` 决定; 未显式配置时使用授权服务器默认值.
* **响应字段**: `access_token`, `token_type` (恒为 `Bearer`), `expires_in`, `scope`.

### ID Token

* **触发条件**: **仅当请求 scope 包含 `openid` 时签发**.
* **格式**: JWT, 遵循 OIDC Core 1.0.
* **包含 Claim**: 标准 claim (`iss`, `sub`, `aud`, `exp`, `iat`, `auth_time`, `nonce`) + 由 scope 决定的可选 claim (见上).
* **`sub` 取值**: 用户在该 Client 的稳定标识. 是否在多个 Client 间共享, 取决于 [OIDC Core 1.0 §8 Subject Identifier Types][oidc-subject-types] 定义的两种类型:
  * **`public` (公共标识)**: 同一个用户面向所有 Client 的 `sub` 一致, 通常等于服务端内部用户标识 (如 `username`). 跨 Client 关联用户简单, 但弱化了用户在不同 Client 间的隐私隔离.
  * **`pairwise` (成对假名标识)**: 同一个用户对不同 Client 看到的 `sub` 不同 (按 `client_id` / `sector_identifier_uri` 派生), 不同 Client 无法基于 `sub` 关联同一用户身份, 适合多租户、第三方接入或对用户隐私要求较高的场景, 是业界通用的 "按接入 Client 隔离 `openid`" 做法.
* **用法**: ID Token 仅用于客户端识别用户身份, **不应**作为 API 调用凭据.

### Refresh Token

* **触发条件**: 同时满足以下条件才签发:
  1. 客户端 `authorizationGrantTypes` 包含 `refresh_token`;
  2. 当前 grant type 不在 [禁用名单](#refresh-token-禁用名单) 中.
* **格式**: 不透明引用令牌, 由授权服务器持久化.
* **TTL**: 由客户端 `refreshTokenTimeToLive` 决定.
* **滚动续期**: 当客户端 `reuseRefreshTokens=false` 时, 每次刷新颁发新的 refresh_token, 旧的立即失效.
* **使用方式**: 通过 `grant_type=refresh_token&refresh_token=...` 请求 `/oauth2/token` 续期.

#### Refresh Token 禁用名单

下列 grant type **永不**签发 refresh_token, 即使客户端配置了 `refresh_token` grant:

| Grant Type | 禁用原因                      |
| --- | ---------- |
| `urn:ietf:params:oauth:grant-type:app_assertion` | 每次请求都要求完整的设备证明 (`kid` + `challenge` + `attestation`/`assertion`), refresh_token 无法提供额外安全收益, 反而增加存储与验证开销 |

---

## 注意事项

* 所有 token 端点请求**必须**使用 HTTPS.
* `client_secret_post` 仅在无法使用 `client_secret_basic` 时使用, 避免 secret 出现在访问日志中.
* `client_secret_basic` 的 client_id / client_secret 需先经 URL form-encode, 再以 `:` 拼接做 Base64 编码, 详见 [RFC 6749 §2.3.1][rfc6749-2.3.1].
* `id_token` 不可作为 API 调用凭据; 调用受保护资源**只能**使用 `access_token`.
* 同一 user-center 实例内 `sub` claim 稳定; 对 `app_assertion` 的匿名用户而言, 重置设备 / 重新执行 attestation 会创建新的匿名用户并产生新的 `sub`.

## 相关文档

* [App Attest Grant 子文档](OAuth2-Token-Grant-%23-App-Attest.md)
* [OAuth2 Client 模型](Model-%23-OAuth2-Client.md)
* [创建 OAuth2 Client](APIs-%23-Admin-OAuth2-Client-Create.md)
* [RFC 6749 - The OAuth 2.0 Authorization Framework][rfc6749]
* [The OAuth 2.1 Authorization Framework (draft)][oauth2.1-draft]
* [OAuth 2.0 Attestation-Based Client Authentication (draft)][attestation-draft]
* [OpenID Connect Core 1.0][oidc-core]

[oidc-subject-types]: https://openid.net/specs/openid-connect-core-1_0.html#SubjectIDTypes
[oidc-core]: https://openid.net/specs/openid-connect-core-1_0.html
[attestation-draft]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08
[rfc6749]: https://datatracker.ietf.org/doc/html/rfc6749
[rfc6749-2.3.1]: https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1
[rfc6749-5.2]: https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
[oauth2.1-draft]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1
