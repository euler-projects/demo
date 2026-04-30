# 部分更新客户端

按 `registrationId` 部分更新客户端配置. 语义为 `patch` — 仅对请求体中出现且**非 `null`** 的字段生效, 未提交或为 `null` 的字段保持原值不变. 如需全量替换语义, 请使用 [更新客户端](APIs-%23-Admin-OAuth2-Client-Update.md) (PUT). 通用约定见 [home.md](home.md#通用约定), 客户端完整结构见 [OAuth2 Client](Model-%23-OAuth2-Client.md).

## Request

### Url

```http
PATCH /admin/oauth2/client/{registrationId}
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

请求体字段定义与 [创建客户端](APIs-%23-Admin-OAuth2-Client-Create.md#请求体字段) 完全一致, 但**所有字段均为可选**: 仅提交需要修改的字段即可, 未提交或 `null` 的字段保持原值不变. `registrationId` 始终以 URL path 为准, 即使请求体中出现也会被忽略.

> **说明**: 本接口不涉及 `clientSecret` 的更新. 如需更换密钥, 请调用 [轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md).

> **集合类字段注意**: `grantTypes` / `redirectUris` / `postLogoutRedirectUris` / `scopes` 等集合字段按**整体替换**生效 (即 `null` 不动, 非 `null` 则整体替换为提交值); 要追加或移除单个元素, 需调用方自行拉取当前值后合并再提交.

### 请求示例

#### 示例 1: 仅收紧同意页 / PKCE 策略

```json
{
    "clientSettings": {
        "requireProofKey": true,
        "requireAuthorizationConsent": true
    }
}
```

只变更 `clientSettings` 中对应的两项策略; `clientName` / `grantTypes` / `redirectUris` 等其余字段保持不变.

> **提醒**: `clientSettings` 本身按**整体替换**语义处理 — 提交的 `clientSettings` 对象会覆盖原有整个 `clientSettings`. 若原来还存在其它 `clientSettings` 子项且希望保留, 需一并带上.

#### 示例 2: 仅延长 Access Token 有效期

```json
{
    "tokenSettings": {
        "accessTokenTimeToLive": 1800
    }
}
```

只修改 `tokenSettings.accessTokenTimeToLive`; 但注意 `tokenSettings` 整体也会被提交内容替换, 其它子项会恢复为默认值. 如需精确控制, 请先 [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 拉取完整 `tokenSettings` 后再整体提交.

#### 示例 3: 仅补充回调地址

```json
{
    "redirectUris": [
        "https://app.example.com/callback",
        "https://app.example.com/auth/callback"
    ]
}
```

在 `redirectUris` 集合字段上**整体替换**为新列表; 必须一次性提交完整列表, 遗漏任何一项会断裂对应回调.

## Response

**Success Response (200):**

响应体为更新后的完整 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型, 与 [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 结构一致: `clientSecret` 始终为 `null` (服务端仅保存哈希; 如需更换请调用 [轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md)).

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

> 响应反映的是服务端落库后的最终配置, 可直接用于校对部分更新是否如预期. `clientId` / `clientIdIssuedAt` 与创建时一致, 不会因更新而改变.
