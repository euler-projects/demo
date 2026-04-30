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
| `STATIC`  | 使用静态预置的 OAuth2 客户端. `clientId` 由 App Attest 侧按固定约定派生 (与 `appId` 对齐), 客户端配置在应用启动前已入库                           |
| `DYNAMIC` | 首次 App Attest 校验通过后, 服务端按 [RFC 7591][RFC-7591] 动态注册一个与当前 App 绑定的 OAuth2 客户端; `clientId` 由服务端生成并回写             |

[RFC-7591]: https://datatracker.ietf.org/doc/html/rfc7591
