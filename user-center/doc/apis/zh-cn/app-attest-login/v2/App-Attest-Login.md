# Apple App Attest 登录完整流程文档

本文档系统地讲述了 iOS App 使用身份认证服务基于 Apple App Attest 完成 App 安装实例证明 + 用户证明, 由服务端签发 Token
的完整流程.

其中用户证明的方式统一以 `<user_grant>` 代指. 它可以是任意一种 OAuth 2.1 Grant Type, 例如:

- 本认证服务支持的标准 OAuth 2.1 Grant Type, 例如 `authorization_code`, `refresh_token`
- 本认证服务支持的扩展 Grant Type, 例如 `otp`

具体 `<user_grant>` 的接入细节由各自专项文档描述 (例如[短信 / 邮箱 OTP 接入细节](App-Attest-Login-%23-OTP.md)), 本文聚焦在
**设备证明 + 用户证明**的组合机制本身.

---

## 零. 流程预览

```mermaid
graph TB
    A[APP 启动] --> C1{已完成 App 实例注册?\n本地有已注册 kid}
    C1 -- 否 --> D1[静默完成 App 实例注册\n生成并登记 kid]
    C1 -- 是 --> C2{已完成客户端注册?\n本地有 client_id}
    D1 --> C2
    C2 -- 否 --> D2[静默完成客户端注册\n取得 client_id]
    C2 -- 是 --> P{本地存在用户身份数据?}
    D2 --> P
    P -- 否, 未登录 --> LOGIN[登录取 Token\n OTP / IdP / user_assertion 等 user_grant]
    P -- 是, 已登录 --> T{AT 是否临期/过期?}
    T -- 是, 已临期/过期 --> E[续期 AT\n grant_type=refresh_token]
    T -- 否, 未临期/过期 --> U[正常使用]
    E -- 续期失败 --> X[自动退出, 清除用户身份数据, 回到登录页面]
    X --> LOGIN
    classDef enroll fill: #e7e0f7, stroke: #6f42c1, stroke-width: 1px, color: #3d1a78
    classDef scene2 fill: #cce5ff, stroke: #007bff, stroke-width: 1px, color: #004085
    classDef normal fill: #d4edda, stroke: #28a745, stroke-width: 1px, color: #155724
    classDef disabled fill: #e2e3e5, stroke: #6c757d, stroke-width: 1px, color: #383d41
    class D1 enroll
    class D2 enroll
    class LOGIN scene2
    class U normal
```

---

## 一. 核心概念

| 概念            | 说明                                                                                                                                                                                   |
|-----------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| AT              | **Access Token**<br>OAuth 2.1 访问令牌, 调业务接口与 Account Service 用.                                                                                                               |
| RT              | **Refresh Token**<br>OAuth 2.1 续期令牌. 服务端可配置为**轮换**(refresh token rotation): RT 一次性、每次续期换发新 RT、旧的随即失效. 客户端每次续期后都应以响应中的 RT 覆盖本地保存的. |
| Issuer          | **认证服务**<br>包含两套认证服务<br>- 基于 OAuth 2.1 和 OIDC 协议的用户认证服务<br>- 基于设备证明的 App 安装实例认证服务<br>本文档还会用 `{issuer}` 表示认证服务的基地址.              |
| Account Service | **账号服务**<br>提供 `/user/identities` 等账号身份管理接口.<br>本文档还会用 `{account-servcie}` 表示用户账号服务的基地址.                                                              |
| `<user_grant>`  | **申请 OAuth Token 时所用的用户证明方式**<br>即一个 `/oauth2/token` 请求的 `grant_type`.                                                                                               |
| `<credential>`  | **`<user_grant>` 所需的用户凭据参数**<br>由对应 grant 的专项文档定义. 例如 `authorization_code` 请求的用户凭据就是授权码 `code`.                                                       |

> 本文档为了书写方便, 描述接口 URL 时, 默认不带基地址, 例如申请 Token 接口一般会直接写为 `POST /oauth2/token` 或
> `POST {issuer}/oauth2/token`, 实际请求时应在前面拼接用户认证服务基地址.
>
> 基地址可能是域名, 也可能包含路径, 以下地址都是合法的基地址, 客户端接入时应注意兼容.
> - 域名: `https://auth.example.com`
> - 域名 + ContextPath: `https://account.example.com/auth`
> - 域名 + API 版本路径: `https://account.example.com/api/v1`
>
> 以获取用户身份接口 `GET {account-service}/user/identities` 为例 , 假设账号服务的基地址为
> `https://account.example.com/api/v1`, 则完整的接口 URL 为
> ```http
> GET https://account.example.com/api/v1/user/identities
> ```

---

## 二. 服务端点发现

### 2.1 认证服务端点

#### 2.1.1 用户认证服务端点

遵循 [OpenID Connect Discovery 1.0](https://openid.net/specs/openid-connect-discovery-1_0.html) 规范, 从以下 well-known
端点获取

```
GET {issuer}/.well-known/openid-configuration
```

#### 2.1.2 App 安装实例认证服务端点

属自定义扩展协议, 从以下 well-known 端点动态获取

```
GET {issuer}/.well-known/app-attest-configuration
```

> ⚠️ **两个 challenge 端点勿混**: 用户认证服务和 App 安装实例认证服务各有一个 `challenge` 端点, 但其地址不同, 应分别从各自的
> well-known 端点获取, 切勿混用.
>
> 用户认证服务的端点为 `POST /oauth2/challenge`, 其获取位置为
> ```
> GET {issuer}/.well-known/openid-configuration#challenge_endpoint
> ```
> App 安装实例认证服务的端点为 ` POST /app_attest/challenge`, 其获取位置为
> ```
> GET {issuer}/.well-known/app-attest-configuration#challenge_endpoint
> ```

### 2.2 账号服务端点

账号服务不支持 well-known 端点, 其端点地址都是固定的, 接入方根据接口文档拼接上 `{account-service}` 即可.

> ⚠️ **认证服务和账号服务合并部署时的注意事项**: 认证服务和账号服务支持合并部署, 即 `{issuer}` 和 `{account-service}`
> 可能相同, 但接入时仍应维护两个相互独立的配置项, 防止其中某一个被单独修改.

---

## 三. 接入步骤

### 3.1 App 实例注册

App 安装实例首次启动时利用 Apple App Attest 的 Attestation 向认证服务证明自己是一个安装在真实 iOS 设备上的 App.
这一机制可以确保服务端拿到未经篡改的可信 teamId 和 bundleId. 注册完成后, App 后续可以使用 Assertion 快速声明自己的身份,
例如用 Assertion 作为 RFC 7591 OAuth Client 动态注册请求的认证凭据.
完整契约见 [Apple App Attest 实例注册](../../App-Attest-Registration.md).

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Auth Server
    App ->> App: generateKey 生成 kid<br>(私钥入 Secure Enclave)
    App ->> Server: POST /app_attest/challenge
    Server -->> App: challenge
    App ->> App: attestKey 对 challenge 签发 attestation
    App ->> Server: POST /app_attest/register (attestation + challenge)
    Server -->> App: 返回 kid, 与 generateKey 生成的 kid 相同
```

### 3.2 OAuth 客户端注册

App 安装实例利用 Apple App Attest 的 Assertion
按 [RFC 7591 OAuth 2.0 Dynamic Client Registration Protocol](../../OAuth2-Client-Registration-%23-App-Attest-Dynamic.md)
将自己注册为一个 OAuth Client, 为后续申请 Token 做准备.

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Auth Server
    App ->> Server: POST /oauth2/challenge
    Server -->> App: challenge
    App ->> App: generateAssertion 对 challenge 签发 assertion
    App ->> Server: POST /oauth2/register (头携 kid+assertion+challenge, 体 RFC7591 JSON)
    Server -->> App: 201 {client_id} 回绑至 kid
```

> ⚠️ 3.1 中 `generateKey` 产生的 `kid` 和 3.2 中 `POST /oauth2/register` 接口返回的 `client_id` 的生命周期应该都是 App
> 安装实例级的. 即不随用户退出登录而清楚, 仅在 App 被卸载或 kid 被吊销时才清除.

### 3.3 Token 申请与续期

App 实例注册与客户端注册完成后 (本地已持有 `kid` 和 `client_id`), 使用 [OAuth2 Client Attestation-Based Authentication with Apple App Attest](../../OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) 作为 OAuth Client 认证机制通过 `POST /oauth2/token` 接口使用不同的 `grant_type`
完成 Token 申请或续期.

目前支持的 `grant_type`

| Grant Type       | Credential 参数      | 用途                           | 参考文档                                                                             |
|------------------|----------------------|--------------------------------|--------------------------------------------------------------------------------------|
| `otp`            | `otp_ticket` + `otp` | 邮箱 / 短信 验证码登录         | [App Attest Login with OTP](App-Attest-Login-%23-OTP.md)                             |
| `refresh_token`  | `refresh_token`      | AT 续期                        | [The OAuth 2.0 Authorization Framework](https://www.rfc-editor.org/rfc/rfc6749.html) |
| `user_assertion` | `user_assertion`     | 匿名试用                       | [App Attest Login with User Assertion](App-Attest-Login-%23-User-Assertion.md)       |
| `conflict_token` | `conflict_token`     | 绑定冲突时一键切换到已绑定账号 | (本期不实现)                                                                         |

#### 3.3.1 离线状态申请 AT

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {kid}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {Base64(Assertion Object)}

grant_type=<user_grant>
&<credential>
&scope=openid
```

时序图:

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Authorization Server
    participant CP as <credential> 提供方<br>(用户, IdP)
    App ->> CP: 请求 <credential>
    CP -->> App: <credential><br>(例如用户输入 OTP 或 IdP 返回授权码)
    App ->> Server: POST /oauth2/challenge
    Server -->> App: challenge
    App ->> App: generateAssertion 对 challenge 签发 assertion
    App ->> Server: POST /oauth2/token (头携 assertion, 体 grant_type=<user_grant> 与 <credential>)
    Server ->> Server: 验证 assertion 完成客户端认证, 再验证 <credential> 完成用户身份认证
    alt 用户身份认未绑定任何账号
        Server ->> Server: 自动开通新账号, 绑定用户身份认, 并签发 AT 和 RT
    else 用户身份认已绑定某账号
        Server ->> Server: 为该账号签发 AT 和 RT
    end
    Server -->> App: AT 与 RT
```

#### 3.3.2 在线状态续期 AT

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {kid}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {Base64(Assertion Object)}

grant_type=refresh_token
&refresh_token={refresh_token}
&scope=openid
```

时序图:

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Authorization Server
    App ->> Server: POST /oauth2/challenge
    Server -->> App: challenge
    App ->> App: generateAssertion 对 challenge 签发 assertion
    App ->> Server: POST /oauth2/token (头携 assertion, 体 grant_type=refresh_token 与 refresh_token)
    Server ->> Server: 验证 assertion 完成客户端认证, 再校验 refresh_token
    alt refresh_token 有效
        Server ->> Server: 签发新 AT (开启轮换时一并换发新 RT, 旧 RT 随即失效)
        Server -->> App: 新 AT (及新 RT)
    else refresh_token 过期 / 失效 (invalid_grant)
        Server -->> App: 400 invalid_grant
        App ->> App: 清除用户身份数据与会话凭证, 回到登录页 (转 3.3.1 重新登录)
    end
```

> 💬 **离线的判定**: "在线"指客户端有可用的 AT, 或 AT 已过期但具备**静默续期 AT** 的能力 (即有可用的 RT). 但由于 RT 没有记录过期时刻, 所以客户端无法直接判定 RT 是否可用, 一个比较简单的办法就是在 AT 过期时直接用 RT 取尝试续期一次, 续期失败即视为离线.

#### 3.3.3 匿名试用

用户认证服务支持一种特殊的用户身份认证方式: `user_assertion`. 该方式允许 App 静默生成一对非对称密钥, 使用私钥签名作为身份认证凭据完整用户身份认证. 此方式的安全性相对较低, 仅适用于未绑定其他用户身份的账号使用. 若账号已绑定其他用户身份, 则无法再绑定 `user_assertion` 身份; 若已绑定 `user_assertion` 身份的账号绑定了其他用户身份, 则已绑定的 `user_assertion` 身份将变为不可用状态.

请求示例:

首次开通, JWS 头需要携带公钥 `jwk`:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {kid}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {Base64(Assertion Object)}

grant_type=user_assertion
&user_assertion={JWS: 头 alg + jwk(携带 kid 和公钥), payload 含 jti/iat/aud}
&scope=openid
```

后续登录, 在 RT 可用时优先走 3.3.2 的续期路径, 若 RT 已过期则使用本地的私钥重签 JWS 申请新 AT 和 RT:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {kid}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {Base64(Assertion Object)}

grant_type=user_assertion
&user_assertion={JWS: 头 alg + kid, payload 含 sub/jti/iat/aud}
&scope=openid
```

时序图:

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Authorization Server
    App ->> App: 平台安全区生成密钥对 (仅首次), 得到公钥 JWK
    App ->> Server: POST /oauth2/challenge
    Server -->> App: challenge
    App ->> App: generateAssertion 对 challenge 签发 assertion (客户端认证)
    App ->> App: 用私钥签发 user_assertion (登录带 sub, 首次开通无 sub 且头附公钥 jwk)
    App ->> Server: POST /oauth2/token (头携 assertion, 体 grant_type=user_assertion 与 user_assertion)
    Server ->> Server: 验证 assertion 完成客户端认证, 再校验 user_assertion (签名与 jti/iat/aud)
    alt 首次开通 (无 sub)
        Server ->> Server: 用 jwk 验签, 自动开通新账号并存入公钥, 签发 AT 和 RT
    else 登录 (有 sub)
        Server ->> Server: 由 sub 定位账号并用登记公钥验签, 校验账号仍仅有 public_key 身份 (否则 invalid_grant), 签发 AT 和 RT
    end
    Server -->> App: AT 与 RT
    App ->> App: 从 token 解析 sub 并持久化 (供后续登录回传)
```

> ⚠️ **注意区分 `OAuth-Client-Attestation-Kid` 的 `kid` 和 `user_assertion` 中的 `kid`.**
> 
> 这是两把不同密钥的 `kid`:
> - `OAuth-Client-Attestation-Kid` 的 kid 是 Apple App Attest 的 kid, 用于生成 Apple App Assertion 完成客户端身份认证;
> - `user_assertion` 的 kid 是是 App 自行生成的不对称密钥的 kid, 用于生成 User Assertion 完成用户身份认证.

### 3.4 绑定用户身份

一个账号可绑定多个用户身份, 例如手机, 邮箱, IdP 账号 (Google, 微信) 等.

- 接口: `POST /user/identities`(携 AT). 它只为 **当前 AT 对应的账号**追加一条绑定, **不下发新 AT、也不改账号**;
  绑定结果体现在后续会话 (下次续期时服务端返回反映已绑定身份的新 AT).
- 绑定其他用户身份后, 该账号的 `user_assertion` 登录 **随即失效**(返回 `invalid_grant`), 改由该身份登录, 数据无损; 无需删除原
  `public_key` 身份数据.
- 各身份类型的绑定参数见其专项文档
  (如 [OTP 接入细节 · 绑定场景](App-Attest-Login-%23-OTP.md#三-绑定场景-账号追加手机--邮箱绑定)).

**绑定冲突处置**: 若目标身份的 `唯一标识` 在服务端已属于 **另一个账号**, `POST /user/identities` 返回
`409 identity_occupied` 并附带短期 `conflict_token`(固定不改变当前账号). 客户端应弹窗引导用户二选一:

- **方式 A — 换一个未被占用的身份**(现行): 用户重新取得新的 `<credential>` 再调 `POST /user/identities`,
  绑定成功后当前账号升级为正式账号.
- **方式 B — 一键切换到该身份已绑定的账号**(本期不实现): 用 `conflict_token` 走 `/oauth2/token`
  (`grant_type=conflict_token`) + assertion 请求头切换至 `账号_2`; APP 清除本地用户数据 (用户身份数据 + 会话凭证)但 **保留
  App 实例级 `kid` + `client_id`**, 服务端返回 `账号_2` 的 AT/RT (`kid` 仅作客户端认证, 不绑定账号). 原 `账号_1`
  在服务端保留但自身无任何用户身份, 成为 **孤儿账号**(试用数据留在服务端、客户端无法再访问, 且不迁移到 `账号_2`). 这是
  **账号切换而非数据合并**, 落地后应在调用前明确告知用户.

### 3.5 退出登录

退出登录执行统一清理:

| 数据                                              | 退出时动作 |
|---------------------------------------------------|------------|
| `access_token` / `refresh_token`                  | 删除       |
| 用户身份数据(identities)                          | 整体删除   |
| App 实例注册信息(`kid` / `client_id`, 见〈五.1〉) | **保留**   |

- **为何保留 `kid` / `client_id`**: 它们是 **App 实例的注册凭据**, 与用户身份解耦 (只标识"哪台设备上的哪个 App 实例",
  不含用户数据), 保留以便下次登录直接复用、免去重新 `attestKey`(Apple 对 attestation 有频率限制). Secure Enclave 中的私钥由
  iOS 管理, APP 无需也无法显式销毁.
- **下次登录路径**: 本地无用户身份数据与 AT/RT, 但 App 实例级 `kid` + `client_id` 仍在 → 直接走〈三.3〉取 Token, 无需重新注册.
- **服务端侧无账号关联**: 退出登录是客户端本地动作, 不通知服务端. 服务端只保留 App 实例注册信息 (`kid → client_id`,
  属客户端域), `kid` **不与任何用户账号绑定** —— 同一 App 实例的 `kid` 可供该实例上任意账号的登录复用, 退出或换账号都无需服务端清理.

---

## 四. 异常处置: `kid` 被服务端吊销

`kid` 吊销主要发生在 **触发 Apple 风控时**: Apple 判定该设备或其 App Attest 密钥存在欺诈风险, 服务端随即吊销对应 `kid`.
被吊销后该 `kid` 及其绑定的 `client_id` 在服务端不再可用, 所有携其 assertion 的请求 (取 Token、续期)都会失败.

**失效处置**: 客户端清除本地 **失效的 App 实例注册信息**(`kid` + `client_id`)与用户数据 (用户身份数据 + 会话凭证),
使下次启动静默 `generateKey()` 重新完成 App 实例注册 + 客户端注册 (得 `kid_new` + `client_id`), 再按账号类型恢复:

- **正式账号**: 用新 `kid` 重新走[取 Token](#33-取-token), 由用户凭据 (如 OTP)解析到原账号并下发新 AT, **账号数据无损**.
- **匿名试用账号 (`user_assertion`)**: 其身份 **与 `kid` 解绑**(由用户自持私钥 + `sub` 定位,
  见 [user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md)). 只要客户端仍持有该账号的私钥与 `sub`, 用新
  `kid` 完成客户端认证后即可照常 `user_assertion` 登录, **账号数据无损**; 仅当私钥也丢失时账号才不可恢复 (成为孤儿).

---

## 五. 客户端持久化数据

客户端持久化的数据分三类, 每类按两个维度定性, 由此决定存储位置与清理时机:

- **生命周期**: `session`(退出登录 / 续期失败时清除, 见〈三.5〉) 或 `persistent`(仅"抹掉所有数据"或注册信息失效时才清除).
- **是否机密**: 机密数据 (bearer 凭据、用户 PII)**强制存 Keychain**, 禁止明文存 `UserDefaults` / `plist`(越狱设备可读取);
  非机密数据 (标识符类, 如 `kid` / `client_id` —— 签名私钥不可导出, 泄露不构成风险)**不强制存储位置**, 由客户端自选.

| 数据类           | 生命周期     | 是否机密 | 存储位置           |
|------------------|--------------|----------|--------------------|
| App 实例注册信息 | `persistent` | 否       | 不强制(客户端自定) |
| 会话凭证         | `session`    | 是       | **Keychain**       |
| 用户身份数据     | `session`    | 是       | **Keychain**       |

### 5.1 App 实例注册信息 (App Instance Enrollment)

App 实例注册与客户端注册的产物, **归属 App 实例、独立于用户身份数据**.

```json
{
  "kid": "<base64-url-encoded-kid>",
  "client_id": "<base64-url-encoded-client-id>",
  "iat": "2026-05-10T12:34:56Z"
}
```

| 字段        | 类型   | 含义                                                                                 |
|-------------|--------|--------------------------------------------------------------------------------------|
| `kid`       | string | **Apple App Attest Key Identifier**, App 实例注册(`/app_attest/register`)后本地持有  |
| `client_id` | string | **per-KEY OAuth2 客户端标识**, 客户端注册(`/oauth2/register`)返回, 与 `kid` 一一绑定 |
| `iat`       | date   | **Apple App Attest Key 的生成时间(Issued At)**                                       |

### 5.2 会话凭证 (Session Credentials)

> 下例为 `POST /oauth2/token` 成功响应结构 (下划线风格, APP 可自行映射为驼峰). 客户端把 token 值原样保存, 并在
> **收到响应的当下**据 `expires_in` 换算出 AT 绝对过期时刻一并存下; `token_type` / `scope` 无需持久化.

```json
{
  "access_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9...",
  "token_type": "Bearer",
  "expires_in": 3599,
  "refresh_token": "8xLOxBtZp8eNq7WmY3sQ1vC5yR2tG4zH",
  "scope": "openid profile",
  "id_token": "eyJhbGciOiJSUzI1NiIsInR5cCI6IkpXVCJ9..."
}
```

客户端持久化字段:

| 字段                      | 类型   | 含义                                                                                                                                                    |
|---------------------------|--------|---------------------------------------------------------------------------------------------------------------------------------------------------------|
| `access_token`            | string | 业务接口凭证(AT)                                                                                                                                        |
| `access_token_expires_at` | date   | AT 绝对过期时刻, **收到响应时**据 `expires_in` 换算记录, 用于临期判断                                                                                   |
| `refresh_token`           | string | 续期凭证(RT). 每次续期后以响应中的新 RT 覆盖本地保存(服务端开启**轮换**时旧 RT 随即失效)                                                                |
| `id_token`                | string | **OIDC ID Token**(JWT), 承载用户 profile 声明(`sub` / `name` / `picture` 等). 需 `scope` 含 `openid`; 用户档案从此解析, 或用 AT 调 `GET /userinfo` 获取 |

> **用户 profile 不在本文档统一持久化**: 业务方可能有自己的 profile 服务, 故本文不定义 profile 结构. 与用户档案相关的信息由
> `id_token` 承载 (或经 `GET /userinfo` 获取), 客户端按需处理.

### 5.3 用户身份数据 (Identities)

> 示例采用下划线风格 (与接口返回一致), APP 可自行映射为驼峰.

```json
[
  {
    "identity_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
    "identity_type": "email",
    "identifier": "3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b",
    "bound_at": 1778899139687,
    "email": "u**r@e*****e.com"
  }
]
```

| 字段                 | 类型         | 含义                                                                                                                                                                     |
|----------------------|--------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| `$`                  | list         | **账号绑定的用户身份(手机 / 邮箱 / Apple / Google / public_key 等)列表**(根数组). 用 AT 调 `GET /user/identities` 获取; 每个元素含下述公共字段, 各类型可追加自身原生字段 |
| `$[*].identity_id`   | string       | **公共字段 — 用户身份 ID**, 服务端生成的 UUID                                                                                                                            |
| `$[*].identity_type` | string       | **公共字段 — 用户身份类型标识**, 如 `apple` / `google` / `phone` / `email` / `public_key`                                                                                |
| `$[*].identifier`    | string       | **公共字段 — 该身份的稳定唯一标识**, 不同 `identity_type` 各自定义其含义(如 `phone` / `email` 为原值的哈希)                                                              |
| `$[*].bound_at`      | timestamp(3) | **公共字段 — 首次绑定时间**, 毫秒级 Unix 时间戳                                                                                                                          |

> `identities` 中每个元素 = 公共字段 (`identity_id` / `identity_type` / `identifier` / `bound_at`) + 该类型的原生字段.
> 原生字段因 `identity_type` 而异, 由各自专项文档定义 —— 例如 [OTP 接入细节](App-Attest-Login-%23-OTP.md) 的 `phone` /
> `email` 元素、[user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md) 的 `public_key` 元素.

---

## 六. 常见坑位

1. **AT 续期用 `refresh_token` + assertion 请求头**: 每次续期后以响应中的新 RT 覆盖本地保存的 (服务端开启 **轮换**时旧
   RT 随即失效); RT 过期后只能引导用户重新登录.
2. **绑定用户身份不下发新 AT, 也不改账号**: `POST /user/identities` 只为当前账号追加一条绑定, 客户端不要替换会话凭证;
   绑定结果体现在后续续期返回的新 AT.
3. **绑定用户身份与登录用不同接口**: 账号追加身份走 `POST /user/identities`(携 AT); 退出后重登走〈三.3〉 (App 实例级
   `kid` / `client_id` 已保留, 直接取 Token); 仅 `kid` 被吊销时才需重新注册.
4. **绑定返回 `409 identity_occupied` 时需用户抉择**: 响应附带 `conflict_token`, 客户端弹二选一 —— 换一个未被占用的身份继续绑定
   (现行方式 A), 或一键切换到该身份对应的已有账号 (方式 B, **本期不实现**, 会使原账号成孤儿、数据不合并).
5. **`assertion` vs `attestation` 不要混用**: `attestation` 只提交至 `/app_attest/register`(每 KEY 一次);
   `/oauth2/register` 与 `/oauth2/token` 一律用 `assertion`(请求头承载). 退出登录 **不**重新生成 `kid`, 仅 `kid` 被吊销时才重新
   `generateKey()` 并重走注册.
6. **Challenge 一次性, 约 5 分钟过期**: 不要缓存跨请求复用; 每次生成 attestation / assertion 前都需重新获取.
7. **机密数据必须存 Keychain**: 会话凭证 (AT / RT)与用户身份数据 (含 PII)属机密, 必须放 Keychain; App 实例注册信息
   (`kid` / `client_id`)非机密 (私钥不可导出), 存储位置不强制.
8. **三类数据生命周期不同**: App 实例注册信息 (`persistent`, 跨登录保留)、用户身份数据 (随账号切换变化)、会话凭证 (随每次
   Token 刷新变化), 建议分开存储 (各自独立条目), 避免一次写入失败导致全部丢失.
9. **`kid` 被吊销时的处理**: assertion 被拒且错误码指向 `kid` 失效时, 清除失效的 `kid` + `client_id` 与用户数据, 回登录页;
   下次启动静默重注册新 `kid` 再按账号类型恢复 (正式账号数据无损; `user_assertion` 账号只要私钥 + `sub` 仍在亦无损).
   详见〈四〉.
10. **`user_assertion` 保证级别低**: 只验客户端不验用户, 仅宜试用 / 低敏感场景; 账号绑定其他用户身份后 `user_assertion`
    登录失效. 详见 [user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md).

---

## 七. 相关文档

- [短信 / 邮箱 OTP 接入细节](App-Attest-Login-%23-OTP.md) — 以手机号 / 邮箱 OTP 作为 `<user_grant>` 的落地
- [user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md) — 匿名试用 (`user_assertion` 公钥持有证明)的落地
- [Apple App Attest 服务发现](../../App-Attest-Discovery.md) — `/.well-known/app-attest-configuration` 端点发现
- [Apple App Attest 实例注册](../../App-Attest-Registration.md) — `POST /app_attest/register` 完整契约
- [OAuth2 Client Registration - App Attest DYNAMIC](../../OAuth2-Client-Registration-%23-App-Attest-Dynamic.md) — RFC
  7591 动态客户端注册契约
- [Attestation Based Client Authentication (Apple App Attest)](../../OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) —
  token 端点的 assertion 客户端认证
- [绑定用户身份](../../APIs-%23-User-Identities-Create.md) — `POST /user/identities` 契约
- [OAuth2 Token Grant - App Attest](../../OAuth2-Token-Grant-%23-App-Attest.md) — 已废弃 `app_assertion` grant 的兼容说明
- [v1 历史版本文档](../../app-attest-login/v1/App-Attest-Login.md) — 改造前的旧版流程 (已废弃)
