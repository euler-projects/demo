# 删除登录身份

按 `identity_id` 删除一条登录身份记录. 通用约定见 [home.md](home.md#通用约定).

## Request

### Url

```http
DELETE /admin/api/users/{userId}/identities/{identityId}
```

### Authorities

需要以下任一权限: `root`, `admin`.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|userId|string|用户 ID (UUID)|
|identityId|string|要删除的登录身份主键 (UUID)|

## Response

**Success Response (204 No Content):**

无响应体.

> **警告**: 登录身份删除后, 用户将无法再通过该身份登录, 操作不可逆.
