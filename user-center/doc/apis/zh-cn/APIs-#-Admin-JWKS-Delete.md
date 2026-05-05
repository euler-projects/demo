# 删除密钥

将指定 `kid` 从底层密钥仓库中**物理删除**, 不可恢复. 仅允许删除 `RETIRED` 状态的密钥, 防止误删仍在使用的签名材料. 通用约定见 [home.md](home.md).

> 删除流程: 先通过 [变更密钥状态 (PATCH)](APIs-%23-Admin-JWKS-Patch.md) 将目标密钥置为 `RETIRED`, 待审计周期结束后再调用本接口物理清除. 一旦物理删除完成, 该 `kid` 不可再被任何节点用于历史令牌验证.

## Request

### Url

```http
DELETE /admin/oauth2/jwks/{kid}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

| 参数名 | 类型   | 说明                                       |
|--------|--------|--------------------------------------------|
| kid    | string | 目标密钥标识 (Key ID), 必须已处于 `RETIRED` |

## Response

**Success Response (204 No Content):**

无响应体. 物理删除成功后异步通知各节点重新加载密钥快照, 客户端如需观察集群收敛状态可调用 [查询集群状态](APIs-%23-Admin-JWKS-Cluster-Get.md).

**Error Response:**

| 状态码 | 说明                                                      |
|--------|-----------------------------------------------------------|
| 400    | 目标 `kid` 存在但状态非 `RETIRED`, 需先通过 PATCH 退役    |
| 404    | 目标 `kid` 不存在                                         |
