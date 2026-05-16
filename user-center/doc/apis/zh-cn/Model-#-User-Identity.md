# User Identity

## 1. 模型定义

```json
{
  "identity_id": "idp_7h8j9k0l...",
  "factor_type": "<factor_type>",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "...": "扩展字段"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `identity_id` | string | **登录因素 ID**<br>由服务端生成, 可唯一定位到一个用户的一个具体登录因素 |
| `factor_type` | string | **登录因素类型**<br>取值不限, 可任意扩展; 同一账号下可重复, 例如绑定两个手机号, 即存在两条 `factor_type = "phone"` 的记录. |
| `bound_at` | timestamp(3) | 首次绑定时间<br>毫秒级 Unix 时间戳 |
| `last_verified_at` | timestamp(3) | **最近一次验证该因素有效性的时间**<br>毫秒级 Unix 时间戳; 例如 OTP 通过、IdP `code` 换取 `access_token` 成功等 |
| ... | ... | **扩展字段**<br>由`factor_type`实现方自行定义 |

---

## 2. `factor_type` **扩展字段**举例

### 2.1 微信开放平台登录 `wechat`

#### 2.1.1 完整数据结构

```json
{
  "identity_id": "idp_7h8j9k0l...",
  "factor_type": "wechat",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "openid": "oX1a2b3c4d5e6f",
  "nickname": "微信原始昵称",
  "headimgurl": "https://wx.qlogo.cn/mmopen/xxx/132",
  "unionid": "o6_bmasdasdsad6_2sgVt7hMZOPfL"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `openid` | string | **微信 `openid`**<br>客户端一般不直接使用, 仅作"已绑定微信"的证据 |
| `nickname` | string | **微信原始昵称**<br>用于"社交账号绑定"管理页展示, 与 `profile.nickname` 独立 |
| `headimgurl` | string | **微信原始头像 URL**<br>用于"社交账号绑定"管理页展示, 与 `profile.avatar_url` 独立 |
| `unionid` | string | **微信 `unionid`**<br>同一开放平台账号下的多个应用, 同一用户取值唯一 |

### 2.2 手机号 + OTP 登录 `phone`


#### 2.2.1 完整数据结构

```json
{
  "identity_id": "idp_7h8j9k0l...",
  "factor_type": "phone",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "recipient": "+8613*******00"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `recipient` | string | **脱敏后的电话号码**<br>(E.164 风格), 原始电话号码仅服务端持久化, 不下发 |

---

### 2.3 邮箱 + OTP 登录 `email`


#### 2.3.1 完整数据结构

```json
{
  "identity_id": "idp_7kbp651...",
  "factor_type": "email",
  "bound_at": 1778899139687,
  "last_verified_at": 1778899139687,
  "recipient": "u**r@e*****e.com"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `recipient` | string | **脱敏后的邮箱地址**<br>原始邮箱地址仅服务端持久化, 不下发 |
