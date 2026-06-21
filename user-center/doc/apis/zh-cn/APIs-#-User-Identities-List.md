# 获取当前用户的全部登录身份

## 请求

```http
GET /user/identities
Authorization: Bearer <access_token>
```

## 响应

```json
[
  {
    "identity_id": "550e8400-e29b-41d4-a716-446655440000",
    "identity_type": "<identity_type>",
    "subject": "<登录身份的唯一标识>",
    "bound_at": 1778899139687,
    "...": "扩展字段"
  }
]
```

字段定义详见 [User Identity](Model-%23-User-Identity.md).
