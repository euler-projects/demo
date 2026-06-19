# 查询密钥

按 `kid` 查询单条 JWK 条目, 返回公钥投影视图 (不含私钥材料). 通用约定见 [home.md](home.md), 密钥完整结构定义见 [JWK Key](Model-%23-Jwk-Key.md).

## Request

### Url

```http
GET /admin/api/oauth2/jwks/{kid}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

| 参数名 | 类型   | 说明               |
|--------|--------|--------------------|
| kid    | string | 目标密钥标识 (Key ID) |

## Response

**Success Response (200):**

响应体为 [JWK Key](Model-%23-Jwk-Key.md).

```json
{
  "kid": "uc-active-2025",
  "alg": "RS256",
  "use": "sig",
  "status": "ACTIVE",
  "hasPrivate": true,
  "issuedAt": 1746288000000,
  "signing": true
}
```

**Error Response (404):**

指定 `kid` 不存在时返回.

> 本接口**不返回**任何私钥材料. `hasPrivate=true` 仅标记服务端密钥仓库中存在该条目的私钥.
