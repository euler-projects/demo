# 第三方 OAuth2 登录(如 Google)

OAuth2 的**授权往返**(浏览器 ↔ IdP)无法走 XHR, 必须**整页跳转**; 但**授权发起 URL 的获取**有两种方式:

| 模式 | 做法 |
|---|---|
| dispatch(推荐, 本 SPA 采用) | 向统一提交入口提交**空表单**: `POST /login?_m=<方式名>`(携带 `Accept: application/json` 与 CSRF) → `200 {"redirect_url": "/oauth2/authorization/<registrationId>"}` → 前端 `window.location.assign(redirect_url)` |
| 直连 | 直接整页跳转 `/oauth2/authorization/{registrationId}` |

dispatch 模式的 registrationId 由服务端从注册表解析, 前端无需知晓; 直连模式需前端自行取得(本部署与[登录方式列表](APIs-%23-Login-Methods.md)的 `attributes.provider` 一致)。

```js
// dispatch 成功后的第二跳(或直连模式)恒为整页跳转
window.location.assign(redirectUrl); // 如 /oauth2/authorization/google
```

---

## 流程与回跳

1. 前端整页跳转到授权发起端点(dispatch 返回的 `redirect_url`, 或直连地址);
2. 浏览器被重定向到第三方 IdP(如 `accounts.google.com`)完成交互授权 —— 这正是不能走 XHR 的原因: 跨域跳转与 IdP 登录页都无法在 fetch 内完成;
3. 授权完成后 IdP 整页回跳本站回调地址, 会话在**这次回跳**中建立;
4. 回跳成功后默认落到 `/`, 由前端路由接管。

> 发起跳转时携带的 `redirect_url` **不会**自动透传回跳。若需还原深链接: 发起跳转前自行保存目标地址(如 `sessionStorage`), 回跳落地后再跳转。

---

## 相关文档

- [登录接口(浏览器会话)](APIs-%23-Login.md) — 通用约定与错误码表
- [登录方式列表](APIs-%23-Login-Methods.md) — 本方式对应 `type=oauth2` 的条目
