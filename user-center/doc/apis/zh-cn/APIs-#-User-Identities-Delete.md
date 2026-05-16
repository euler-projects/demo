# 删除用户登录因素

## 请求

```http
DELETE /user/identities/{id}
Authorization: Bearer <access_token>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `id` | string | **登录因素 ID** |

## 响应

`204 No Content`, 响应体为空.
