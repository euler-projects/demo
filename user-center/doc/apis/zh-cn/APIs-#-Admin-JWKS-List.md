# 列出密钥

列出当前已管理的全部 JWK 条目. 通用约定见 [home.md](home.md), 密钥完整结构定义见 [JWK Key](Model-%23-Jwk-Key.md).

## Request

### Url

```http
GET /admin/oauth2/jwks
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

## Response

**Success Response (200):**

响应体为 [JWK Key](Model-%23-Jwk-Key.md) 数组.

```json
[
  {
    "kid": "uc-active-2025",
    "alg": "RS256",
    "use": "sig",
    "status": "ACTIVE",
    "hasPrivate": true,
    "issuedAt": 1746288000000,
    "signing": true
  },
  {
    "kid": "uc-pending-2026",
    "alg": "RS256",
    "use": "sig",
    "status": "PENDING",
    "hasPrivate": true,
    "issuedAt": 1777824000000,
    "signing": false
  }
]
```

> 本接口**不返回**任何私钥材料. `hasPrivate=true` 仅标记服务端密钥仓库中存在该条目的私钥. 列表无分页: JWK 集合通常很小 (每种算法几把密钥), 全量返回即可.
