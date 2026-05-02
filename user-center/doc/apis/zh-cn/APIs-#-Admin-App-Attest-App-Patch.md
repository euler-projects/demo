# 部分更新 App Attest 应用

按 `registrationId` 部分更新 App Attest 应用注册记录. 语义为 `patch` — 仅对请求体中出现且**非 `null`** 的字段生效, 未提交或为 `null` 的字段保持原值不变. 如需全量替换语义, 请使用 [更新应用](APIs-%23-Admin-App-Attest-App-Update.md) (PUT). 通用约定见 [home.md](home.md#通用约定), 应用完整结构见 [App Attest App](Model-%23-App-Attest-App.md).

## Request

### Url

```http
PATCH /admin/appattest/app/{registrationId}
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

请求体为 [App Attest App](Model-%23-App-Attest-App.md) 模型的子集, 可选字段: `teamId`, `bundleId`, `oauth2Enabled`, `oauth2ClientType`. `registrationId` 不可变更 (以 URL path 为准); `appId` 由服务端派生.

> **约束**: `teamId` 与 `bundleId` 必须**成对**提交 — 两者同时存在或同时缺省; 仅提交其一会返回 `400 Bad Request`. 因为 `appId` 由两者共同派生, 任一字段单独变更会破坏派生一致性.

### 请求示例

#### 示例 1: 仅切换 `oauth2Enabled`

```json
{
    "oauth2Enabled": false
}
```

关闭 OAuth2 联动, 其它字段 (如 `teamId`, `bundleId`, `oauth2ClientType`) 保持不变.

#### 示例 2: 同时更新 `teamId` 与 `bundleId`

```json
{
    "teamId": "WXYZ5678GH",
    "bundleId": "com.example.app.v3"
}
```

派生的 `appId` 会重新计算; `oauth2Enabled` / `oauth2ClientType` 保持不变.

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|teamId|string|Apple Developer Team ID; 与 `bundleId` 必须**成对**提交|否|保持原值|
|bundleId|string|应用 Bundle Identifier; 与 `teamId` 必须**成对**提交|否|保持原值|
|oauth2Enabled|boolean|是否联动 OAuth2 客户端注册|否|保持原值|
|oauth2ClientType|enum|OAuth2 客户端注册策略, 可选值见 [App Attest App - oauth2ClientType 枚举值](Model-%23-App-Attest-App.md#oauth2clienttype-枚举值)|否|保持原值|

## Response

**Success Response (200):**

响应体为更新后的完整 [App Attest App](Model-%23-App-Attest-App.md) 模型.

```json
{
    "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
    "appId": "WXYZ5678GH.com.example.app.v3",
    "teamId": "WXYZ5678GH",
    "bundleId": "com.example.app.v3",
    "oauth2Enabled": true,
    "oauth2ClientType": "DYNAMIC"
}
```

> 更新 `teamId` 或 `bundleId` 会导致 `appId` 及其 SHA-256 指纹 (App Attest RP ID hash) 重新派生; 已签发的 App Attest 证明会因 RP ID hash 变化而失效, 操作前请评估影响.
