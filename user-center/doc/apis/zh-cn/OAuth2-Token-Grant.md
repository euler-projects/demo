# OAuth2 Token Grant

本文档描述 `/oauth2/token` 接口的通用约定. 不同 Grant Type 的具体细节请见对应子文档.

> 本 OAuth2 认证服务以 [RFC 6749 (OAuth 2.0 Authorization Framework)][rfc6749] 和 [OIDC Core 1.0][oidc-core] 标准为基础, 在设计上大量参考了 [The OAuth 2.1 Authorization Framework (draft)][oauth2.1-draft], 并支持增强型客户端认证 ([OAuth 2.0 Attestation-Based Client Authentication (draft)][attestation-draft]).

## 请求格式

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
Authorization: <客户端认证, 见客户端认证方式>

grant_type=<grant_type>&scope=<scope>&...
```

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| grant_type | 是 | 认证方式. |
| scope | 否 | 本次申请的权限范围, 多个用空格分隔. |
| ... | ... | 其他参数随不同的 `grant_type` 和不同的客户端认证方式而不同, 请参考具体文档. |

更多详细说明请继续阅读:
- [OAuth2 客户端认证][OAuth2 Client Authentication]
- [Grant Types](#grant-types)
- [Scopes](#scopes)

## 响应格式

### 成功响应格式

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
* `refresh_token` 是一次性的, 每次续期都会重新下发新的 `refresh_token`.
* 更多说明请参考 [RFC6749 §4].

### 错误响应格式

```json
{"error": "invalid_client", "error_description": "..."}
```

错误码遵循 [RFC6749 §5.2].

---

## Grant Types

| `grant_type` | 说明 | 子文档 |
| --- | --- | --- |
| `authorization_code` | 标准 OAuth2 / OIDC 授权码流程, 推荐配合 PKCE | _待补充_ |
| `refresh_token` | 使用 Refresh Token 续期 | _待补充_ |
| `urn:ietf:params:oauth:grant-type:app_assertion` | 基于 Apple App Attest 的设备认证 Grant (自定义) | [App Attest](OAuth2-Token-Grant-%23-App-Attest.md) |

> `authorization_code` / `refresh_token` 严格遵循 [RFC 6749][rfc6749] 与 [OIDC Core 1.0][oidc-core] 定义的标准行为, 用法可直接参考对应 RFC.

---

## Scopes

`scope` 控制 Access Token 的权限范围, 以及 ID Token / UserInfo 中可见的 Claim 子集. 客户端只能申请其 `scopes` 白名单内的值, 越权会返回 `invalid_scope`.

| Scope | 含义 | 影响 |
| ---| --- | --- |
| `openid` | OIDC 必需 | **决定是否签发 `id_token`**; 未申请 `openid` 时即使 grant type 支持 OIDC 也不会签发 ID Token |
| `profile` | 用户信息 | UserInfo / ID Token 中追加: `nickname`, `avatar_url` |

## 注意事项

* `id_token` 不可作为 API 调用凭据; 调用受保护资源**只能**使用 `access_token`.
* 同一 Client 为同一用户申请的 token 的 `sub` claim 始终相同; 但不同 Client 之间可能不同, 取决于服务端是否对该 Client 启用 `pairwise` 模式.
* 对 `app_assertion` 的匿名用户而言, 重置设备 / 重新执行 attestation 会创建新的匿名用户并产生新的 `sub`.

## 相关文档

* [App Attest Grant 子文档](OAuth2-Token-Grant-%23-App-Attest.md)
* [OAuth2 Client Authentication]
* [OAuth2 Client 模型](Model-%23-OAuth2-Client.md)
* [创建 OAuth2 Client](APIs-%23-Admin-OAuth2-Client-Create.md)
* [RFC 6749 - The OAuth 2.0 Authorization Framework][rfc6749]
* [The OAuth 2.1 Authorization Framework (draft)][oauth2.1-draft]
* [OAuth 2.0 Attestation-Based Client Authentication (draft)][attestation-draft]
* [OpenID Connect Core 1.0][oidc-core]

[OAuth2 Client Authentication]: OAuth2-Client-Authentication.md
[oidc-subject-types]: https://openid.net/specs/openid-connect-core-1_0.html#SubjectIDTypes
[oidc-core]: https://openid.net/specs/openid-connect-core-1_0.html
[attestation-draft]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08
[rfc6749]: https://datatracker.ietf.org/doc/html/rfc6749
[RFC6749 §4]: https://datatracker.ietf.org/doc/html/rfc6749#section-4
[RFC6749 §5.2]: https://datatracker.ietf.org/doc/html/rfc6749#section-5.2
[oauth2.1-draft]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-v2-1
