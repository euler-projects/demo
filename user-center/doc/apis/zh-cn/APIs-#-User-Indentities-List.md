# 获取指定用户的全部登录因素

## 请求

```http
GET /user/identities
Authorization: Bearer <access_token>
```

## 响应

```json
[
  {
    "id": "idp_7h8j9k0l...",
    "factor_type": "<factor_type>",
    "identifier": "<登录因素的唯一标识>",
    "bound_at": 1778899139687,
    "last_verified_at": 1778899139687,
    "...": "扩展字段"
  }
]
```

字段定义详见 [用户登录因素](Model-%23-User-Identity.md).
