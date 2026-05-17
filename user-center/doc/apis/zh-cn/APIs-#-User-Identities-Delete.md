# 删除用户认证因素

## 请求

```http
DELETE /user/identities/{factor_id}
Authorization: Bearer <access_token>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `factor_id` | string | **用户认证因素 ID** (UUID) |

## 响应

`204 No Content`, 响应体为空.
