# 获取当前用户的指定用户认证因素

## 请求

```http
GET /user/identities/{factor_id}
Authorization: Bearer <access_token>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `factor_id` | string | **用户认证因素 ID** (UUID) |

## 响应

```json
{
  "factor_id": "550e8400-e29b-41d4-a716-446655440000",
  "factor_type": "<factor_type>",
  "identifier": "<用户认证因素的唯一标识>",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "...": "扩展字段"
}
```

字段定义详见 [User Authentication Factor](Model-%23-User-Authentication-Factor.md).
