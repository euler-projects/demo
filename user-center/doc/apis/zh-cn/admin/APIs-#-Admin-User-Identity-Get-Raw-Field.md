# 查询登录身份脱敏字段原文

按 `identityId` 查询指定登录身份中某个脱敏字段的原始明文值. 用于管理端在需要时获取敏感信息原文 (如完整手机号、完整邮箱地址等). 通用约定见 [home.md](home.md#通用约定), 脱敏规则见 [User Identity - 脱敏与原文查询](Model-%23-User-Identity.md#3-脱敏与原文查询).

## Request

### Url

```http
GET /admin/api/users/{userId}/identities/{identityId}/raw-fields/{fieldName}
```

### Authorities

需要以下任一权限: `root`, `admin`.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|userId|string|用户 ID (UUID)|
|identityId|string|登录身份主键 (UUID)|
|fieldName|string|要查询原文的字段名, 如 `phone` / `email`|

## Response

**Success Response (200):**

响应体为单一对象, 包含请求的字段名及其原始明文值.

```json
{
    "fieldName": "phone",
    "rawValue": "+8613800138000"
}
```

### 响应示例

#### 示例 1: 查询手机号原文

```http
GET /admin/api/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/identities/550e8400-e29b-41d4-a716-446655440000/raw-fields/phone
```

```json
{
    "fieldName": "phone",
    "rawValue": "+8613800138000"
}
```

#### 示例 2: 查询邮箱原文

```http
GET /admin/api/users/a1b2c3d4-e5f6-7890-abcd-ef1234567890/identities/6ba7b810-9dad-11d1-80b4-00c04fd430c8/raw-fields/email
```

```json
{
    "fieldName": "email",
    "rawValue": "user@example.com"
}
```

> **安全提示**: 本接口返回敏感信息的明文原文, 调用方应严格控制使用场景, 避免日志记录或前端展示原文. 建议仅在必要时按需调用, 不要批量拉取.

> **字段不存在时**: 若请求的 `fieldName` 在该登录身份中不存在或该字段不具备脱敏处理, 服务端返回 `404 Not Found`.
