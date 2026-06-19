# 查询 App Attest 应用

按 `registrationId` 查询单条 App Attest 应用注册记录. 通用约定见 [home.md](home.md#通用约定), 应用完整结构见 [App Attest App](Model-%23-App-Attest-App.md).

## Request

### Url

```http
GET /admin/api/appattest/apps/{registrationId}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|应用内部主键|

## Response

**Success Response (200):**

响应体为完整的 [App Attest App](Model-%23-App-Attest-App.md) 模型. 当指定的 `registrationId` 不存在时, 响应体为 `null`.

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
