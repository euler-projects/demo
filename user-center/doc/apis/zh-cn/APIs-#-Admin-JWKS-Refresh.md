# 刷新密钥状态

重新读取底层密钥仓库 (如 `keys.yaml`) 并重建管理状态. 当密钥仓库文件被外部更新后, 通过本接口使运行时状态生效. 通用约定见 [home.md](home.md).

## Request

### Url

```http
POST /admin/oauth2/jwks/refresh
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 请求体

无请求体.

## Response

**Success Response (204):**

无响应体. 本端点只触发一次本地 reload, 不等待集群其它节点收敛; 客户端如需观察集群状态可调用 [查看集群状态](APIs-%23-Admin-JWKS-Cluster-Get.md).

> 单节点 (Standalone) 部署时本端点直接等价于本地 reload. 集群 (Clustered) 部署下, 各节点基于事件和心跳机制独立地重新加载密钥快照, 本接口仅负责通知本节点立即 reload, 不保证其它节点的同步完成时机.
