# 更新客户端

整体替换指定 `registrationId` 客户端的配置. 通用约定见 [home.md](home.md#通用约定).

## Request

### Url

```http
PUT /admin/oauth2/client/{registrationId}
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

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|要更新的客户端主键|

### 请求体字段

与 [创建客户端](APIs-%23-Admin-OAuth2-Client-Create.md#请求体字段) 完全一致. `registrationId` 始终以 URL path 为准, 即使请求体中出现也会被忽略.

> **说明**: 更新采用替换式语义, 未传入的可选字段会被重置为空; 如需保留既有值, 请先调用 [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 获取全量内容后再提交.

> **说明**: 本接口不涉及 `clientSecret` 的更新. 如需更换密钥, 请调用 [轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md).

### 请求示例

> **提醒**: 因采用**替换式**语义, 下列示例均假设调用方已拉取当前客户端的全量配置, 仅在此基础上修改关注的字段并整体提交. 未列出的可选字段会被清空.

#### 示例 1: 收紧安全策略 (启用 PKCE + 同意页, 延长 Token 有效期)

在保持 `client_secret_basic` 认证方式不变的前提下, 为历史遗留的机密 Web 应用**追加 PKCE 与同意页强制要求**, 并放宽 Token / Refresh Token 有效期.

```json
{
    "clientName": "example-web-app-v2",
    "tokenEndpointAuthMethod": "client_secret_basic",
    "grantTypes": ["authorization_code", "refresh_token"],
    "redirectUris": ["https://app.example.com/callback", "https://app.example.com/auth/callback"],
    "postLogoutRedirectUris": ["https://app.example.com/"],
    "scopes": ["openid", "profile", "email"],
    "clientSettings": {
        "requireProofKey": true,
        "requireAuthorizationConsent": true
    },
    "tokenSettings": {
        "accessTokenTimeToLive": 600,
        "refreshTokenTimeToLive": 1209600,
        "reuseRefreshTokens": false,
        "accessTokenFormat": "self-contained"
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 改为 `example-web-app-v2` 以标记新版本; 替换式语义下必须显式提交, 否则会被清空|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式. **本示例**: 保持 `client_secret_basic` 不变, 仅收紧安全策略, 原 `clientSecret` 继续有效|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 保留 `authorization_code` + `refresh_token`, 不改变授权流程|是|无|
|redirectUris|string[]|允许的回调地址列表. **本示例**: 在原回调基础上**追加** `.../auth/callback` 新版本前端路由; 必须一次性提交完整列表, 遗漏任何一项会导致老版本断裂|否|无|
|postLogoutRedirectUris|string[]|OIDC 注销回调地址列表. **本示例**: 显式保留首页, 以免被替换式语义清空|否|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 新增 `email` 以支持邮箱展示; 原有 `openid`, `profile` 必须完整列出|是|无|
|clientSettings.requireProofKey|boolean|是否强制要求 PKCE (授权码流程). **本示例**: 从 `false` 切换到 `true`, **已签发的不带 PKCE 的授权码请求会立即失败**, 需同步通知所有调用方|否|`false`|
|clientSettings.requireAuthorizationConsent|boolean|是否在授权码流程中显示同意页. **本示例**: 开启, 登录链路会新增一步同意页, 需要验证前端跳转兼容性|否|`false`|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: 调整到 `600` (10 分钟), 为常规请求留出更长容忍窗口|否|`300`|
|tokenSettings.refreshTokenTimeToLive|number|Refresh Token 有效期, 单位秒. **本示例**: 调整到 `1209600` (14 天), 减少用户重新登录频次; 变更后旧 Token 仍按签发时的有效期生效|否|`3600`|
|tokenSettings.reuseRefreshTokens|boolean|刷新 Token 时是否复用原 Refresh Token. **本示例**: 显式置 `false` 开启轮换, 与更严格的安全策略匹配|否|`true`|
|tokenSettings.accessTokenFormat|enum|Access Token 格式. **本示例**: `self-contained`, 与更新前保持一致, 避免资源服务器同步改造|否|`self-contained`|

#### 示例 2: 迁移客户端认证方式 (共享密钥 → 私钥 JWT)

将原先使用 `client_secret_basic` 的客户端**切换为非对称密钥认证**, 避免继续使用共享密钥.

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
|clientName|string|客户端显示名称. **本示例**: 保持原名, 仅变更认证方式|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式. **本示例**: **核心变更** —— 从共享密钥切换为 `private_key_jwt` 非对称认证; 原 `clientSecret` 不会被删除但失去使用场景, 如需彻底失效应单独调用[轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md)|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 保留 `authorization_code` + `refresh_token`, 只改客户端认证方式|是|无|
|redirectUris|string[]|允许的回调地址列表. **本示例**: 保持原有合作方回调|否|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 保留 OIDC 基础 scope|是|无|
|jwksUri|string|客户端 JWKS 的 URL, 用于 `private_key_jwt` / `self_signed_tls_client_auth` 校验. **本示例**: **本次新增**, 是 `private_key_jwt` 校验客户端签名的必要前置; 切换前应先部署 JWKS 端点并确认可公开访问|否|无|
|clientSettings.requireProofKey|boolean|是否强制要求 PKCE (授权码流程). **本示例**: 在非对称客户端认证之上继续叠加 PKCE, 形成纵深防御|否|`false`|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: `300`, 缩短至默认值以对齐合作伙伴场景对密钥防扩散的要求|否|`300`|
|tokenSettings.refreshTokenTimeToLive|number|Refresh Token 有效期, 单位秒. **本示例**: `86400` (1 天), 在便利性与安全性间取平衡|否|`3600`|

> **本示例刻意缺省的字段**: `postLogoutRedirectUris` 未提交会被**清空**; 若原客户端已配置 OIDC 注销回调, 必须显式保留, 否则登出链路将失效.

#### 示例 3: 扩展授权类型 (新增 Client Credentials 与 admin scope)

在原有后台服务客户端基础上追加 `client_credentials` 以支撑新批处理场景, 并开放管理员级 scope.

```json
{
    "clientName": "backend-service",
    "tokenEndpointAuthMethod": "client_secret_post",
    "grantTypes": ["client_credentials", "refresh_token"],
    "scopes": ["api:read", "api:write", "api:admin"],
    "tokenSettings": {
        "accessTokenTimeToLive": 3600,
        "accessTokenFormat": "self-contained"
    }
}
```

**关键差异字段说明**:

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|clientName|string|客户端显示名称. **本示例**: 保持原名, 仅扩展能力|是|无|
|tokenEndpointAuthMethod|enum|Token 端点客户端认证方式. **本示例**: 保持 `client_secret_post` 不变; 若之前为其他方式, 替换后需全链路同步调整 Token 端点调用方式|否|`client_secret_basic`|
|grantTypes|string[]|允许的 `grant_type` 列表. **本示例**: 新增 `client_credentials` 支撑批处理场景; 因采用整体替换, 必须同时列出需要保留的 `refresh_token`, 否则旧流程断裂|是|无|
|scopes|string[]|允许申请的 `scope` 列表. **本示例**: 新增 `api:admin` 扩展管理员能力; 调整后应审核下游服务对 scope 的授权校验, 避免权限越界|是|无|
|tokenSettings.accessTokenTimeToLive|number|Access Token 有效期, 单位秒. **本示例**: 保持 `3600` (1 小时), 满足批处理长任务需求|否|`300`|
|tokenSettings.accessTokenFormat|enum|Access Token 格式. **本示例**: `self-contained`, 让资源服务器无需回源校验|否|`self-contained`|

> **本示例刻意缺省的字段**: `redirectUris` / `postLogoutRedirectUris` / `clientSettings` 均未提交等同于**清空**; 若该客户端之前同时承担用户态流程, 此次更新会破坏旧配置, 需谨慎评估.

## Response

**Success Response (200):**

响应体为更新后的完整 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型, 与 [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 结构一致: `clientSecret` 始终为 `null` (服务端仅保存哈希; 如需更换请调用[轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md)).

```json
{
    "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
    "clientId": "dBjftJeZ4CVP-mB92K27uhbUJU1p1r_wW1gFWFOEjXk",
    "clientIdIssuedAt": 1777709730123,
    "clientSecret": null,
    "clientSecretExpiresAt": null,
    "clientName": "example-web-app-v2",
    "tokenEndpointAuthMethod": "client_secret_basic",
    "grantTypes": ["authorization_code", "refresh_token"],
    "responseTypes": ["code"],
    "redirectUris": ["https://app.example.com/callback", "https://app.example.com/auth/callback"],
    "postLogoutRedirectUris": ["https://app.example.com/"],
    "scopes": ["openid", "profile", "email"],
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

> 响应反映的是服务端落库后的最终配置, 可直接用于校对替换式更新是否如预期 (注意未提交的可选字段会被清空). `clientId` / `clientIdIssuedAt` 与创建时一致, 不会因更新而改变.
