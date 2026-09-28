# App Attest 登录 - 短信 / 邮箱 OTP 接入细节

> 本文档为 **v2 现行版本** (上位文档 [App-Attest-Login](App-Attest-Login.md) 的附录), 与 [Apple App Attest 实例注册](../../App-Attest-Registration.md)、[Attestation Based Client Authentication (Apple App Attest)](../../OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) 描述的实现保持一致. 改造前的旧版流程见 [v1](../../app-attest-login/v1/App-Attest-Login-%23-OTP.md).

本文档是 [Apple App Attest 登录完整流程文档](App-Attest-Login.md) 的配套附录, 专门描述**短信 / 邮箱 OTP**(One-Time Password, 一次性验证码)作为 `<user_grant>` 接入时的具体细节. 总流程、抽象概念、客户端持久化数据、异常处置、退出登录、常见坑位等均请参考上位文档, 本文只补充 OTP 侧的实例化内容.

---

## 一. 抽象概念在 OTP 侧的映射

| 总文档抽象 | OTP 落地 |
|---|---|
| `<user_grant>` | `otp`(自定义 grant type, 统一承载所有 OTP 通道) |
| `<credential>` | `otp_ticket` + `otp` 二元组 |
| `identity_type` | `phone`(短信通道绑定结果) / `email`(邮箱通道绑定结果) |
| 唯一标识 | E.164 手机号 / 邮箱地址 |
| CP(Credential Provider) | 用户本人(在设备上输入 OTP) + 平台下发通道(SMS / SMTP) |

落到 `/oauth2/token` 请求矩阵:

| `grant_type` | 请求体关键参数 | 用途 |
|---|---|---|
| `otp` | `otp_ticket` + `otp` | 标准 OTP 登录 (App Attest 数据经请求头承载) |

> 注 1: v2 中 App Attest 数据一律经请求头 (`OAuth-Client-Attestation-*`) 承载, 请求体只放 `grant_type` 与该 grant 自身的参数; 设备注册已拆分为独立的 App 实例注册 + OAuth2 客户端注册前置步骤, 详见上位文档场景二.
>
> 注 2: 本平台自定义的 `grant_type=otp` 只负责"以 OTP 作为用户证明"这件事, 具体的通道类型(`sms` / `email` / 未来扩展)、下发目标(`recipient`)、业务用途(`purpose`)均在 `otp_ticket` 签发时记录于服务端, 客户端在 `/oauth2/token` 时无需重复提交.

### OTP 侧关键角色

| 角色 | 含义 |
|---|---|
| **用户** | 在设备上读取短信 / 邮件中的 OTP 并输入到 App |
| **下发通道** | 服务端将 OTP 下行到 `recipient` 的能力提供方(运营商短信网关 / SMTP 邮件服务), 属于服务端内部依赖, 对客户端不可见 |

---

## 二. 发送 OTP: `POST /otp/tickets`

**此接口为平台通用接口, 不专属 OAuth.** 调用方通过 `channel` 指定下发途径, 通过 `purpose` 声明业务用途, 服务端按 `purpose` 选择对应的文案模板 / 频率策略 / 有效期 / 后续允许的调用接口.

### 2.1 请求

```http
POST /otp/tickets
Content-Type: application/x-www-form-urlencoded

channel=sms
&recipient=%2B8613900000000
&purpose=sign_in
```


| 参数 | 必选 | 说明 |
|---|---|---|
| `channel` | 是 | **下发通道**<br>`sms` / `email` / 未来扩展(如 `voice`) |
| `recipient` | 是 | **下发目标**<br>手机号需符合 E.164 格式, 邮箱需为合法邮箱地址. |
| `purpose` | 否 | **业务用途**<br>可选参数, 可以在后续的验证流程中验证是否是期望的用途, 若不指定表示可以用于任何用途, 服务端采用缺省的文案模板 / 频率策略 / 有效期 / 后续允许的调用接口.|

### 2.2 响应

```json
{
  "otp_ticket": "3f2504e0-4f89-11d3-9a0c-0305e82c3301",
  "expires_in": 300,
  "retry_after": 60
}
```

| 字段 | 说明 |
|---|---|
| `otp_ticket` | **本次 OTP 会话句柄**, 服务端签发的不可预测短随机串, 单次使用 |
| `expires_in` | OTP 有效期(秒), 过期后 `otp_ticket` 连同 OTP 一起失效 |
| `retry_after` | 调用方再次发起 `POST /otp/tickets` 的最短间隔(秒). 限流维度针对调用方, 即使更换 `recipient` / `purpose` 在该间隔内仍不会下发 |

> `otp_ticket` 与 OTP 一一强绑定, 同时记录 `channel`、`recipient`、`purpose`、下发时间、已校验失败次数等. 客户端仅需持有 `otp_ticket`, 无需理解其内部结构.

---

## 三. 绑定场景: 账号追加手机 / 邮箱绑定

本场景对应[总文档〈三.4 绑定用户身份〉](App-Attest-Login.md#34-绑定用户身份升级为正式账号)(账号追加手机 / 邮箱绑定). 客户端先调 `POST /otp/tickets`触发下发, 用户输入 OTP 后, 凭当前 AT 调 `POST /user/identities` 上行 `otp_ticket` + `otp` 二元组, 服务端校验通过后在当前账号下新增 `phone` / `email` 元素.

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Authorization Server
    participant User as 用户(含短信/邮件通道)

    Note over App,Server: 1. 请求下发 OTP
    App->>Server: POST /otp/tickets<br/>channel 和 recipient
    Server->>Server: 生成 otp 与 otp_ticket<br/>存储 channel/recipient/purpose
    Server->>User: 通过 SMS/SMTP 下发 OTP
    Server-->>App: otp_ticket 和 expires_in

    Note over App,User: 2. 用户输入 OTP
    User-->>App: 用户读取验证码并输入

    Note over App,Server: 3. 上行 <credential> 二元组完成绑定
    App->>Server: POST /user/identities<br/>Authorization Bearer AT<br/>identity_type=phone/email 和 otp_ticket 和 otp
    Server->>Server: 解析 AT 归属 账号_1
    Server->>Server: 校验 ticket 未过期未使用
    Server->>Server: 校验 otp 与 ticket 关联的验证码一致
    Server->>Server: recipient 未占用 在 账号_1 下新增 phone/email 绑定
    Server->>Server: 标记 otp_ticket 已使用
    Server-->>App: 绑定结果(identities 列表中 phone/email 元素)
    Note over App: 会话凭证保持不变 继续用原 AT
```

> 若目标手机号 / 邮箱已被其他账号占用, 服务端返回 `409 identity_occupied` 附带 `conflict_token`, 处置方式参见[总文档〈三.4 绑定用户身份〉](App-Attest-Login.md#34-绑定用户身份升级为正式账号).

---

## 四. 标准 OTP 登录完整流程

本场景对应[总文档〈三.3 取 Token〉](App-Attest-Login.md#33-取-token). 客户端先走 `POST /otp/tickets`(无 `purpose`)拿到 `otp_ticket` 并等待用户输入 OTP; 若本地尚无可用 `kid` + `client_id`, 先完成 App 实例注册与 OAuth2 客户端注册(见总文档〈三.1〉〈三.2〉); 最后以 `grant_type=otp` + `otp_ticket` + `otp` + assertion 请求头上行 `/oauth2/token`, 服务端一次性完成"客户端认证 + OTP 验证". 对于新注册(`recipient` 尚未绑定任何账号)的分支, 服务端将 `recipient` 写入新账号的 `identities`; 已绑定账号的分支直接复用原有 `identities`.

```mermaid
sequenceDiagram
    participant App as iOS App
    participant Server as Authorization Server
    participant User as 用户(含短信/邮件通道)

    Note over App,Server: 1. 请求下发 OTP
    App->>Server: POST /otp/tickets<br/>channel 和 recipient
    Server->>Server: 生成 otp 与 otp_ticket
    Server->>User: 通过 SMS/SMTP 下发 OTP
    Server-->>App: otp_ticket 和 expires_in

    User-->>App: 用户读取验证码并输入

    Note over App,Server: 2. App 实例注册 + OAuth2 客户端注册 (本地已有可用 kid 和 client_id 时跳过)
    App->>App: generateKey 生成 kid (仅首次)
    App->>Server: POST /app_attest/challenge
    Server-->>App: challenge
    App->>App: attestKey 对 kid 签发 attestation
    App->>Server: POST /app_attest/register (attestation + challenge)
    Server-->>App: {kid} 仅登记 KEY
    App->>Server: POST /oauth2/challenge
    Server-->>App: challenge2
    App->>App: generateAssertion 对 challenge2 签发 assertion
    App->>Server: POST /oauth2/register 头携带 kid+assertion+challenge2, 体为 RFC7591 JSON
    Server-->>App: 201 {client_id} 已回绑至 kid

    Note over App,Server: 3. 一次完成 客户端认证 加 用户认证
    App->>Server: POST /oauth2/challenge
    Server-->>App: challenge3
    App->>App: generateAssertion 对 challenge3 签发 assertion
    App->>Server: POST /oauth2/token<br/>头携带 kid+assertion+challenge3<br/>grant_type=otp 和 otp_ticket 和 otp
    Server->>Server: 校验 assertion 解析出 per-KEY 客户端
    Server->>Server: 校验 ticket 未过期未使用
    Server->>Server: 校验 otp 与 ticket 关联的验证码一致
    Server->>Server: 标记 otp_ticket 已使用

    alt recipient 未绑定任何账号
        Server->>Server: 新建正式账号 账号_new 并写入 identities 的 phone/email 元素
        Server-->>App: AT 和 RT 归属 账号_new
    else recipient 已绑定某个 账号_2
        Server->>Server: 登录到 账号_2
        Server-->>App: AT 和 RT 归属 账号_2
    end

    App->>App: 用新 AT 拉取用户身份数据并写回本地
```

---

## 五. 请求示例

### 5.1 发送 OTP(通用接口, 示例为登录场景)

```http
POST /otp/tickets
Content-Type: application/x-www-form-urlencoded

channel=sms
&recipient=%2B8613900000000
```

### 5.2 标准 OTP 登录

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation-Type: apple_app_attest
OAuth-Client-Attestation-Kid: {kid}
OAuth-Client-Attestation-Challenge: {challenge}
OAuth-Client-Attestation-Assertion: {Base64(Assertion Object)}

grant_type=otp
&otp_ticket={otp_ticket}
&otp={用户输入的验证码}
&scope=openid
```

### 5.3 绑定手机 / 邮箱(使用当前 AT)

```http
POST /user/identities
Authorization: Bearer {AT}
Content-Type: application/x-www-form-urlencoded

identity_type=phone
&otp_ticket={otp_ticket}
&otp={用户输入的验证码}
```

---

## 六. `identities` 中 phone / email 元素结构

作为 `identities` 列表中 `identity_type=phone` / `identity_type=email` 的元素, 由公共字段(`identity_id` / `identity_type` / `identifier` / `bound_at`, 详见[总文档 5.3 用户身份数据](App-Attest-Login.md#53-用户身份数据-identities))与 OTP 原生字段两部分组成:

```json
{
  "identity_id": "550e8400-e29b-41d4-a716-446655440000",
  "identity_type": "phone",
  "identifier": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
  "bound_at": 1778899139687,
  "phone": "+8613*******00"
}
```

```json
{
  "identity_id": "6ba7b810-9dad-11d1-80b4-00c04fd430c8",
  "identity_type": "email",
  "identifier": "3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a2b3c4d5e6f7a8b9c0d1e2f3a4b",
  "bound_at": 1778899139687,
  "email": "u**r@e*****e.com"
}
```

| 字段 | 类型 | 含义 |
|---|---|---|
| `identity_id` | string | **公共字段** — 用户身份 ID (UUID) <br> 在 OTP 这一细分场景下可以替代原始手机号或邮箱调用 `POST /otp/tickets` 做 OTP 二次下发, 避免用户隐私信息频繁暴露于网络链路. |
| `identity_type` | string | **公共字段** — 固定为 `phone` / `email`, 用于在 `identities` 列表中识别该元素的类型 |
| `identifier` | string | **公共字段** — 用户身份的唯一标识, 取值为**原始手机号 / 邮箱的 SHA-256 哈希值**, 用于全局唯一性校验. |
| `bound_at` | timestamp(3) | **公共字段** — 首次绑定时间, 毫秒级 Unix 时间戳 |
| `phone` / `email` | string | **脱敏后的手机号 / 邮箱地址**<br>用于管理页展示; 原始值仅服务端持久化, 不下发 |

> `phone` / `email` 元素无需额外的原生用户资料字段(昵称、头像等), 故无 "/userinfo" 类二次拉取.

---

## 七. 相关文档

- [Apple App Attest 登录完整流程文档](App-Attest-Login.md) — 本文档的上位文档, 描述抽象的完整流程
- [Apple App Attest 服务发现](../../App-Attest-Discovery.md) / [Apple App Attest 实例注册](../../App-Attest-Registration.md) — App 实例注册前置步骤契约
- [OAuth2 Client Registration - App Attest DYNAMIC](../../OAuth2-Client-Registration-%23-App-Attest-Dynamic.md) — RFC 7591 动态客户端注册契约
- [Attestation Based Client Authentication (Apple App Attest)](../../OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) — token 端点的 assertion 客户端认证
- [RFC 4226: HOTP](https://datatracker.ietf.org/doc/html/rfc4226) / [RFC 6238: TOTP](https://datatracker.ietf.org/doc/html/rfc6238) — 一次性验证码算法规范(本平台 OTP 非 TOTP, 仅列为参考)
- [绑定用户身份](../../APIs-%23-User-Identities-Create.md)
