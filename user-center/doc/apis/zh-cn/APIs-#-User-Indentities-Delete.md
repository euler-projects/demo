# 删除用户登录因素

## 请求

```http
DELETE /user/identities/{identity_id}
Authorization: Bearer <access_token>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `identity_id` | string | **登录因素 ID**, 见 [User Identity # 公共字段](Model-%23-User-Identity.md#公共字段) |

## 响应

`204 No Content`, 响应体为空.
