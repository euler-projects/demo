# 列出登录身份

列出指定用户的所有登录身份, 支持按身份类型筛选. 通用约定见 [home.md](home.md#通用约定), 登录身份完整结构定义见 [User Identity](Model-%23-User-Identity.md).

## Request

### Url

```http
GET /admin/api/users/{userId}/identities
```

### Authorities

需要以下任一权限: `root`, `admin`.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|userId|string|用户 ID (UUID)|

### 查询参数

|参数名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|identityType|string|按身份类型筛选, 如 `phone` / `email` / `wechat`|否|无|

## Response

**Success Response (200):**

响应体为数组, 每个元素是完整的 [User Identity](Model-%23-User-Identity.md) 模型.

```json
[
    {
        "identityId": "550e8400-e29b-41d4-a716-446655440000",
        "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "identityType": "phone",
        "subject": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
        "boundAt": 1778899139687,
        "phone": "+8613*******00"
    },
    {
        "identityId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
        "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
        "identityType": "email",
        "subject": "3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b",
        "boundAt": 1778899139687,
        "email": "u**r@e*****e.com"
    }
]
```

> **敏感字段脱敏**: 敏感扩展字段 (如 `phone` / `email`) 始终以脱敏形式返回. 如需查询原文, 请使用 [查询脱敏字段原文](APIs-%23-Admin-User-Identity-Get-Raw-Field.md).
