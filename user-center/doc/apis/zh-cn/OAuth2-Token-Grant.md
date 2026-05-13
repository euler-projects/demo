# OAuth2 Token Grant

本文档描述 `/oauth2/token` 的通用约定、支持的 Grant Type、客户端认证方式、Scope 与各类 Token 的语义. 具体 grant type 的请求/响应细节请见对应子文档.

> 本 OAuth2 认证服务基于 [RFC 6749 (OAuth 2.0 Authorization Framework)][rfc6749] 与 [OIDC Core 1.0][oidc-core], 在设计上大量参考了 [OAuth 2.1 草案][oauth2.1-draft] (如强制 PKCE、移除 implicit / password grant 等安全加固), 并在此之上扩展了 `wechat_authorization_code` 与 `urn:ietf:params:oauth:grant-type:app_assertion` 等自定义 grant type, 以及 `attest_jwt_client_auth` 客户端认证方式 ([OAuth 2.0 Attestation-Based Client Authentication (draft)][attestation-draft]).

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
| Authorization | 否 | 位于请求头, 有多种格式, 相见 [客户端认证方式](#客户端认证方式) |
| grant_type | 是 | 认证方式. 详见 [支持的 Grant Types](#支持的-grant-types) |
| scope | 否 | 本次申请的权限范围, 多个用空格分隔. 详见 [支持的 Scopes](#支持的-scopes) |

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

| 属性名 | 类型 | 说明 |
| --- | --- | --- |
| access_token | string | 调用业务接口的身份凭证 |
| token_type | enum | `access_token` 类型, 恒为 `Bearer`|
| refresh_token | string | 用于续期 `access_token` 的临时凭证, 仅机密客户端会收到 |
| expires_in | int | `access_token` 的有效期, 单位: 秒|
| id_token | string | 基于 OIDC 协议签发的, 包含用户档案信息的 JWT, 可由客户端解析, 仅当请求 `scope` 包含 `openid` 时才返回 |
| scope | string | `access_token` 的权限范围, 多个用空格分隔, 如果申请 Token 时没有传入 `scope` 参数, 则响应中也不会有此属性 |

* `access_token` 的有效期很短, 通常为几分钟分钟到几小时; `refresh_token` 的有效期很长, 通常为数天到数月. 
* 每次使用 `access_token` 前都应检查有效期, 若已过期或剩余有效时间小于1分钟, 则应及时续期.
* 更多说明请参考 [RFC6749 §4](https://datatracker.ietf.org/doc/html/rfc6749#section-4)

### 通用错误响应 (4xx)

```json
{"error": "invalid_client", "error_description": "..."}
```

错误码遵循 [RFC 6749 §5.2][rfc6749-5.2].

---

## 支持的 Grant Types

| `grant_type` | 说明 | 子文档 |
| --- | --- | --- |
| `authorization_code` | 标准 OAuth2 / OIDC 授权码流程, 推荐配合 PKCE | _待补充_ |
| `refresh_token` | 使用 Refresh Token 续期 | _待补充_ |
| `wechat_authorization_code` | 微信登录 (自定义) | _待补充_ |
| `urn:ietf:params:oauth:grant-type:app_assertion` | 基于 Apple App Attest 的设备认证 Grant (自定义) | [App Attest](OAuth2-Token-Grant-%23-App-Attest.md) |

> `authorization_code` / `refresh_token` 严格遵循 [RFC 6749][rfc6749] 与 [OIDC Core 1.0][oidc-core] 定义的标准行为, 用法可直接参考对应 RFC.

---

## 支持的 Scopes

`scope` 控制 Access Token 的权限范围, 以及 ID Token / UserInfo 中可见的 Claim 子集. 客户端只能申请其 `scopes` 白名单内的值, 越权会返回 `invalid_scope`.

| Scope | 含义 | 影响 |
| ---| --- | --- |
| `openid` | OIDC 必需 | **决定是否签发 `id_token`**; 未申请 `openid` 时即使 grant type 支持 OIDC 也不会签发 ID Token |
| `profile` | 用户信息 | UserInfo / ID Token 中追加: `nickname`, `avatar_url` |

---

## 客户端认证方式

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

定义见 [OAuth 2.0 Attestation-Based Client Authentication (draft) §13.4](https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08#section-13.4). 客户端通过 `OAuth-Client-Attestation` / `OAuth-Client-Attestation-PoP` 等请求头携带设备证明与 PoP 数据, 可作为独立认证方式, 也可叠加在其他标准认证方式之上作为安全增强.

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

## 注意事项

* 所有 token 端点请求**必须**使用 HTTPS.
* `client_secret_post` 仅在无法使用 `client_secret_basic` 时使用, 避免 secret 出现在访问日志中.
* `client_secret_basic` 的 client_id / client_secret 需先经 URL form-encode, 再以 `:` 拼接做 Base64 编码, 详见 [RFC 6749 §2.3.1][rfc6749-2.3.1].
* `id_token` 不可作为 API 调用凭据; 调用受保护资源**只能**使用 `access_token`.
* 同一 Client 为同一用户申请的 token 的 `sub` claim 始终相同; 但不同 Client 之间可能不同, 取决于服务端是否对该 Client 启用 `pairwise` 模式.
* 对 `app_assertion` 的匿名用户而言, 重置设备 / 重新执行 attestation 会创建新的匿名用户并产生新的 `sub`.

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
