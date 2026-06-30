# 更新登录身份

按 `identityId` 更新登录身份. **无需任何校验**, 直接覆盖写入. 通用约定见 [home.md](home.md#通用约定), 登录身份完整结构定义见 [User Identity](Model-%23-User-Identity.md).

> **支持的 `identityType`**: 本接口仅支持对 `phone` 和 `email` 类型的身份执行更新. 其他类型 (如 `wechat` / `apple` / `google`) 为只读, 仅支持查询/列出/删除.

## Request

### Url

```http
PUT /admin/api/users/{userId}/identities/{identityId}
```

### Authorities

需要以下任一权限: `root`, `admin`.

### Content Type

```
application/json
```

### 路径参数

|参数名|类型|说明|
|---|---|---|
|userId|string|用户 ID (UUID)|
|identityId|string|要更新的登录身份主键 (UUID)|

### 请求体字段

请求体字段定义与 [创建登录身份](APIs-%23-Admin-User-Identity-Create.md#请求体字段) 一致 (不含 `identityType`). 只读字段 (`identityId` / `userId` / `subject` / `boundAt`) 始终由服务端管理, 请求体中出现会被忽略.

> **注意**: 本接口为管理端直接写入, 不做任何业务校验.

> **subject 重算**: 更新扩展字段 (如 `phone` / `email`) 后, 服务端将自动重新计算 `subject`.

### 请求示例

#### 示例 1: 换绑手机号

```json
{
    "phone": "+8613900139000"
}
```

服务端将自动重新计算 `subject`.

#### 示例 2: 换绑邮箱

```json
{
    "email": "new-user@example.com"
}
```

服务端将自动重新计算 `subject`.

## Response

**Success Response (200):**

响应体为更新后的完整 [User Identity](Model-%23-User-Identity.md) 模型, 与 [查询登录身份](APIs-%23-Admin-User-Identity-Get.md) 结构一致.

```json
{
    "identityId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "identityType": "phone",
    "subject": "b2c3d4e5f6a7b8c9d0e1f2a3b4c5d6e7f8a9b0c1d2e3f4a5b6c7d8e9f0a1b2c3",
    "boundAt": 1778899139687,
    "phone": "+8613*******00"
}
```

> 响应反映的是服务端落库后的最终状态, 可直接用于校对更新是否如预期. 敏感扩展字段仍以脱敏形式返回.
