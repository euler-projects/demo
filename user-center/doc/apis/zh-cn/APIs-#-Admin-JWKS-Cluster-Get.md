# 查询集群状态

查询集群级 JWK 管理状态, 包括全局版本号和各节点心跳快照. 通用约定见 [home.md](home.md), 响应体结构见 [JWK Cluster Status](Model-%23-Jwk-Cluster-Status.md).

## Request

### Url

```http
GET /admin/oauth2/jwks/cluster
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

## Response

**Success Response (200):**

响应体为完整的 [JWK Cluster Status](Model-%23-Jwk-Cluster-Status.md) 模型.

```json
{
  "version": 12,
  "nodes": [
    {
      "nodeId": "web-1-a8f3c2",
      "missing": false,
      "version": 12,
      "lag": 0,
      "signingKid": "uc-active-2025",
      "publishedKids": ["uc-active-2025", "uc-pending-2026"],
      "updatedAt": 1746288060000,
      "host": "web-1",
      "pid": "42"
    }
  ]
}
```

> 单节点 (InMemory) 部署时返回一个合成的单节点视图 (`nodeId="local"`, `lag=0`, `missing=false`), 始终处于同步状态.
