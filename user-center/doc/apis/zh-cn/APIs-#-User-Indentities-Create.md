# 为用户绑定一个新的登录因素

为当前 `access_token` 归属的账号追加一条登录因素绑定记录. 同一账号下 `factor_type` 可以重复(例如同时绑定两个手机号).

## 1. 请求

```http
POST /user/identities
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

factor_type=<factor_type>
&...=<不同登录因素的定制参数>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `factor_type` | string | **登录因素类型**<br>例如: `phone`, `email`, `google` |
| ... | ... | **不同登录因素的定制参数**<br>根据具体的因素类型决定 |

## 2. 响应

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

## 3. `factor_type` 完整请求举例

### 3.1 微信开放平台登录 `wechat`

客户端先通过微信 Open SDK (`WXApi.sendReq`) 取得授权临时票据 `code`, 再上行本接口.

```http
POST /user/identities
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

factor_type=wechat
&code={wxCode}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `code` | string | **微信授权临时票据**<br>微信开放平台下发, 单次使用, 由客户端通过 `WXApi.sendReq` 取得 |

### 3.2 手机号 + OTP 登录 `phone`

客户端先调 [`POST /otp/tickets`] (`channel=sms`) 触发下发, 用户输入验证码后上行本接口.

```http
POST /user/identities
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

factor_type=phone
&otp_ticket={otp_ticket}
&otp={用户输入的验证码}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `otp_ticket` | string | **OTP 会话句柄**<br>调用 `POST /otp/tickets` 取得, 单次使用; 已隐含目标手机号与下发通道 |
| `otp` | string | **用户输入的短信验证码** |

### 3.3 邮箱 + OTP 登录 `email`

客户端先调 [`POST /otp/tickets`] (`channel=email`) 触发下发, 用户输入验证码后上行本接口.

```http
POST /user/identities
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

factor_type=email
&otp_ticket={otp_ticket}
&otp={用户输入的验证码}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `otp_ticket` | string | **OTP 会话句柄**<br>调用 `POST /otp/tickets` 取得, 单次使用; 已隐含目标邮箱与下发通道 |
| `otp` | string | **用户输入的邮件验证码** |

[`POST /otp/tickets`]: App-Attest-Login-%23-OTP.md#二-发送-otp-post-otptickets.md
