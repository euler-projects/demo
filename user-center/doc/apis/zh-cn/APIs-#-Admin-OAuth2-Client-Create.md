# 创建客户端

创建一个新的 OAuth 2.0 客户端. 服务端接管字段 (`clientId` / `clientIdIssuedAt` / `clientSecret` 等) 由本接口负责分配; 通用约定见 [home.md](home.md#通用约定), 客户端完整结构见 [OAuth2 客户端](Model-%23-OAuth2-Client.md).

## Request

### Url

```http
POST /admin/api/oauth2/clients
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### Content Type

```
application/json
```

### 请求体字段

请求体为 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型的子集, 仅接受下列字段: `clientName`, `tokenEndpointAuthMethod`, `grantTypes`, `redirectUris`, `postLogoutRedirectUris`, `scopes`, `jwksUri`, `clientSettings`, `tokenSettings`. 详细语义请参阅 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型, 下方为各典型场景的请求示例与字段说明.

> 其他属于 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型但未在上述列表中的字段由服务端管理, 请求体中出现会被忽略.

### 请求示例

#### 示例 1: 标准 Web 应用 (Authorization Code + PKCE)

面向有后端的机密 Web 应用, 使用授权码流程, 服务端持有 `clientSecret` 并在 Token 端点以 HTTP Basic 方式携带, 同时强制 PKCE 抵御授权码拦截攻击.

```json
{
    "clientName": "example-web-app",
    "tokenEndpointAuthMethod": "client_secret_basic",
    "grantTypes": ["authorization_code", "refresh_token"],
    "redirectUris": ["https://app.example.com/callback"],
    "postLogoutRedirectUris": ["https://app.example.com/"],
    "scopes": ["openid", "profile"],
    "clientSettings": {
        "requireProofKey": true,
        "requireAuthorizationConsent": true
    },
    "tokenSettings": {
        "accessTokenTimeToLive": 300,
        "refreshTokenTimeToLive": 604800,
        "reuseRefreshTokens": false,
        "accessTokenFormat": "self-contained"
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 使用易识别的业务名称, 便于在管理台区分|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式, 可选值见 [OAuth2 客户端 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-枚举值). **本示例**: 选 `client_secret_basic` 以让机密 Web 应用通过 HTTP Basic 携带 `clientSecret`; 响应会一次性返回明文密钥|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: `authorization_code` 完成用户登录, 叠加 `refresh_token` 以免频繁跳转登录|是|无|
|redirectUris|string[]|允许的回调地址列表, 使用 Authorization Code Flow 时必填. **本示例**: 使用 HTTPS 外部域名作为浏览器重定向目的地|否|无|
|postLogoutRedirectUris|string[]|OIDC 注销回调地址列表. **本示例**: 登出后跳回站点首页|否|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 仅需 OIDC 基础 scope (`openid`, `profile`), 遵循最小权限原则|是|无|
|clientSettings.requireProofKey|boolean|是否强制要求 PKCE (授权码流程). **本示例**: 即使是机密客户端也显式启用, 防范授权码拦截/重放|否|`false`|
|clientSettings.requireAuthorizationConsent|boolean|是否在授权码流程中显示同意页. **本示例**: 开启以便向用户明示授权范围|否|`false`|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: `300` (5 分钟), 与默认一致, 确保泄漏窗口足够短|否|`300`|
|tokenSettings.refreshTokenTimeToLive|number|Refresh Token 有效期, 单位秒. **本示例**: `604800` (7 天), 在默认 1 小时基础上延长以减少用户重新登录频次|否|`3600`|
|tokenSettings.reuseRefreshTokens|boolean|刷新 Token 时是否复用原 Refresh Token. **本示例**: 显式置 `false` 开启轮换, 降低长期泄漏风险|否|`true`|
|tokenSettings.accessTokenFormat|enum|Access Token 格式, 可选 `self-contained` (JWT 自包含) 或 `reference`. **本示例**: `self-contained`, 资源服务器可本地校验, 无需回源|否|`self-contained`|

#### 示例 2: 公共客户端 (无密钥, 强制 PKCE)

面向 SPA / 原生移动 App 等**无法安全保管密钥**的公共客户端. 不生成 `clientSecret`, 完全依赖 PKCE 保证授权码安全.

```json
{
    "clientName": "mobile-app",
    "tokenEndpointAuthMethod": "none",
    "grantTypes": ["authorization_code", "refresh_token"],
    "redirectUris": ["myapp://callback"],
    "scopes": ["openid", "profile", "email"],
    "clientSettings": {
        "requireProofKey": true
    },
    "tokenSettings": {
        "accessTokenTimeToLive": 600,
        "refreshTokenTimeToLive": 2592000
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 标记为移动端应用, 便于与 Web 版区分|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式, 可选值见 [OAuth2 客户端 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-枚举值). **本示例**: 选 `none` 表示公共客户端, **不会生成 `clientSecret`**, 因为移动端无法安全保管密钥|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 移动端仍走授权码流程换 Token, 叠加 `refresh_token` 降低重复登录|是|无|
|redirectUris|string[]|允许的回调地址列表, 使用 Authorization Code Flow 时必填. **本示例**: 使用自定义 URL Scheme `myapp://callback`, 以深度链接方式唤起移动端 App|否|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 追加 `email` 以便在 App 内展示用户邮箱|是|无|
|clientSettings.requireProofKey|boolean|是否强制要求 PKCE (授权码流程). **本示例**: 公共客户端下 PKCE 是**强依赖**, 必须为 `true`|否|`false`|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: `600` (10 分钟), 相较示例 1 略长, 减轻移动网络波动下的重试成本|否|`300`|
|tokenSettings.refreshTokenTimeToLive|number|Refresh Token 有效期, 单位秒. **本示例**: `2592000` (30 天), 匹配移动端长时间免登录体验|否|`3600`|

#### 示例 3: 服务端到服务端 (Client Credentials)

面向后端服务之间的非用户态调用, 以客户端自身凭据获取 Token, **不涉及浏览器重定向, 无需同意页**.

```json
{
    "clientName": "backend-service",
    "tokenEndpointAuthMethod": "client_secret_post",
    "grantTypes": ["client_credentials"],
    "scopes": ["api:read", "api:write"],
    "tokenSettings": {
        "accessTokenTimeToLive": 3600,
        "accessTokenFormat": "self-contained"
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 标记为后端服务, 便于与用户态客户端区分|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式, 可选值见 [OAuth2 客户端 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-枚举值). **本示例**: 选 `client_secret_post`, 将 `clientSecret` 放在请求体 `form` 参数中传递, 适配不便使用 Basic 头的 HTTP 客户端|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 仅 `client_credentials`, 以客户端自身凭据换取 Token, **不涉及用户交互**|是|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 使用业务自定义 scope (`api:read`, `api:write`) 控制服务端能力, 而非 OIDC 标准 scope|是|无|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: `3600` (1 小时), 后台批处理可容忍更长有效期以减少 Token 交换开销|否|`300`|
|tokenSettings.accessTokenFormat|enum|Access Token 格式, 可选 `self-contained` 或 `reference`. **本示例**: `self-contained`, 资源服务器无需回源即可校验|否|`self-contained`|

> **本示例刻意缺省的字段**: `redirectUris` / `postLogoutRedirectUris` / `clientSettings` 均未提交 —— `client_credentials` grant 不涉及浏览器重定向和授权码流程, 这些字段没有意义.

#### 示例 4: 使用私钥 JWT 认证

面向高安全合作伙伴场景, 使用**非对称密钥**替代共享密钥: 合作方用私钥签发断言 JWT, 服务端通过 `jwksUri` 拉取公钥校验.

```json
{
    "clientName": "secure-partner-app",
    "tokenEndpointAuthMethod": "private_key_jwt",
    "grantTypes": ["authorization_code", "refresh_token"],
    "redirectUris": ["https://partner.example.com/oauth/callback"],
    "scopes": ["openid", "profile"],
    "jwksUri": "https://partner.example.com/.well-known/jwks.json",
    "clientSettings": {
        "requireProofKey": true
    },
    "tokenSettings": {
        "accessTokenTimeToLive": 300,
        "refreshTokenTimeToLive": 86400
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 标记为合作伙伴 App, 便于审计追踪|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式, 可选值见 [OAuth2 客户端 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-枚举值). **本示例**: 选 `private_key_jwt`, 合作方使用**自有私钥**签发断言 JWT, **不会生成 `clientSecret`**, 避免在双方之间传递共享密钥|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 保持 `authorization_code` + `refresh_token` 的用户态流程, 与示例 1 相比仅客户端认证方式不同|是|无|
|redirectUris|string[]|允许的回调地址列表, 使用 Authorization Code Flow 时必填. **本示例**: 指向合作方站点的 OAuth 回调|否|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 使用 OIDC 基础 scope 完成身份识别|是|无|
|jwksUri|string|客户端 JWKS 的 URL, 用于 `private_key_jwt` / `self_signed_tls_client_auth` 校验. **本示例**: 指向合作方托管的 JWKS 端点, 服务端据此拉取公钥校验客户端签名的 JWT; 若使用 `private_key_jwt` 则必填|否|无|
|clientSettings.requireProofKey|boolean|是否强制要求 PKCE (授权码流程). **本示例**: 在非对称客户端认证基础上继续叠加 PKCE, 形成纵深防御|否|`false`|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: `300` (5 分钟), 合作伙伴场景下刻意缩短泄漏窗口|否|`300`|
|tokenSettings.refreshTokenTimeToLive|number|Refresh Token 有效期, 单位秒. **本示例**: `86400` (1 天), 在便利性与安全性间取平衡|否|`3600`|

## Response

**Success Response (200):**

响应体为完整的 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型. 与 [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 唯一的差异在于: **仅此一次**, `clientSecret` 字段会以明文形式返回新生成的密钥, 供调用方保存; 若 `tokenEndpointAuthMethod` 不使用共享密钥 (`none` / `private_key_jwt` / `tls_client_auth` / `self_signed_tls_client_auth`), 则 `clientSecret` 仍为 `null`.

```json
{
    "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
    "clientId": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
    "clientIdIssuedAt": 1777709730123,
    "clientSecret": "euler_sk_9qZt3C9m1s6cPqz1nB7Vw8X2Yl3kU4oA5dJhI6pR7eS",
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

> **重要**: `clientSecret` 字段仅在本次响应中以明文返回一次, 服务端仅保存哈希形式. 调用方必须立即妥善存储; 遗失后只能通过[轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md)重新签发. 后续所有 [查询](APIs-%23-Admin-OAuth2-Client-Get.md) / [列表](APIs-%23-Admin-OAuth2-Client-List.md) / [更新](APIs-%23-Admin-OAuth2-Client-Update.md) 响应中, 该字段均会被置为 `null`.
