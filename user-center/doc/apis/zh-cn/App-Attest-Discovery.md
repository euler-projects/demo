# Apple App Attest 服务发现

App Attest 的 challenge 与注册端点**不属于 OAuth**, 因此不出现在 OIDC Discovery (`.well-known/openid-configuration`) 元数据中, 而是由一份**独立的 well-known 发现文档**描述:

```
GET /.well-known/app-attest-configuration
```

客户端在发起 [App 实例注册](App-Attest-Registration.md) 前, 先从本文档发现实际端点地址, **不要硬编码** `/app_attest/challenge`、`/app_attest/register` 等路径.

---

## 一. 为什么是独立的发现文档

| 发现文档 | 描述的能力 | 规范 |
|---|---|---|
| `/.well-known/openid-configuration` | OAuth 2.0 / OIDC 端点 (token、userinfo、jwks 等) | OpenID Connect Discovery 1.0 / RFC 8414 |
| `/.well-known/app-attest-configuration` | Apple App Attest 的 challenge 与注册端点 (非 OAuth) | 本文档 (RFC 8615 风格 well-known) |

App 实例注册是"App 安装实例凭设备证明完成自身注册"的 enrollment 环节. 在 OAuth attestation 草案中, 客户端实例如何向其证明方 (此处为 Apple) 取证本就属于**范围外**的前置步骤, 草案只规范 OAuth 侧的客户端认证. 把这两个端点塞进 OIDC 元数据会污染那份标准文档, 故独立成档.

两份文档**同源** (同一 issuer / 主机), 客户端用同一个基地址即可分别拉取, 各司其职、互不替代.

---

## 二. 端点

```http
GET /.well-known/app-attest-configuration
```

匿名访问, 无需请求体. 路径固定 —— well-known URI 按定义即固定注册路径, 不可配置 (与 OIDC 的 `/.well-known/openid-configuration` 同理).

### 响应 (200)

```json
{
  "challenge_endpoint": "https://auth.example.com/app_attest/challenge",
  "registration_endpoint": "https://auth.example.com/app_attest/register"
}
```

| 字段 | 类型 | 说明 |
|------|------|------|
| `challenge_endpoint` | string | 一次性 challenge 端点的绝对 URL, 用于 [App 实例注册](App-Attest-Registration.md) |
| `registration_endpoint` | string | App 实例注册端点的绝对 URL |

与 OIDC 发现文档一致, 本端点不设 `Cache-Control`, 响应可被缓存. 端点地址相对稳定, 客户端可缓存复用; 冷启动时重新拉取即可感知变更.

---

## 三. 客户端使用

1. 配置授权服务基地址 (issuer), 与 OIDC Discovery 同一个.
2. 拉取 `{issuer}/.well-known/app-attest-configuration`, 解析出 `challenge_endpoint` 与 `registration_endpoint`.
3. 后续 App 实例注册流程一律使用发现到的绝对 URL.

> OAuth 端点 (token / userinfo / jwks) 仍从 `{issuer}/.well-known/openid-configuration` 发现. 两套发现文档互不替代.

---

## 相关文档

- [Apple App Attest 实例注册](App-Attest-Registration.md) — 发现到端点后的注册流程
- [OAuth2 Client Registration - App Attest DYNAMIC](OAuth2-Client-Registration-%23-App-Attest-Dynamic.md) — 注册 KEY 之后的 OAuth 客户端动态注册
