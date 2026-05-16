# 获取当前用户的指定登录因素

## 请求

```http
GET /user/identities/{id}
Authorization: Bearer <access_token>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `id` | string | **登录因素 ID** |

## 响应

```json
{
  "id": "idp_7h8j9k0l...",
  "factor_type": "<factor_type>",
  "identifier": "<登录因素的唯一标识>",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "...": "扩展字段"
}
```

字段定义详见 [用户登录因素](Model-%23-User-Identity.md).
