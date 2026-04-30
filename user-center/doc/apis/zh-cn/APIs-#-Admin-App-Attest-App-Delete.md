# 删除 App Attest 应用

按 `registrationId` 删除 App Attest 应用注册记录. 通用约定见 [home.md](home.md#通用约定).

## Request

### Url

```http
DELETE /admin/appattest/app/{registrationId}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|要删除的应用主键 (UUID 或预配置 Map key)|

## Response

**Success Response (204 No Content):**

无响应体.

> **警告**: 应用删除后, 该应用的 App Attest 证明会因 RP ID hash 不再可查而全部失效; 若启用了 OAuth2 联动注册, 相关 OAuth2 客户端记录不会随之删除, 需另行调用 OAuth2 客户端 API 清理. 操作不可逆.
