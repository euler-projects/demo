# 更新 App Attest 应用

按 `registrationId` 全量更新 App Attest 应用注册记录. 语义为 `replace` — 请求体之外的**可变字段**将被重置为默认值. 通用约定见 [home.md](home.md#通用约定), 应用完整结构见 [App Attest App](Model-%23-App-Attest-App.md).

## Request

### Url

```http
PUT /admin/appattest/app/{registrationId}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|要更新的应用主键 (UUID 或预配置 Map key); **以 path 变量为权威值**, 请求体中的同名字段会被忽略|

### Content Type

```
application/json
```

### 请求体字段

请求体为 [App Attest App](Model-%23-App-Attest-App.md) 模型的子集, 仅接受下列字段: `teamId`, `bundleId`, `oauth2Enabled`, `oauth2ClientType`. `registrationId` 不可变更 (以 URL path 为准); `appId` 由服务端派生.

### 请求示例

```json
{
    "teamId": "ABCD1234EF",
    "bundleId": "com.example.app.v2",
    "oauth2Enabled": true,
    "oauth2ClientType": "DYNAMIC"
}
```

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|teamId|string|Apple Developer Team ID|是|无|
|bundleId|string|应用 Bundle Identifier|是|无|
|oauth2Enabled|boolean|是否联动 OAuth2 客户端注册|否|`false`|
|oauth2ClientType|enum|OAuth2 客户端注册策略, 可选值见 [App Attest App - oauth2ClientType 枚举值](Model-%23-App-Attest-App.md#oauth2clienttype-枚举值); `oauth2Enabled=true` 时必填|否|无|

## Response

**Success Response (200):**

响应体为更新后的完整 [App Attest App](Model-%23-App-Attest-App.md) 模型.

```json
{
    "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
    "appId": "ABCD1234EF.com.example.app.v2",
    "teamId": "ABCD1234EF",
    "bundleId": "com.example.app.v2",
    "oauth2Enabled": true,
    "oauth2ClientType": "DYNAMIC"
}
```

> 更新 `teamId` 或 `bundleId` 会导致 `appId` 及其 SHA-256 指纹 (App Attest RP ID hash) 重新派生; 已签发的 App Attest 证明会因 RP ID hash 变化而失效, 操作前请评估影响.
