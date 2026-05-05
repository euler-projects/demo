# JWK Key

Admin JWK 管理接口返回的密钥视图模型. 本模型是 JWK 条目的安全投影 — **严禁返回任何私钥材料**, 仅通过 `hasPrivate` 标记是否持有私钥, 以便 UI 标注可用于 `signWith` 提升的条目.

```json
{
  "kid": "uc-active-2025",
  "alg": "RS256",
  "use": "sig",
  "status": "ACTIVE",
  "hasPrivate": true,
  "issuedAt": 1746288000000,
  "signing": true
}
```

| PROPERTY   | TYPE           | READONLY | DESCRIPTION                                                                       |
|------------|----------------|----------|-----------------------------------------------------------------------------------|
| kid        | string         | Yes      | 密钥标识 (Key ID), 与 JOSE Header `kid` 对应                                      |
| alg        | string         | Yes      | JWA 算法名称, 如 `RS256` / `ES256` / `ES384` / `ES512` / `EdDSA`                  |
| use        | string         | Yes      | JWK 用途参数, 通常为 `sig` (签名)                                                  |
| status     | enum           | Yes      | 密钥生命周期状态, 可选值见下方 [枚举值对照表](#status-枚举值)                       |
| hasPrivate | boolean        | Yes      | 密钥仓库中是否持有该条目的私钥; `true` 表示可用于签名 (配合 `ACTIVE` 或 `PENDING`) |
| issuedAt   | timestamp(3)   | Yes      | 密钥签发时间戳 (毫秒), 对应 manifest 中的 `iat` 字段                               |
| signing    | boolean        | Yes      | 当前是否被选为签名密钥; 同一时刻只有一个密钥 `signing=true`                        |

> 本模型为**纯只读**, 不存在请求体场景. 通过 [列出密钥](APIs-%23-Admin-JWKS-Get.md) 接口返回, 也作为 [创建密钥](APIs-%23-Admin-JWKS-Create.md) 的成功响应体. 创建密钥的请求体见 [JWK 密钥创建请求](Model-%23-Jwk-Key-Create-Request.md).

---

## `status` 枚举值

| VALUE      | DESCRIPTION                                                                                           |
|------------|-------------------------------------------------------------------------------------------------------|
| `PENDING`  | 已发布至 JWK Set 供验证方预热缓存, 但尚未用于签名; 可通过 `signWith` 提升为签名密钥                    |
| `ACTIVE`   | 候选签名密钥池; 实际签名密钥从此子集中选取                                                             |
| `ROTATING` | 轮换过渡期: 仍在已发布 JWK Set 中以验证历史令牌, 但不再用于新签名                                      |
| `RETIRED`  | 已归档: 保留在仓库中用于审计, 从已发布 JWK Set 中移除                                                  |
