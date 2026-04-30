# 创建 App Attest 应用

创建一条新的 App Attest 应用注册记录. 通用约定见 [home.md](home.md#通用约定), 应用完整结构见 [App Attest App](Model-%23-App-Attest-App.md).

## Request

### Url

```http
POST /admin/appattest/app
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

请求体为 [App Attest App](Model-%23-App-Attest-App.md) 模型的子集, 仅接受下列字段: `teamId`, `bundleId`, `oauth2Enabled`, `oauth2ClientType`. 详细语义请参阅 [App Attest App](Model-%23-App-Attest-App.md) 模型.

> `registrationId` 为服务端独占字段, 由服务端分配为 UUID 格式; 请求体中出现的同名字段会被忽略. 人类可读的 `registrationId` 仅保留给通过 `application.yml` 以 `euler.security.app-attest.apps.<key>` 预配置的条目. 其他属于 [App Attest App](Model-%23-App-Attest-App.md) 模型但未在上述列表中的字段 (`appId` 等) 由服务端派生, 请求体中出现会被忽略.

### 请求示例

#### 示例 1: 仅启用 App Attest

面向只需要完成 Apple App Attest 证明、暂不联动 OAuth2 客户端注册的场景.

```json
{
    "teamId": "ABCD1234EF",
    "bundleId": "com.example.app",
    "oauth2Enabled": false
}
```

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|teamId|string|Apple Developer Team ID. **本示例**: 10 位字母数字|是|无|
|bundleId|string|应用 Bundle Identifier. **本示例**: 反向域名风格|是|无|
|oauth2Enabled|boolean|是否联动 OAuth2 客户端注册. **本示例**: `false`, 不触发 OAuth2 侧动作|否|`false`|

#### 示例 2: 启用静态 OAuth2 客户端联动

面向预置 OAuth2 客户端的静态部署模式, `clientId` 由 App Attest 侧按固定约定派生.

```json
{
    "teamId": "ABCD1234EF",
    "bundleId": "com.example.app",
    "oauth2Enabled": true,
    "oauth2ClientType": "STATIC"
}
```

|字段名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|oauth2Enabled|boolean|是否联动 OAuth2 客户端注册. **本示例**: `true`, 需同步提供 `oauth2ClientType`|否|`false`|
|oauth2ClientType|enum|OAuth2 客户端注册策略, 可选值见 [App Attest App - oauth2ClientType 枚举值](Model-%23-App-Attest-App.md#oauth2clienttype-枚举值). **本示例**: `STATIC`, 使用预置客户端|否 (`oauth2Enabled=true` 时必填)|无|

## Response

**Success Response (200):**

响应体为完整的 [App Attest App](Model-%23-App-Attest-App.md) 模型, `registrationId` 已由服务端分配为 UUID.

```json
{
    "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
    "appId": "ABCD1234EF.com.example.app",
    "teamId": "ABCD1234EF",
    "bundleId": "com.example.app",
    "oauth2Enabled": true,
    "oauth2ClientType": "STATIC"
}
```

> `appId` 由服务端根据 `teamId` + `bundleId` 派生; `registrationId` 由服务端分配为 UUID 格式, 请求体中无需提供.
