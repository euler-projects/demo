# User Identity

## 1. 模型定义

```json
{
  "identity_id": "550e8400-e29b-41d4-a716-446655440000",
  "identity_type": "<identity_type>",
  "subject": "<登录身份的唯一标识>",
  "bound_at": 1778899139687,
  "...": "扩展字段"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `identity_id` | string | **登录身份 ID**<br>由服务端生成的 UUID, 可唯一定位到一个用户的一个具体登录身份. 即使后续换绑(如 `phone` 类型更换手机号), `identity_id` 也保持不变. |
| `identity_type` | string | **登录身份类型**<br>取值不限, 可任意扩展; 同一账号下可重复, 例如绑定两个手机号, 即存在两条 `identity_type = "phone"` 的记录. |
| `subject` | string | **登录身份的确定性唯一标识**<br>不同 `identity_type` 各自定义其派生规则: `phone` / `email` 取标识值的 SHA-256 hex; `wechat` 取微信下发的 `openid`; `apple` / `google` 取 IdP 下发的 `sub` (OIDC subject claim). 在 `(identity_type, subject)` 维度全局唯一. 客户端可作为该身份的稳定标识使用; 换绑后 `subject` 会随之变化. |
| `bound_at` | timestamp(3) | 首次绑定时间<br>毫秒级 Unix 时间戳 |
| ... | ... | **扩展字段**<br>由 `identity_type` 实现方自行定义 |

---

## 2. `identity_type` **扩展字段**举例

### 2.1 微信开放平台登录 `wechat`

#### 2.1.1 完整数据结构

```json
{
  "identity_id": "550e8400-e29b-41d4-a716-446655440000",
  "identity_type": "wechat",
  "subject": "oX1a2b3c4d5e6f",
  "bound_at": 1778899139687,
  "openid": "oX1a2b3c4d5e6f",
  "nickname": "微信原始昵称",
  "headimgurl": "https://wx.qlogo.cn/mmopen/xxx/132",
  "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `subject` | string | **微信确定性唯一标识**<br>取值与 `openid` 相同(微信开放平台下发的不可逆 opaque ID). |
| `openid` | string | **微信 `openid`**<br>同 `subject`, 保留为业务语义字段, 客户端按需使用. |
| `nickname` | string | **微信原始昵称**<br>用于 “社交账号绑定” 管理页展示, 与 `profile.nickname` 独立 |
| `headimgurl` | string | **微信原始头像 URL**<br>用于 “社交账号绑定” 管理页展示, 与 `profile.avatar_url` 独立 |
| `unionid` | string | **微信 `unionid`**<br>同一开放平台账号下的多个应用, 同一用户取值唯一 |

### 2.2 手机号 + OTP 登录 `phone`


#### 2.2.1 完整数据结构

```json
{
  "identity_id": "550e8400-e29b-41d4-a716-446655440000",
  "identity_type": "phone",
  "subject": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
  "bound_at": 1778899139687,
  "phone": "+8613*******00"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `subject` | string | **手机号的 SHA-256 哈希值**<br>由原始手机号(E.164 风格)经 SHA-256 哈希计算得到, 64 位小写 hex, 不可逆; 用于全局唯一性约束与反查. |
| `phone` | string | **脱敏后的手机号**<br>(E.164 风格), 用于管理页展示; 原始手机号仅服务端持久化, 不下发 |

---

### 2.3 邮箱 + OTP 登录 `email`


#### 2.3.1 完整数据结构

```json
{
  "identity_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "identity_type": "email",
  "subject": "3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b",
  "bound_at": 1778899139687,
  "email": "u**r@e*****e.com"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `subject` | string | **邮箱地址的 SHA-256 哈希值**<br>由原始邮箱地址(全小写归一化后)经 SHA-256 哈希计算得到, 64 位小写 hex, 不可逆; 用于全局唯一性约束与反查. |
| `email` | string | **脱敏后的邮箱地址**<br>用于管理页展示; 原始邮箱地址仅服务端持久化, 不下发 |
