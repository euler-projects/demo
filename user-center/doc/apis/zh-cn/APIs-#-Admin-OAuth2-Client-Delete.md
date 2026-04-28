# 删除客户端

按 `registrationId` 删除 OAuth 2.0 客户端及其相关数据. 通用约定见 [home.md](home.md#通用约定).

## Request

### Url

```http
DELETE /admin/oauth2/client/{registrationId}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|要删除的客户端主键|

## Response

**Success Response (204 No Content):**

无响应体.

> **警告**: 客户端删除后, 其已签发的 Token、授权码、授权记录均将失效, 操作不可逆.
