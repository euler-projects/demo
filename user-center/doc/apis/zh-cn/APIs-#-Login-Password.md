# 密码登录

```http
POST /doLogin
Content-Type: application/x-www-form-urlencoded
Accept: application/json
X-XSRF-TOKEN: <csrf_token>

username=<username>
&password=<password>
&redirect_url=<登录后跳转地址>
```

通用约定(JSON 协商、成功/失败信封)、CSRF token 取法与错误码表见[登录接口(浏览器会话)](APIs-%23-Login.md)。

---

## 请求参数

| 参数 | 必选 | 说明 |
|---|---|---|
| `username` | 是 | 用户名 |
| `password` | 是 | 密码 |
| `redirect_url` | 否 | 登录成功后跳转的地址; 缺省为 `/`。被登录拦截的深链接会以此参数带回登录页, 原样回传即可还原跳转 |

---

## 响应

- 成功 `200` + `{"redirect_url":"..."}`, 前端自行 `window.location.assign(redirect_url)`;
- 失败按真实状态 + 错误信封: 凭据错误 `401 authentication_failed`(模糊码, 不区分密码错/用户不存在; 本表单提示"用户名或密码错误"), 账号锁定 `403 account_locked` 等, 见父文档错误码表。

---

## 参考调用

```js
const res = await fetch('/doLogin', {
  method: 'POST',
  headers: {
    'Content-Type': 'application/x-www-form-urlencoded',
    'Accept': 'application/json',
    'X-XSRF-TOKEN': csrfToken
  },
  body: new URLSearchParams({ username, password, redirect_url: '/admin/console' })
});
if (res.ok) {
  const { redirect_url } = await res.json();
  window.location.assign(redirect_url);
} else {
  const { error } = await res.json();
  if (error === 'invalid_csrf_token') {
    await refreshCsrfTokenAndRetry();
  } else {
    showLoginError(localize(error));
  }
}
```

---

## 相关文档

- [登录方式列表](APIs-%23-Login-Methods.md) — 本方式对应 `type=password` 的条目
