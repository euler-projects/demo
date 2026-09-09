# 登录方式列表

```http
GET /login-methods
```

匿名可调, 无需任何请求头与 CSRF token。返回本部署**当前提供**的全部登录方式, 供登录页决定渲染哪些表单与按钮。

> 本部署已启用该端点; 若某环境返回 `404`, 表示服务端未启用, 前端应回退到仅渲染密码登录。

---

## 响应

`200` + JSON **裸数组**(无外层包装):

```json
[
  {
    "name": "password",
    "type": "password",
    "primary": true,
    "attributes": {}
  },
  {
    "name": "email",
    "type": "otp",
    "primary": false,
    "attributes": { "channel": "email" }
  },
  {
    "name": "google",
    "type": "oauth2",
    "primary": false,
    "attributes": { "provider": "google" }
  }
]
```

> 示例涵盖全部类型便于说明; 实际条目与顺序以响应为准。

| 字段 | 类型 | 说明 |
|---|---|---|
| `name` | string | 方式的寻址名, 全局唯一 |
| `type` | string | 方式类型: `password` / `otp` / `oauth2` |
| `primary` | boolean | 是否默认展开渲染为表单; 其余渲染为按钮 |
| `attributes` | object | 类型相关的附加信息, 见下表; 无则为 `{}` |

`attributes` 键:

| `type` | 键 | 说明 |
|---|---|---|
| `otp` | `channel` | 下发通道: `sms` / `email`; 申请验证码时作为 `channel` 参数回传 |
| `oauth2` | `provider` | 第三方标识(如 `google`); 整页跳转 `/oauth2/authorization/<provider>` 时使用 |

---

## 各类型如何接入登录流程

| `type` | 前端动作 | 详见 |
|---|---|---|
| `password` | 经 dispatch 入口提交(username + password); 亦可直连 `POST /doLogin` | [密码登录](APIs-%23-Login-Password.md) |
| `otp` | 两步: `POST /otp/tickets`(channel 取自 `attributes.channel`) → 经 dispatch 入口提交(otp_ticket + otp); 亦可直连 `POST /login/otp` | [OTP 登录](APIs-%23-Login-OTP.md) |
| `oauth2` | 向 dispatch 入口提交空表单取得授权 URL 后**整页跳转**(授权往返不可 XHR); 亦可直连 `/oauth2/authorization/<attributes.provider>` | [OAuth2 登录](APIs-%23-Login-OAuth2.md) |

渲染建议: `primary=true` 的条目展开为内联表单, 其余渲染为"使用 X 登录"按钮; 文案按 `type`/`attributes` 本地化, 不要硬编码方式清单。

---

## 错误响应

| HTTP | body | 触发场景 |
|---|---|---|
| `500` | `{"error":"server_error","timestamp":...}` | 服务端装配列表失败; 前端回退仅密码登录, 不要重试轰炸 |
