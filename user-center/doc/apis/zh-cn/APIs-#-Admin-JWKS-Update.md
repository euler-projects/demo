# 更新密钥 (PUT)

以全量覆盖语义更新指定 `kid` 的生命周期状态. 请求体中 `status` 为必填字段, 将完全替换当前持久化状态. 如仅需部分更新, 请使用 [变更密钥状态 (PATCH)](APIs-%23-Admin-JWKS-Patch.md). 通用约定见 [home.md](home.md).

## Request

### Url

```http
PUT /admin/oauth2/jwks/{kid}
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

### Content Type

```
application/json
```

### 请求体字段

| PROPERTY | TYPE | REQUIRED | DESCRIPTION                                                                       |
|----------|------|----------|-----------------------------------------------------------------------------------|
| status   | enum | Yes      | 目标生命周期状态, 取值 `PENDING` / `ACTIVE` / `DEPRECATED` / `VERIFY_ONLY` / `RETIRED` |

### 请求示例

```json
{
  "status": "ACTIVE"
}
```

## Response

**Success Response (204 No Content):**

无响应体. 变更完成后异步通知各节点重新加载密钥快照.

**Error Response:**

| 状态码 | 说明                                                                 |
|--------|----------------------------------------------------------------------|
| 400    | `status` 为空, 或变更后违反跨条目约束 (如同算法多个 ACTIVE)          |
| 404    | 目标 `kid` 不存在                                                    |

> PUT 语义要求 `status` 必传; 若仅确认当前状态不变, 请传入与当前一致的值. 变更 status 时自动触发跨条目校验: 每种算法最多一个 `ACTIVE`, `ACTIVE`/`PENDING` 必须持有私钥.
