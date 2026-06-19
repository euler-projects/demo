# 变更密钥状态 (PATCH)

以部分更新语义变更指定 `kid` 的生命周期状态. `status` 为 `null` 时视为 no-op. 通用约定见 [home.md](home.md).

> 本接口覆盖所有状态流转场景, 取代了原先的独立端点:
> - **提升为签名密钥**: `{"status": "ACTIVE"}`
> - **弃用**: `{"status": "DEPRECATED"}`
> - **退役**: `{"status": "RETIRED"}`

## Request

### Url

```http
PATCH /admin/api/oauth2/jwks/{kid}
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
| status   | enum | No       | 目标生命周期状态, 取值 `PENDING` / `ACTIVE` / `DEPRECATED` / `VERIFY_ONLY` / `RETIRED`; `null` 时不做任何变更 |

### 请求示例

#### 示例 1: 提升为签名候选 (promote)

```json
{
  "status": "ACTIVE"
}
```

#### 示例 2: 弃用 (deprecate)

```json
{
  "status": "DEPRECATED"
}
```

#### 示例 3: 退役 (retire)

```json
{
  "status": "RETIRED"
}
```

## Response

**Success Response (204 No Content):**

无响应体. 变更完成后异步通知各节点重新加载密钥快照.

**Error Response:**

| 状态码 | 说明                                                                 |
|--------|----------------------------------------------------------------------|
| 400    | 变更后违反跨条目约束 (如同算法多个 ACTIVE、ACTIVE 无私钥)            |
| 404    | 目标 `kid` 不存在                                                    |

> 零中断轮换推荐流程: [创建密钥](APIs-%23-Admin-JWKS-Create.md) (`PENDING`) → 等待验证方缓存预热 → 本接口 `{"status": "ACTIVE"}` 提升 → 旧密钥本接口 `{"status": "DEPRECATED"}` → 审计期结束后 `{"status": "RETIRED"}` → [删除密钥](APIs-%23-Admin-JWKS-Delete.md) 物理清除.
