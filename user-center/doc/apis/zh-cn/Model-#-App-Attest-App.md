# App Attest App

Admin App Attest 应用接口的统一数据模型. 创建 / 查询 / 列出 / 更新接口共用本结构, 其中**请求体是本模型的子集** — 只接受 `REQ PARAM` 列包含 `C` / `U` 的字段, 其余均为服务端独占或派生, 在请求体中出现会被忽略.

```json
{
  "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
  "appId": "ABCD1234EF.com.example.app",
  "teamId": "ABCD1234EF",
  "bundleId": "com.example.app",
  "oauth2Enabled": true,
  "oauth2ClientType": "STATIC"
}
```

| PROPERTY         | TYPE    | REQ PARAM           | READONLY | DESCRIPTION                                                                                                                                                                                                 |
|------------------|---------|---------------------|----------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| registrationId   | string  | `-` `R` `U` `D` `-` | Yes      | 应用内部主键; 通过 Admin API 创建时由服务端分配 UUID, 仅通过 `euler.security.app-attest.apps` 预配置时才可用人类可读的 Map key 作为 `registrationId` (与 Map key 语义对齐); 对标 OAuth2 侧 `RegisteredClient.id` |
| appId            | string  | `-` `-` `-` `-` `-` | Yes      | 对外应用标识, 派生自 `teamId + "." + bundleId`; 对标 OAuth2 侧 `clientId`; 以原值持久化用于唯一索引查询                                                                                       |
| teamId           | string  | `C` `-` `U` `-` `-` | No       | Apple Developer Team ID, Apple App Attest 场景下为 10 位字母数字                                                                                                                                    |
| bundleId         | string  | `C` `-` `U` `-` `-` | No       | 应用的 Bundle Identifier, 例如 `com.example.app`                                                                                                                                                      |
| oauth2Enabled    | boolean | `C` `-` `U` `-` `-` | No       | 是否启用 OAuth2 客户端联动注册; 默认 `false`                                                                                                                                                  |
| oauth2ClientType | enum    | `C` `-` `U` `-` `-` | No       | OAuth2 客户端注册策略, 可选值见下方[枚举值对照表](#oauth2clienttype-枚举值); `oauth2Enabled=true` 时必填                                                                                            |

> 协议层指纹 (App Attest RP ID hash = `SHA-256(appId)`) 不在模型中暴露, 由服务端在需要时按 `appId` 派生; 持久化层以 hex 字符串形式落到唯一索引列并用于协议查询路径.

---

## `oauth2ClientType` 枚举值

| VALUE     | DESCRIPTION                                                                                                                                    |
|-----------|------------------------------------------------------------------------------------------------------------------------------------------------|
| `STATIC`  | 静态预置客户端, 同一 App 的所有设备共享一个客户端. `clientId` 按固定约定从 `appId` 派生 (`base64url(SHA-256(appId))`), 在应用保存 / 启动时按 provision-if-absent 入库 (仅当 `clientId` 不存在时创建, 不覆盖管理员改动). 兼容"仅凭 assertion 续期 + JIT 匿名用户"(过渡期, 后续可下掉), 默认不签发 `refresh_token` |
| `DYNAMIC` | 每个设备 KEY 独享一个客户端, 与用户解耦. 两步式: 先 `POST /app_attest/register` (attestation, 单次) 注册设备 KEY, 再按 [RFC 7591][RFC-7591] 携带 assertion 请求[动态注册端点](OAuth2-Client-Registration-%23-App-Attest-Dynamic.md), 服务端生成随机 `clientId` 并回绑到该 KEY. Token 签发须凭其他用户因素 (如 OTP), 续期凭 `refresh_token` + assertion |

[RFC-7591]: https://datatracker.ietf.org/doc/html/rfc7591
