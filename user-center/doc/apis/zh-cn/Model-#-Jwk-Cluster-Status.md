# JWK Cluster Status

集群部署 (Redis-backed) 下由 [查询集群状态](APIs-%23-Admin-JWKS-Cluster-Get.md) 接口返回的集群级快照. 包含 Redis 中的全局版本号以及所有已注册节点的心跳摘要.

> 单节点 (InMemory) 部署时, 接口返回一个合成的单节点状态以保持结构一致.

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
    },
    {
      "nodeId": "web-2-b4e1d7",
      "missing": false,
      "version": 11,
      "lag": 1,
      "signingKid": "uc-active-2025",
      "publishedKids": ["uc-active-2025"],
      "updatedAt": 1746288045000,
      "host": "web-2",
      "pid": "108"
    }
  ]
}
```

## ClusterStatus

| PROPERTY | TYPE            | DESCRIPTION                                                       |
|----------|-----------------|-------------------------------------------------------------------|
| version  | number          | Redis `{ns}:state` 中记录的集群级版本号                            |
| nodes    | array\<object\> | 各节点心跳摘要, 结构见下方 [NodeStatus](#nodestatus); 按 `nodeId` 排序 |

## NodeStatus

| PROPERTY      | TYPE            | DESCRIPTION                                                                                       |
|---------------|-----------------|---------------------------------------------------------------------------------------------------|
| nodeId        | string          | 节点唯一标识, 启动时自动生成 (`host-pid-random`) 或通过配置指定                                    |
| missing       | boolean         | 心跳哈希已过期 (TTL 失效), 表示该节点可能已离线                                                    |
| version       | number          | 该节点上次心跳时观测到的版本号; `missing=true` 时为 `-1`                                           |
| lag           | number          | `clusterVersion - nodeVersion`; `0` 表示与集群同步; 正值表示落后                                    |
| signingKid    | string          | 该节点当前使用的签名密钥 kid; 未初始化时为 `null`                                                   |
| publishedKids | array\<string\> | 该节点已发布的 kid 列表 (签名密钥排在首位)                                                          |
| updatedAt     | timestamp(3)    | 最近一次心跳时间戳 (毫秒); `missing=true` 时为 `null`                                              |
| host          | string          | 节点启动时的主机名                                                                                 |
| pid           | string          | 节点启动时的进程 ID                                                                                |

> `lag > 0` 的节点仍在使用旧版本状态签发令牌. 可通过 [刷新指定节点](APIs-%23-Admin-JWKS-Cluster-Node-Refresh.md) 触发该节点重新拉取 Redis 状态.
