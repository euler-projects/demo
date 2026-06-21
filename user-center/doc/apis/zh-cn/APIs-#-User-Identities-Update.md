# 更新已绑定的登录身份

更新当前 `access_token` 归属账号下指定 `identity_id` 的登录身份. 仅可在同类型身份内重新绑定(例如将已绑定的手机号更换为另一手机号), 不可变更 `identity_type`.

## 1. 请求

```http
PUT /user/identities/{identity_id}
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

...=<不同登录身份的定制参数>
```


| 参数 | 类型 | 含义 |
|---|---|---|
| `identity_id` | string (path) | **登录身份 ID** (UUID), 必须为当前用户已绑定的身份 |
| ... | ... | **不同登录身份的定制参数**<br>与 `POST /user/identities` 相同(不含 `identity_type`), 根据具体的身份类型决定 |

> **注意**: 请求体中**不需要**传 `identity_type`, 服务端根据已有记录自动识别身份类型. 若传入 `identity_type` 参数将被忽略.

## 2. 响应

```json
{
  "identity_id": "550e8400-e29b-41d4-a716-446655440000",
  "identity_type": "<identity_type>",
  "subject": "<更新后登录身份的唯一标识>",
  "bound_at": 1778899139687,
  "...": "扩展字段"
}
```

字段定义详见 [User Identity](Model-%23-User-Identity.md).

## 3. 错误码

| HTTP 状态码 | `error` | 含义 |
|---|---|---|
| 400 | `invalid_request` | 缺少必要参数或参数无效 |
| 401 | `invalid_token` | 无有效的 `access_token` |
| 404 | `not_found` | `identity_id` 不存在或不属于当前用户 |
| 409 | `identity_occupied` | 新的身份标识已被其他账号占用 |

## 4. `identity_type` 完整请求举例

### 4.1 微信开放平台登录 `wechat`

客户端先通过微信 Open SDK (`WXApi.sendReq`) 取得授权临时票据 `code`, 再上行本接口.

```http
PUT /user/identities/{identity_id}
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

code={wxCode}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `code` | string | **微信授权临时票据**<br>微信开放平台下发, 单次使用, 由客户端通过 `WXApi.sendReq` 取得 |

### 4.2 手机号 + OTP 登录 `phone`

客户端先调 [`POST /otp/tickets`] (`channel=sms`) 触发下发, 用户输入验证码后上行本接口.

```http
PUT /user/identities/{identity_id}
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

otp_ticket={otp_ticket}
&otp={用户输入的验证码}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `otp_ticket` | string | **OTP 会话句柄**<br>调用 `POST /otp/tickets` 取得, 单次使用; 已隐含目标手机号与下发通道 |
| `otp` | string | **用户输入的短信验证码** |

### 4.3 邮箱 + OTP 登录 `email`

客户端先调 [`POST /otp/tickets`] (`channel=email`) 触发下发, 用户输入验证码后上行本接口.

```http
PUT /user/identities/{identity_id}
Authorization: Bearer <access_token>
Content-Type: application/x-www-form-urlencoded

otp_ticket={otp_ticket}
&otp={用户输入的验证码}
```

| 参数 | 类型 | 含义 |
|---|---|---|
| `otp_ticket` | string | **OTP 会话句柄**<br>调用 `POST /otp/tickets` 取得, 单次使用; 已隐含目标邮箱与下发通道 |
| `otp` | string | **用户输入的邮件验证码** |

[`POST /otp/tickets`]: App-Attest-Login-%23-OTP.md#二-发送-otp-post-otptickets
