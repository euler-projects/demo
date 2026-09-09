# 登录接口(浏览器会话)

本文档是**浏览器会话登录**的总览: 约定、CSRF 前置、dispatch 调用模式、统一错误码。各登录方式的直连契约见子文档:

| 登录方式 | 子文档 |
|---|---|
| 密码 | [密码登录](APIs-%23-Login-Password.md) |
| OTP(短信/邮箱验证码) | [OTP 登录](APIs-%23-Login-OTP.md) |
| 第三方(Google 等) | [OAuth2 登录](APIs-%23-Login-OAuth2.md) |

所有接口建立的是**基于 Session Cookie 的浏览器会话**, 供纯前端 SPA 以同源 XHR/fetch 调用(OAuth2 除外, 其为整页跳转)。渲染登录页前应先调用[登录方式列表](APIs-%23-Login-Methods.md)获取本部署提供哪些登录方式。

> 文中路径为本部署的取值; 服务端可配置改写, 以实际环境为准。

---

## 一. 通用约定

| 约定 | 说明 |
|---|---|
| 同源 | 全部接口仅同源可调; 不注册任何 CORS |
| 会话 | 登录成功后由 `Set-Cookie: SESSION` 建立会话, 后续同源请求自动携带 |
| JSON 协商 | 请求头**必须**显式携带 `Accept: application/json` 才返回 JSON; 否则按传统表单行为返回 `302` 重定向(`fetch` 默认的 `*/*` **不会**触发 JSON) |
| 成功信封 | `200` + `{"redirect_url": "<登录后应跳转的地址>"}`, 前端自行 `window.location.assign(redirect_url)` |
| 失败信封 | 真实 HTTP 状态 + `{"error": "<机器可读错误码>", "timestamp": <epoch 毫秒>}`; 不含 message, 前端按 `error` 码本地化文案 |
| CSRF | 见第二节; 所有状态变更类登录请求都必须携带 |

---

## 二. 获取 CSRF token

```http
GET /_csrf
Accept: application/json
```

响应 `200`:

```json
{
  "headerName": "X-XSRF-TOKEN",
  "parameterName": "_csrf",
  "token": "8f2a...-...-...-...-...c1d3"
}
```

- 匿名可调, 同源校验, 响应禁缓存;
- `XSRF-TOKEN` Cookie 为 HttpOnly, JS 读不到, **只能**经本端点取得;
- 后续请求以 `X-XSRF-TOKEN: <token>` 请求头回传(或以 `parameterName` 表单域回传);
- token 失效时状态变更请求返回 `403 {"error":"invalid_csrf_token"}`: 重新取 token 重试即可。

---

## 三. dispatch 调用模式(统一提交入口)

除各子文档的**直连**模式(每种方式 POST 到自己的处理端点)外, 服务端另提供一个**统一提交入口**: 任何方式的字段都可以提交到同一地址, 由服务端依据方式名与字段完整性决定后续重定向。

```http
POST /login?_m=<方式名>
Content-Type: application/x-www-form-urlencoded
Accept: application/json
X-XSRF-TOKEN: <csrf_token>

<该方式收集的字段, 如 password 方式的 username & password>
```

- 方式名为[登录方式列表](APIs-%23-Login-Methods.md)中的 `name`; 入口路径与参数名 `_m` 服务端可配置, 本部署取值如上;
- 同样受 CSRF 保护;
- 响应恒为"重定向", 或其 JSON 等价物(携带 `Accept: application/json` 时):

| 场景 | 表单客户端 | JSON 客户端 |
|---|---|---|
| 字段已**完整** | `307 Temporary Redirect`: 将本 POST 原样重放(方法与 body 不变)至该方式的处理端点 | 同左, `307` **不变形**: fetch 自动跟随并重发 body, 最终响应为该端点的 JSON 信封 |
| 字段**不完整**(如仅选择了方式) | `302 Found` 至该方式的收集页(带 `?_m=<方式名>` 的登录页) | `200` + `{"redirect_url": "<收集页>"}`, 前端自行导航 |
| 方式名未知 | `302` 回登录页并带 `?error` | `400` + `{"error": "invalid_request", "timestamp": ...}` |

> `307` 不变形的原因: fetch 跟随 307 时会自动重发 body, 终端处理端点的 JSON 信封原样到手; 若变形为 `200 + redirect_url`, 反而需要前端手动重发 body。

- **SPA 不强制使用 dispatch**: 直连各处理端点(见子文档)语义更可控; dispatch 主要服务"多方式共用一个 form action"的传统表单页。

---

## 四. 错误码表

| HTTP | `error` | 触发场景 |
|---|---|---|
| `401` | `authentication_failed` | 认证失败: 凭据错误、验证码错误或过期、密码过期、用户不存在等**统一模糊**为此码, 服务端不提供专属凭据错误码; 前端按表单语境提示("用户名或密码错误" / "验证码错误或过期") |
| `403` | `account_locked` | 账号被锁定 |
| `403` | `account_disabled` | 账号被禁用 |
| `403` | `account_expired` | 账号已过期 |
| `403` | `invalid_csrf_token` | CSRF token 缺失/失效 —— 重新取 token 后重试 |
| `403` | `access_denied` | 其它访问拒绝(越权等) |
| `500` | `server_error` | 服务端内部错误(**非**凭证问题, 不应提示"密码错误"或无限重试) |
| `401` | `mfa_required` | *(预留)* 需二次验证 |
| `429` | `too_many_requests` | *(预留)* 登录限流 |

---

## 五. 相关文档

- [登录方式列表](APIs-%23-Login-Methods.md) — 登录页应渲染哪些登录方式
- [App Attest 登录 - OTP 接入细节](App-Attest-Login-%23-OTP.md) — `POST /otp/tickets` 的完整参数与响应
