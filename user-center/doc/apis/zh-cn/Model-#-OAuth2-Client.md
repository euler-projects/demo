# OAuth2 客户端

Admin OAuth2 客户端接口的统一数据模型. 创建 / 查询 / 列出 / 更新接口共用本结构, 其中**请求体是本模型的子集** — 只接受 `REQ PARAM` 列包含 `C` / `U` 的字段, 其余均为服务端独占, 在请求体中出现会被忽略.

```json
{
  "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
  "clientId": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
  "clientIdIssuedAt": 1777709730123,
  "clientSecret": null,
  "clientSecretExpiresAt": null,
  "clientName": "example-web-app",
  "tokenEndpointAuthMethod": "client_secret_basic",
  "grantTypes": ["authorization_code", "refresh_token"],
  "responseTypes": ["code"],
  "redirectUris": ["https://app.example.com/callback"],
  "postLogoutRedirectUris": ["https://app.example.com/"],
  "scopes": ["openid", "profile"],
  "jwksUri": null,
  "jwks": null,
  "tokenEndpointAuthSigningAlgorithm": null,
  "idTokenSignedResponseAlgorithm": null,
  "tlsClientAuthSubjectDN": null,
  "tlsClientCertificateBoundAccessTokens": false,
  "clientSettings": { "...": "见 ClientSettings" },
  "tokenSettings":  { "...": "见 TokenSettings" }
}
```

| PROPERTY                              | TYPE         | REQ PARAM           | READONLY | DESCRIPTION                                                                                                                                 |
|---------------------------------------|--------------|---------------------|----------|---------------------------------------------------------------------------------------------------------------------------------------------|
| registrationId                        | string       | `-` `R` `U` `D` `-` | Yes      | 客户端内部主键, 带横线格式的 UUID; `POST` 由服务端分配, `GET` / `PUT` / `DELETE` 通过 URL path 指定 |
| clientId                              | string       | `-` `-` `-` `-` `-` | Yes      | OAuth 2.0 客户端标识, 由服务端按 [RFC 7591 §3.2.1][RFC-7591-3.2.1] 生成; 内部实现为 256 位 (32 字节) 强随机二进制数据的 Base64URL (无 `=` 填充) 编码, 定长 43 个字符, 取值集为 `[A-Za-z0-9_-]`, 可直接用于 HTTP 头 / form 参数 / URL path 而无需再次转义 |
| clientIdIssuedAt                      | timestamp(3) | `-` `-` `-` `-` `-` | Yes      | `clientId` 的签发时间戳 (RFC 7591 §3.2.1) |
| clientSecret                          | string       | `-` `-` `-` `-` `-` | Yes      | 常规响应中**始终为 `null`** (服务端仅保存哈希). 明文仅在客户端创建响应和密钥轮转响应中各一次性返回, 调用方必须立即妥善存储 |
| clientSecretExpiresAt                 | timestamp(3) | `-` `-` `-` `-` `-` | Yes      | `clientSecret` 过期时间, `null` 表示不过期; 由服务端按轮转策略决定, 默认 `null` |
| clientName                            | string       | `C` `-` `U` `-` `-` | No       | 客户端显示名称 |
| tokenEndpointAuthMethod               | enum         | `C` `-` `U` `-` `-` | No       | Token 端点客户端认证方式, 可选值见下方[枚举值对照表](#tokenendpointauthmethod-枚举值); 默认 `client_secret_basic` |
| grantTypes                            | string[]     | `C` `-` `U` `-` `-` | No       | 允许的 `grant_type` 列表, 可选值见下方[枚举值对照表](#granttypes-枚举值) |
| responseTypes                         | string[]     | `-` `-` `-` `-` `-` | Yes      | 允许的 `response_type` 列表, 由服务端根据 `grantTypes` 推导 |
| redirectUris                          | string[]     | `C` `-` `U` `-` `-` | No       | 允许的回调地址列表, 使用 Authorization Code Flow 时必填 |
| postLogoutRedirectUris                | string[]     | `C` `-` `U` `-` `-` | No       | OIDC 注销回调地址列表 |
| scopes                                | string[]     | `C` `-` `U` `-` `-` | No       | 允许申请的 `scope` 列表 |
| jwksUri                               | string       | `C` `-` `U` `-` `-` | No       | 客户端 JWKS 的 URL, 用于 `private_key_jwt` / `self_signed_tls_client_auth` 校验 |
| jwks                                  | object       | `-` `-` `-` `-` `-` | Yes      | 内联 JWKS, 结构遵循 [RFC 7517][RFC-7517]; 与 `jwksUri` 互补, 由服务端维护 |
| tokenEndpointAuthSigningAlgorithm     | string       | `-` `-` `-` `-` `-` | Yes      | `private_key_jwt` / `client_secret_jwt` 使用的签名算法, 例如 `RS256` |
| idTokenSignedResponseAlgorithm        | string       | `-` `-` `-` `-` `-` | Yes      | OIDC ID Token 签名算法, 例如 `RS256` |
| tlsClientAuthSubjectDN                | string       | `-` `-` `-` `-` `-` | Yes      | `tls_client_auth` 证书的 Subject DN |
| tlsClientCertificateBoundAccessTokens | boolean      | `-` `-` `-` `-` `-` | Yes      | 是否启用 [RFC 8705][RFC-8705] 证书绑定的 Access Token |
| clientSettings                        | object       | `C` `-` `U` `-` `-` | No       | 客户端级扩展配置, 结构见 [ClientSettings](#clientsettings) |
| tokenSettings                         | object       | `C` `-` `U` `-` `-` | No       | Token 级扩展配置, 结构见 [TokenSettings](#tokensettings) |

---

## ClientSettings

客户端级扩展配置, 位于客户端对象的 `clientSettings` 字段下.

`settings` 是客户端级配置的**主容器** — 以键值 Map 存储所有配置项, key 为规范化的配置键 (`settings.client.*`). 外层的 `requireProofKey` 等字段是 `settings` 中常用配置项的**具名快捷方式**.

* **请求**: 仅允许提交**外层具名字段**; 请求体中的 `settings` 会被忽略. 未出现的具名字段不会被覆盖.
* **响应**: 同时返回外层具名字段和底层 `settings` Map, 二者语义等价 (例如 `requireProofKey` 与 `settings["settings.client.require-proof-key"]` 始终一致).

**请求示例**:

```json
{
  "requireProofKey": true,
  "requireAuthorizationConsent": true
}
```

**响应示例**:

```json
{
  "requireProofKey": true,
  "requireAuthorizationConsent": true,
  "settings": {
    "settings.client.require-proof-key": true,
    "settings.client.require-authorization-consent": true
  }
}
```

| PROPERTY                    | TYPE    | REQ PARAM           | READONLY | DESCRIPTION                                                                                       |
|-----------------------------|---------|---------------------|----------|---------------------------------------------------------------------------------------------------|
| requireProofKey             | boolean | `C` `-` `U` `-` `-` | No       | 是否强制要求 PKCE (授权码流程); 默认 `false` |
| requireAuthorizationConsent | boolean | `C` `-` `U` `-` `-` | No       | 是否在授权码流程中显示同意页; 默认 `false` |
| settings                    | object  | `-` `-` `-` `-` `-` | Yes      | 完整的客户端级配置 Map (`settings.client.*` → value), **仅响应侧返回**; 请求体中出现会被忽略 |

---

## TokenSettings

Token 级扩展配置, 位于客户端对象的 `tokenSettings` 字段下. 规则与 [ClientSettings](#clientsettings) 一致 — `settings` 是 Token 级配置的主容器 (key 前缀 `settings.token.*`), 外层字段是常用配置项的具名快捷方式.

* **请求**: 仅允许提交外层具名字段; 请求体中的 `settings` 会被忽略.
* **响应**: 同时返回外层具名字段和底层 `settings` Map.

TTL 类字段统一使用**秒**为单位.

**请求示例**:

```json
{
  "authorizationCodeTimeToLive": 300,
  "accessTokenTimeToLive": 300,
  "accessTokenFormat": "self-contained",
  "deviceCodeTimeToLive": 300,
  "reuseRefreshTokens": false,
  "refreshTokenTimeToLive": 604800
}
```

**响应示例**:

```json
{
  "authorizationCodeTimeToLive": 300,
  "accessTokenTimeToLive": 300,
  "accessTokenFormat": "self-contained",
  "deviceCodeTimeToLive": 300,
  "reuseRefreshTokens": false,
  "refreshTokenTimeToLive": 604800,
  "settings": {
    "settings.token.authorization-code-time-to-live": 300,
    "settings.token.access-token-time-to-live": 300,
    "settings.token.access-token-format": "self-contained",
    "settings.token.device-code-time-to-live": 300,
    "settings.token.reuse-refresh-tokens": false,
    "settings.token.refresh-token-time-to-live": 604800
  }
}
```

| PROPERTY                    | TYPE    | REQ PARAM           | READONLY | DESCRIPTION                                                                                       |
|-----------------------------|---------|---------------------|----------|---------------------------------------------------------------------------------------------------|
| authorizationCodeTimeToLive | number  | `C` `-` `U` `-` `-` | No       | 授权码有效期, 单位秒; 默认 `300` (5 分钟) |
| accessTokenTimeToLive       | number  | `C` `-` `U` `-` `-` | No       | Access Token 有效期, 单位秒; 默认 `300` (5 分钟) |
| accessTokenFormat           | enum    | `C` `-` `U` `-` `-` | No       | Access Token 格式, 可选值 `self-contained` (JWT 自包含) 或 `reference` (服务端引用); 默认 `self-contained` |
| deviceCodeTimeToLive        | number  | `C` `-` `U` `-` `-` | No       | Device Code 有效期, 单位秒; 默认 `300` (5 分钟) |
| reuseRefreshTokens          | boolean | `C` `-` `U` `-` `-` | No       | 刷新 Token 时是否复用原 Refresh Token; 默认 `true` |
| refreshTokenTimeToLive      | number  | `C` `-` `U` `-` `-` | No       | Refresh Token 有效期, 单位秒; 默认 `3600` (1 小时) |
| settings                    | object  | `-` `-` `-` `-` `-` | Yes      | 完整的 Token 级配置 Map (`settings.token.*` → value), **仅响应侧返回**; 请求体中出现会被忽略 |

---

## `tokenEndpointAuthMethod` 枚举值

| VALUE                         | GENERATES SECRET | DESCRIPTION                                                                                                                                        |
|-------------------------------|------------------|----------------------------------------------------------------------------------------------------------------------------------------------------|
| `none`                        | No               | **默认值** 公共客户端, 不进行客户端认证, 此时必需启用 `PKCE` 或使用 [Attestation-Based Client Authentication][attestation-based-client-auth] 作为增强验证手段 |
| `client_secret_basic`         | Yes              | HTTP Basic 头携带 `clientId:clientSecret` ([RFC 6749 §2.3.1][RFC-6749-2.3.1]) |
| `client_secret_post`          | Yes              | 请求体携带 `client_secret` 参数 |
| `client_secret_jwt`           | Yes              | 使用 `clientSecret` 派生的 HMAC 签名 JWT 作为凭据 |
| `private_key_jwt`             | No               | 使用客户端私钥签名 JWT (非对称), 通过 `jwksUri` / `jwks` 公钥校验 |
| `tls_client_auth`             | No               | 双向 TLS + CA 颁发证书, 通过 `tlsClientAuthSubjectDN` 绑定 ([RFC 8705][RFC-8705]) |
| `self_signed_tls_client_auth` | No               | 双向 TLS + 自签名证书, 通过 `jwksUri` / `jwks` 校验 ([RFC 8705][RFC-8705]) |


---

## `grantTypes` 枚举值

| VALUE                                             | DESCRIPTION                                                                                                              |
|---------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------|
| `authorization_code`                              | 授权码流程 ([RFC 6749 §4.1][RFC-6749-4.1]), 面向有能力完成浏览器重定向的机密 / 公共客户端; **依赖**: `redirectUris` 必填 |
| `refresh_token`                                   | 使用 Refresh Token 换取新 Access Token ([RFC 6749 §6][RFC-6749-6]); **依赖**: 通常与其他 grant 同时启用 |
| `client_credentials`                              | 客户端自身凭据获取 Token ([RFC 6749 §4.4][RFC-6749-4.4]), 服务端到服务端调用 |
| `urn:ietf:params:oauth:grant-type:device_code`    | Device Authorization Grant ([RFC 8628][RFC-8628]), 面向输入受限的设备 (如 TV / CLI) |
| `urn:ietf:params:oauth:grant-type:jwt-bearer`     | JWT Bearer Grant ([RFC 7523][RFC-7523]), 使用 JWT 作为授权凭据换取 Token; **依赖**: `jwksUri` / `jwks` |
| `urn:ietf:params:oauth:grant-type:token-exchange` | Token 交换 ([RFC 8693][RFC-8693]), 用已有 Token 换取新 Token |

[RFC-6749-2.3.1]: https://datatracker.ietf.org/doc/html/rfc6749#section-2.3.1
[RFC-6749-4.1]: https://datatracker.ietf.org/doc/html/rfc6749#section-4.1
[RFC-6749-4.4]: https://datatracker.ietf.org/doc/html/rfc6749#section-4.4
[RFC-6749-6]: https://datatracker.ietf.org/doc/html/rfc6749#section-6
[RFC-7517]: https://datatracker.ietf.org/doc/html/rfc7517
[RFC-7523]: https://datatracker.ietf.org/doc/html/rfc7523
[RFC-7591-3.2.1]: https://datatracker.ietf.org/doc/html/rfc7591#section-3.2.1
[RFC-8628]: https://datatracker.ietf.org/doc/html/rfc8628
[RFC-8693]: https://datatracker.ietf.org/doc/html/rfc8693
[RFC-8705]: https://datatracker.ietf.org/doc/html/rfc8705
[attestation-based-client-auth]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08
