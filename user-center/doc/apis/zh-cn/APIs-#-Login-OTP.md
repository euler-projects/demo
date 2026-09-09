# OTP 登录(短信 / 邮箱验证码)

两步流程, 均为 JSON 接口。通用约定(JSON 协商、成功/失败信封)与错误码表见[登录接口(浏览器会话)](APIs-%23-Login.md)。

---

## 第一步: 申请下发验证码

```http
POST /otp/tickets
Content-Type: application/x-www-form-urlencoded

channel=sms
&recipient=%2B8613900000000
```

| 参数 | 必选 | 说明 |
|---|---|---|
| `channel` | 是 | 下发通道: `sms` / `email`; 取值见[登录方式列表](APIs-%23-Login-Methods.md)中 otp 方式的 `attributes.channel` |
| `recipient` | 是 | 送达目标(手机号或邮箱) |

- 匿名可调且**豁免 CSRF**;
- 验证码经通道异步送达, 本接口只负责签发会话句柄。

响应 `200`:

```json
{ "otp_ticket": "ot_2b8f4e...", "expires_in": 300, "retry_after": 60 }
```

| 字段 | 说明 |
|---|---|
| `otp_ticket` | 会话句柄, 单次使用, 第二步回传 |
| `expires_in` | 句柄有效期(秒) |
| `retry_after` | 再次申请的最小间隔(秒), 用于倒计时 |

---

## 第二步: 提交验证码完成登录

```http
POST /login/otp
Content-Type: application/x-www-form-urlencoded
Accept: application/json
X-XSRF-TOKEN: <csrf_token>

otp_ticket=<第一步取得的 otp_ticket>
&otp=<用户输入的验证码>
&redirect_url=<登录后跳转地址>
```

| 参数 | 必选 | 说明 |
|---|---|---|
| `otp_ticket` | 是 | 第一步签发的会话句柄 |
| `otp` | 是 | 用户输入的验证码 |
| `redirect_url` | 否 | 同[密码登录](APIs-%23-Login-Password.md) |

响应与密码登录**完全一致**: 成功 `200 {redirect_url}`; 验证码错误或过期 `401 authentication_failed`(模糊码; 本表单提示"验证码错误或过期")。

---

## 相关文档

- [App Attest 登录 - OTP 接入细节](App-Attest-Login-%23-OTP.md) — `POST /otp/tickets` 的完整参数与响应
- [登录方式列表](APIs-%23-Login-Methods.md) — 本方式对应 `type=otp` 的条目
