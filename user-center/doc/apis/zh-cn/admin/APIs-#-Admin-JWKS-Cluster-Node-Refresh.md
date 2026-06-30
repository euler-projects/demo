# 刷新指定节点

向集群中指定节点发送一条定向 `refresh` 事件, 促使其立即从 Redis 拉取最新 JWK 状态. 仅在 Redis-backed 集群部署下有效; 单节点 (InMemory) 部署时为 no-op. 通用约定见 [home.md](home.md).

## Request

### Url

```http
POST /admin/api/oauth2/jwks/cluster/{nodeId}/refresh
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

| 参数名 | 类型   | 说明                                                                            |
|--------|--------|---------------------------------------------------------------------------------|
| nodeId | string | 目标节点标识, 可从 [查询集群状态](APIs-%23-Admin-JWKS-Cluster-Get.md) 接口获取  |

### 请求体

无请求体.

## Response

**Success Response (204 No Content):**

无响应体. 事件已发送到 Redis pub/sub 通道; 目标节点在下次消费时将触发重载.

> 本接口为异步操作 — 返回 `204` 仅表示 pub/sub 消息已发送, 不保证目标节点已完成重载. 可通过 [查询集群状态](APIs-%23-Admin-JWKS-Cluster-Get.md) 确认目标节点的 `lag` 是否归零.
