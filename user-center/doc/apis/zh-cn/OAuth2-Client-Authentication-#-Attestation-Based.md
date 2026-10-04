# OAuth2 Attestation-Based Client Authentication

基于 [OAuth 2.0 Attestation-Based Client Authentication (Draft)] 实现, 认证方式标识为 `attest_jwt_client_auth`.

## 概述

Attestation-Based Client Authentication 允许客户端通过设备证明 (Client Attestation) 和持有证明 (Proof-of-Possession) 向授权服务器证明自身身份. 本实现支持两种使用模式:

1. **独立认证** — `attest_jwt_client_auth` 作为唯一的客户端认证方式, 适用于原生 App 等无法安全存储 client_secret 的场景.
2. **增强认证** — 在标准认证 (如 `client_secret_basic`, `client_secret_post`, `private_key_jwt`, `tls_client_auth`) 基础上叠加设备证明, 为已认证客户端提供额外的安全信号.

## 请求头

| 请求头 | 必需 | 说明 |
|--------|------|------|
| `OAuth-Client-Attestation` | 是 | Client Attestation JWT (草案 §4). 草案 §7.1 第 1 条要求恰好一个; 其 `cnf` claim 内的公钥是验证 PoP 签名的**唯一标准依据**(§5.1 规则 3、§7.2 第 4 条) |
| `OAuth-Client-Attestation-PoP` | 是 | Client Attestation PoP JWT (草案 §5.1), 证明客户端实例持有 `cnf` 公钥对应的私钥. 草案 §7.2 第 1 条要求恰好一个 |
| `OAuth-Client-Attestation-Type` | 否 | **已废弃**, 非草案内容. 仅存量客户端仍在发送并被识别; 新客户端**不应**携带. 详见下文〈PoP 载体类型扩展〉 |

### PoP 载体类型扩展

草案 §5 定义了**两种** PoP 机制: Client Attestation PoP JWT (§5.1) 与 DPoP 合并模式 (§5.2, 认证方式值 `attest_jwt_client_auth_dpop`); **本实现只支持前者**. 草案 §5 同时允许其他规范定义新的 PoP 机制, 并要求其**注册自己的 token 端点认证方式值**(类比 `attest_jwt_client_auth_dpop`), 而非在既有值之下再加子类型开关.

本实现按该路径扩展出 Apple App Attest 变体: 它以 App Attest assertion 替代 PoP JWT, 使用**独立的认证方式值 `attest_appattest_client_auth`**; 变体由请求携带的凭据判定(携 App Attest 凭据 → Apple 变体, 否则 → 本文描述的标准 JWT 变体), 无需任何类型开关头. 其凭据承载、接入流程、废弃用法与错误处理**全部**见 [Apple App Attest 子文档](OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md), 本文不展开.

早期的 `OAuth-Client-Attestation-Type` 头(取值 `jwt` / `apple_app_attest`)**已废弃**, 仅作为识别存量客户端的判别器保留: 它们把 Apple 凭据放在已废弃的通用名表单参数里, 无法由载体识别. `attest_appattest_client_auth` 也**未在 IANA 注册**(注册为 Specification Required, 需公开规范), 仅在本部署内有效.

## PoP JWT 结构

**Header:**

| 参数  | 必需 | 说明                                      |
|-------|------|-------------------------------------------|
| `typ` | 是   | 必须为 `oauth-client-attestation-pop+jwt` |
| `alg` | 是   | 签名算法 (如 `ES256`)                     |

**Claims:**

| 声明        | 必需 | 说明                                                                                                                                                |
|-------------|------|-----------------------------------------------------------------------------------------------------------------------------------------------------|
| `aud`       | 是   | 授权服务器的 Issuer URL                                                                                                                             |
| `iat`       | 是   | 签发时间. 本实现有效窗口 5 分钟, 允许 30 秒时钟偏移                                                                                                 |
| `jti`       | 是   | 唯一标识, 用于防重放                                                                                                                                |
| `challenge` | 是   | 从 Challenge 端点获取的一次性挑战值, 使用后失效. **草案 §5.1 为 OPTIONAL**(§7.2 第 5 条: 若服务端提供 challenge 则必须匹配), 本实现强制要求, 属收紧 |

## 请求示例 (独立认证)

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation: <Client Attestation JWT>
OAuth-Client-Attestation-PoP: <Client Attestation PoP JWT>

grant_type={grant_type}&scope=openid
```

> 两个头**必须同时携带**: 服务端用 `OAuth-Client-Attestation` 里 `cnf` claim 的公钥验证 PoP 签名(草案 §7.2 第 4 条), 并从其 `sub` claim 取得 `client_id`(§7.1 第 7 条). 本实现另支持省略 `OAuth-Client-Attestation` 的单 PoP 模式(需 PoP 头带已注册 `kid`), 属非标准扩展, 见〈与草案的差异〉.

## 请求示例 (增强认证)

```http
POST /oauth2/token
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-PoP: <PoP JWT>

grant_type=refresh_token&refresh_token=...
```

> 标准认证 (`client_secret_basic`) 完成后, 服务端额外验证 PoP 数据. 验证失败将拒绝请求.

## 认证流程

### 独立认证

```
Client                                    Authorization Server
  |                                                |
  |  POST /oauth2/token                            |
  |  Headers: OAuth-Client-Attestation,            |
  |           OAuth-Client-Attestation-PoP         |
  |  Body: grant_type, scope, ...                  |
  | ---------------------------------------------> |
  |                                                |
  |  1. Converter extracts attestation data        |
  |  2. Provider verifies PoP, resolves client_id  |
  |  3. Lookup RegisteredClient, check auth method |
  |  4. Validate client_id consistency if present  |
  |  5. Issue Access Token                         |
  |                                                |
  | <--------------------------------------------- |
  |  200 { access_token, token_type, ... }         |
```

### 增强认证

```
Client                                    Authorization Server
  |                                                |
  |  POST /oauth2/token                            |
  |  Authorization: Basic / client_secret_post     |
  |  Headers: OAuth-Client-Attestation,            |
  |           OAuth-Client-Attestation-PoP         |
  |  Body: grant_type, ...                         |
  | ---------------------------------------------> |
  |                                                |
  |  1. Standard client authentication completes   |
  |  2. AttestationFilter detects attestation hdrs |
  |  3. Reuse Converter + Provider to verify PoP   |
  |  4. Verify PoP client_id matches standard auth |
  |  5. Attach attestation context, continue       |
  |                                                |
  | <--------------------------------------------- |
  |  200 { access_token, token_type, ... }         |
```

## `client_id` 一致性校验

依据草案 §7.1 第 7 条: 若请求携带 `client_id`, 授权服务器 **必须** 校验其与 Client Attestation JWT 的 `sub` claim 一致. 增强认证模式下同样会校验解析出的 `client_id` 与标准认证已确认的 `client_id` 一致.

## 错误码

| 错误码 | HTTP 状态码 | 场景 |
|--------|-------------|------|
| `invalid_client_attestation` | 400 | PoP 验证失败 (签名、时间窗口、challenge、jti 重放等) |
| `invalid_client` | 401 | client_id 不匹配或客户端不存在 |
| `unauthorized_client` | 400 | 客户端未配置本次请求所用的 attestation 认证方式 (`attest_jwt_client_auth` / `attest_appattest_client_auth`) |

## 安全考量

* PoP JWT 有效期限制为 5 分钟, `jti` 防重放, `challenge` 一次性消费 — 三层防护防止重放攻击.
* 增强认证模式下, 标准认证与设备证明 **双重校验**, 任一失败即拒绝.

## 与草案的差异

本实现相对 [OAuth 2.0 Attestation-Based Client Authentication (Draft)] 有以下出入:

| # | 差异点 | 草案规定 | 本实现 | 性质 |
|---|--------|---------|--------|------|
| 1 | `OAuth-Client-Attestation` 可省略 | §7.1 第 1 条要求恰好一个该头; §5.1 规则 3 与 §7.2 第 4 条要求 PoP 签名**必须**用 Client Attestation JWT 的 `cnf` 公钥验证 | `jwt` 载体下允许省略, 改用 PoP JWT 头的 `kid` 查找**已注册**公钥(需启用 `euler.security.authentication.app-attest`); 动机是公钥未变更时不必每次重传 Attestation | **违背** |
| 2 | 挑战 / 新鲜度错误码 | §7.4: challenge 不匹配 **MUST** 返回 `use_attestation_challenge`, 并在 `OAuth-Client-Attestation-Challenge` **响应头**回带新 challenge; Attestation 不够新 **MUST** 返回 `use_fresh_attestation` | 两个错误码已在 `EulerOAuth2ErrorCodes` 定义但**从未抛出**; challenge 失效统一返回 `invalid_client_attestation`(草案对其为 MAY), 也不回带新 challenge | **违背**(MUST 级) |
| 3 | 新增 PoP 载体的扩展方式 | §5: 其他 PoP 机制 **MUST** 注册自己的 token 端点认证方式值(类比 `attest_jwt_client_auth_dpop`) | Apple 变体使用独立的 `attest_appattest_client_auth`, 符合"独立认证方式值"的机制要求; 但该值**未在 IANA 注册**(注册为 Specification Required, 需公开规范), 且为识别存量客户端仍接受已废弃的自定义头 `OAuth-Client-Attestation-Type` | 机制不违背; 仅未完成 IANA 注册 |
| 4 | DPoP 合并模式 | §5.2 / §7.3 定义, 认证方式值 `attest_jwt_client_auth_dpop`; §7 对 AS 是否支持为 MAY | 不支持 | 不违背 |
| 5 | `challenge` claim | §5.1 为 OPTIONAL | 强制要求 | 不违背(收紧) |
| 6 | PoP JWT 头的 `kid` | §5.1 只定义 `typ` 与 `alg` | 单 PoP 模式下要求 `kid` | 随差异 1 派生 |

> 差异 1-3 属真实违背, 是否收敛为标准行为需权衡已发布客户端的兼容性; 差异 4-6 属草案允许的自由度(不支持可选模式、收紧可选字段).

## 参考

* [The OAuth 2.0 Authorization Framework]
* [The OAuth 2.1 Authorization Framework (Draft)]
* [OAuth 2.0 Attestation-Based Client Authentication (Draft)]

[The OAuth 2.0 Authorization Framework]: https://www.rfc-editor.org/rfc/rfc6749.html
[The OAuth 2.1 Authorization Framework (Draft)]: https://www.ietf.org/archive/id/draft-ietf-oauth-v2-1-16.txt
[OAuth 2.0 Attestation-Based Client Authentication (Draft)]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-11
