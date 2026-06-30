# 列出 App Attest 应用

按分页方式列出所有 App Attest 应用注册记录. 通用约定见 [home.md](home.md#通用约定), 应用完整结构见 [App Attest App](Model-%23-App-Attest-App.md).

## Request

### Url

```http
GET /admin/api/appattest/apps?offset={offset}&limit={limit}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 查询参数

|参数名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|offset|int|起始偏移 (从 0 开始)|是|无|
|limit|int|本次最多返回条数|是|无|

## Response

**Success Response (200):**

响应体为数组, 每个元素是完整的 [App Attest App](Model-%23-App-Attest-App.md) 模型.

```json
[
    {
        "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
        "appId": "ABCD1234EF.com.example.app",
        "teamId": "ABCD1234EF",
        "bundleId": "com.example.app",
        "oauth2Enabled": true,
        "oauth2ClientType": "STATIC"
    }
]
```

数组元素结构参见 [App Attest App](Model-%23-App-Attest-App.md).
