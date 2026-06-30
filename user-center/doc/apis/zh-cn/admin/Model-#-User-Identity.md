# User Identity (管理端视图)

管理端完整模型, 包含服务端只读字段. 用户侧视图见 [User Identity](../Model-%23-User-Identity.md).

> User Identity 是 User 的子资源, 所有管理端接口通过 `/admin/api/users/{userId}/identities/...` 访问; 响应中仍包含 `userId` 以方便引用.

## 1. 模型定义

```json
{
  "identityId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identityType": "phone",
  "subject": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
  "boundAt": 1778899139687,
  "phone": "+8613*******00"
}
```

| 字段 | 类型 | 只读 | 含义 |
|---|---|---|---|
| `identityId` | string | 是 | **登录身份 ID**<br>由服务端生成的 UUID, 可唯一定位到一个用户的一个具体登录身份. |
| `userId` | string | 是 | **所属用户 ID**<br>取自 URL 路径 `{userId}`, 响应中回显. |
| `identityType` | string | 否 | **登录身份类型**<br>取值不限, 可任意扩展; 同一账号下可重复 (如绑定两个手机号). |
| `subject` | string | 是 | **登录身份的确定性唯一标识**<br>由服务端根据 `identityType` 与扩展字段自动计算, 在 `(identityType, subject)` 维度全局唯一. 调用方无需传入. |
| `boundAt` | timestamp(3) | 是 | **首次绑定时间**<br>毫秒级 Unix 时间戳, 由服务端在创建时自动生成. |
| *扩展字段* | — | — | 由 `identityType` 实现方自行定义, 与通用字段平铺在同一层级, 详见下方各类型示例. |

> **只读字段**: `identityId` 由服务端分配, `userId` 取自 URL 路径, `subject` 由服务端计算, `boundAt` 由服务端在创建时自动生成. 请求体中出现这些字段会被忽略.

---

## 2. `identityType` 扩展字段

### 2.1 手机号 `phone`

```json
{
  "identityId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identityType": "phone",
  "subject": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
  "boundAt": 1778899139687,
  "phone": "+8613*******00"
}
```

| 字段 | 类型 | 脱敏 | 含义 |
|---|---|---|---|
| `phone` | string | 是 | **手机号**<br>响应中以脱敏形式返回 (E.164 风格); 创建/更新时需传入原始手机号, 服务端持久化原文并自动计算 `subject`, 响应只返回脱敏值. |

### 2.2 邮箱 `email`

```json
{
  "identityId": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identityType": "email",
  "subject": "3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b",
  "boundAt": 1778899139687,
  "email": "u**r@e*****e.com"
}
```

| 字段 | 类型 | 脱敏 | 含义 |
|---|---|---|---|
| `email` | string | 是 | **邮箱地址**<br>响应中以脱敏形式返回; 创建/更新时需传入原始邮箱, 服务端持久化原文并自动计算 `subject`, 响应只返回脱敏值. |

### 2.3 微信开放平台 `wechat`

```json
{
  "identityId": "550e8400-e29b-41d4-a716-446655440000",
  "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
  "identityType": "wechat",
  "subject": "oX1a2b3c4d5e6f",
  "boundAt": 1778899139687,
  "openid": "oX1a2b3c4d5e6f",
  "nickname": "微信原始昵称",
  "headimgurl": "https://wx.qlogo.cn/mmopen/xxx/132",
  "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
}
```

| 字段 | 类型 | 脱敏 | 含义 |
|---|---|---|---|
| `openid` | string | 否 | **微信 openid** |
| `nickname` | string | 否 | 微信原始昵称 |
| `headimgurl` | string | 否 | 微信原始头像 URL |
| `unionid` | string | 否 | 微信 unionid |

---

## 3. 脱敏与原文查询

管理端所有查询/列表接口默认对敏感字段 (如 `phone` / `email`) 进行脱敏. 如需获取原文, 调用 [查询脱敏字段原文](APIs-%23-Admin-User-Identity-Get-Raw-Field.md).
