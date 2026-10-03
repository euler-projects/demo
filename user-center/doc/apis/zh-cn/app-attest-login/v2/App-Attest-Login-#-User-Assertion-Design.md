# user_assertion / public_key — 服务端设计

> **状态**: 草案. 暂置于本目录, 定稿后移入专门的设计文档目录.
>
> 本文是 [user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md) 的**服务端设计 companion**. 分工: 接入细节面向**客户端开发者**, 只讲协议与可观察行为; 本文面向**服务端实现者与架构评审**, 讲数据模型、身份定位、SPI / provider 实现与决策记录. 客户端可见契约以接入细节为准, 服务端内部机制以本文为准, 二者不重复.

---

## 一. 范围与定位

- **机制**: `grant_type=user_assertion` + `identity_type=public_key` —— 用户以"持有私钥、对一次性输入签名"完成 proof-of-possession; 登录后 assertion 体内携 `sub`(账号用户名)声明, 故为名副其实的 user assertion.
- **用途**: 面向**未绑定任何其他身份**的账号(独占账号 + 绑定其他身份后自禁用); 典型场景是匿名 / 试用登录.
- **前提**: `user_assertion` grant **强制**经**已认证客户端**(当前实现为 App Attest); 用户凭证是**独立于 App Attest `kid`** 的另一把密钥, 身份**不与任何客户端认证机制绑定**(见〈二.2〉). 允许开通该类账号的客户端认证机制由 JitProvisioning 层白名单把关(见〈五〉).
- **替代**: 本设计是废弃的 `app_assertion` grant + 设备→用户映射的正规替代(见〈二.3〉).

---

## 二. 领域模型与关联决策

### 2.1 `public_key` 是 `UserIdentityService` 的一个后端

- `PublicKeyUserIdentityService extends AbstractUserIdentityService`, `identityType()` 返回 `public_key`, 纳入 `DelegatingUserIdentityService` 的按类型路由.
- 与 `phone` / `email` / `google` 并列; 无 IdP 原生资料字段, 特有数据只有单把公钥(`jwk`).

### 2.2 身份与客户端认证机制解绑; 账号由 `sub`(用户名)路由, 身份各有独立标识

**决策**: `user_assertion` 的 `sub` claim = **账号用户名**(即 token `sub`, 本实现为 `EulerUserDetails.getUsername()`). 服务端据此 `loadUserByUsername(sub)` → `user_id` → `listUserIdentities(user_id, "public_key")` 取该账号的 `public_key` 身份. 身份**自身另有独立的稳定标识**(`subject` / 对外的 `identifier`), **逐身份不同、不等于账号 `sub`**. 身份与 `app_attest_kid`、`client_id` 均**解绑**.

理由:
1. **领域纯净(彻底)**: `user_identity` 属身份域. 早期方案以 `app_attest_kid` 为命名空间, 实则把**客户端认证域**的概念焊进了身份主键(与当年"否决 `client_id`"的理由自相矛盾). 解绑后身份既不引用 `client_id` 也不引用 `app_attest_kid`.
2. **客户端认证机制无关**: 注册入口不限于 `/oauth2/token`, 客户端认证也不必然是 App Attest(mTLS / client_secret 等皆可). 具体允许哪些机制由 JitProvisioning 白名单表达(见〈五〉), 不侵入身份模型.
3. **账号级 `sub` 与身份级 `identifier` 必须分离**: token `sub` 是**账号**标识(用户名); 一个账号可绑定**多个** `public_key` 身份, 故 `identifier` 必须**逐身份不同**, 绝不能取账号 `sub`. 反查也不要求二者同值: 先查用户主表得 `user_id`, 再查 `t_user_identity`.
4. **消除 SPI 分叉**: 登录经既有通用 SPI(`loadUserByUsername` + `listUserIdentities(userId, identityType)`)路由, 无需早期 `(app_attest_kid, kid)` 复合键的专用查询 `findByAppAttestKidAndKid`(**废弃**). `findUserIdentityByRawSubject("public_key", rawSubject)` 仍保留, 但 `rawSubject` = 身份自有 `subject`(见〈三〉), 用于**开通期跨账号唯一性校验**, 不用于登录路由.
5. **扛过 App Attest `kid` 吊销**: 身份不再随 `app_attest_kid` 变化而失效, 消除早期"重注册即孤儿"的后果(见〈九〉).

### 2.3 禁止复用废弃的设备→用户映射

不得复用 `app_attest_attestation_user_mapping` / `EulerDeviceUserDetailsService` / `OAuth2AppAssertionAuthenticationProvider`(均已 `@Deprecated`, 待随旧 APP 下线删除). 那是"App Attest `kid` → 用户"的旧混淆(把客户端密钥当用户身份). 本设计中**用户凭自己持有的私钥认证**, 账号经 `sub`(用户名)路由、身份各有独立 `identifier`, 客户端认证只是**开通 / 登录的准入门槛**, 语义干净.

---

## 三. 数据模型

```text
t_user_identity  (既有父表, 不新增列)
  identity_id (PK), user_id, identity_type='public_key', subject, bound_at
  -- subject = 该身份自有的稳定标识(服务端由公钥派生, 如 RFC 7638 JWK Thumbprint),
  --            跨账号唯一、逐身份不同; ≠ 账号 username(token sub)

t_user_identity_public_key  (新增; 1:1 单行子表 —— 与 phone/email 扩展表同构)
  identity_id   PK, FK → t_user_identity.identity_id
  jwk              单把公钥材料 (kty / crv / x,y | n,e / alg)
  kid              公钥标识 (客户端生成; 账号绑多个 public_key 身份时用作登录选择器, 否则仅审计)
  origin_client_auth  开通时的客户端认证机制 (审计用, 如 attest_jwt_client_auth; 可空)
  created_at
```

- **1 身份 = 1 公钥**: **不支持密钥轮换**, `public_key` 身份创建后其 `jwk` 只读不可改(与 phone/email 单行扩展一致). 换密钥即换新账号, 旧号成孤儿(见〈九〉).
- **三个层级别混**: ①**账号级** token `sub` = 用户名(客户端回传, 路由到账号); ②`user_id`(内部); ③**身份级** `subject`(父表, 服务端由公钥派生、跨账号唯一、**逐身份不同**)—— 对外呈现为 `identities` 元素的 `identifier`. **`identifier` ≠ `sub`**: 一个账号可绑多个 `public_key` 身份, 各自 `identifier` 不同.
- **不与 `app_attest_kid` / `client_id` 绑定**: 二者均**不进入**身份键; 如需追溯开通来源, 存 `origin_client_auth` 审计列即可. 身份唯一性由父表 `subject`(公钥派生)承担, **子表无需复合唯一键**; 这也天然保证"同一把公钥不会同时属于两个身份 / 账号".

---

## 四. 登录: 身份定位与验证

`user_assertion` grant 的 provider(与 OTP provider 同构)执行:

1. 取已认证客户端: `OAuth2AuthenticationProviderUtilsAccessor.getAuthenticatedClientElseThrowInvalidClient(grantToken)`.
2. 解析 `user_assertion`(JWS) → payload 的 `sub`(= 用户名)、`jti` / `iat` / `aud`; header 的 `alg`(+`kid` 可选).
3. 路由到账号: `loadUserByUsername(sub)` → `user_id`(`sub` 有值但用户不存在 → `invalid_grant`); `listUserIdentities(user_id, "public_key")` 取该账号的 `public_key` 身份(含各自 `jwk`). **走既有通用 SPI, 无专用查询分叉**(见〈二.2〉).
4. 选钥验签: header 带 `kid` 则选中 `jwk.kid` 匹配的那个身份(账号有多个 `public_key` 身份时用以消歧), 否则取唯一的那个; 按其 `jwk` 的 `alg` 分派验签器(ECDSA / RSASSA / EdDSA)验 `user_assertion`(`sub` 非秘密, 安全全在私钥).
5. 新鲜性: `NonceService` 拒 `jti` 重用; 校验 `iat` 在时间窗内、`aud` 匹配 token 端点. (与 App Attest challenge **相互独立**, 见〈七〉)
6. 执行**独占校验**(〈六〉: 该 `user_id` 的身份须**全部为 `public_key`**); 通过后签发 AT/RT/id_token(`sub` 仍为该用户名).

> 首次开通的 assertion **无 `sub`**(客户端尚无账号), 走〈五〉的 auto-provision 分支.

---

## 五. 首次开通 (auto-provision) 与客户端认证白名单

### 5.1 开通流程

assertion **无 `sub`**(首次注册, 客户端尚无账号)且头带 `jwk`(公钥), 即视为首次开通:

1. 用 header `jwk` 验签(证明持有对应私钥); 可经 `findUserIdentityByRawSubject("public_key", 公钥派生 subject)` 兜底该公钥是否已属某身份(防重复注册).
2. 经 **JitProvisioning 白名单**校验(见 5.2)通过后, JIT 建用户(**自动生成用户名 = 日后 token `sub`**) + 建 `public_key` 身份(其 `subject` 由公钥派生、**独立于用户名**), 存入 `jwk`(及可选 `kid` / `origin_client_auth`).
3. 签发 token; **token `sub` = 该用户名**, 客户端解析并持久化, 供后续登录在 assertion `sub` claim 回传(无需专用返回字段). 身份级 `identifier` 与 `sub` 不同, 仅经 `identities` 下发, 登录**不回传**.

注册与登录**合并为一条 grant 路径**(未知即开通、已知即登录), 与 OTP 的 auto-provision 同构; 受 `JitProvisioningPolicy` 约束.

### 5.2 开通准入的客户端认证机制白名单 (JitProvisioning 层)

**决策**: "哪些客户端认证机制可开通该类账号"的限制**不放在 OAuth grant 层**, 而下沉到 **JitProvisioning 层** —— 因为注册入口未来未必是 `/oauth2/token`(可能是专用注册端点或 App 实例注册), 闸门须与入口无关. 该落点契合既有 `JitProvisioningPolicyResolver` 的契约("JIT provisioning 是 identity-type 关注点, 与入口无关"):

- **策略扩展**: `JitProvisioningPolicy` 增加 `allowedClientAuthenticationMethods`(`Set<String>`, **空 = 不限制**, 对 OTP / federated 等既有路径完全向后兼容). 配置:
  ```yaml
  euler.security.identity-type.public_key.jit-provisioning:
    enabled: true
    default-authorities: [user]
    allowed-client-authentication-methods: [attest_jwt_client_auth]   # 仅真机 App Attest 可开通
  ```
- **机制检测(中立、可插拔)**: 从 `SecurityContextHolder` 取当前请求 `Authentication`, 经 `ClientAuthenticationMechanismExtractor` **提取器链**归约为一个中立机制 id. OAuth2 模块贡献一个实现: `OAuth2ClientAuthenticationToken → getClientAuthenticationMethod().getValue()`(含 App Attest 的 `attest_jwt_client_auth`). 未来非 OAuth 入口只需再注册一个提取器, **不碰 OAuth 层**.
- **统一闸门**: `JitProvisioningGuard.check(identityType)` 在任何 JIT 开通点被调用 —— 解析策略, 要求 `enabled` 且(白名单非空时)当前机制 ∈ 白名单, 否则拒绝开通. 新 `public_key` 路径接入该 guard; 既有路径白名单为空、行为不变.
- **前提**: 该 grant 强制经**已认证客户端**(非 public client); 防刷强度 = 客户端认证机制强度(App Attest 为真机强门槛). 限频维度从早期 `app_attest_kid` 上移到"已认证客户端"这一泛化概念.
- **与废弃逻辑划清**: 该 grant 走 `identity_type=public_key`, 白名单配在 `public_key` 下, **不复用** `IDENTITY_TYPE_DEVICE`(`device`)伪类型或 `app_assertion` 路径.

---

## 六. 独占约束

`public_key` 身份只允许存在于**无任何其他身份**的账号上. 为避免"其他身份后端反过来检查 `public_key`"的反模式, 只在 `public_key` 侧两处强制:

- **绑定侧** — `PublicKeyUserIdentityService.createUserIdentity(...)`: 校验目标账号当前无任何身份(auto-provision 的新账号天然满足).
- **认证侧** — `user_assertion` provider: `listUserIdentities(userId)` 必须**全部为 `public_key`**, 否则 `invalid_grant`.

效果: 账号绑定其他用户身份后, 认证侧校验立即拒绝 `user_assertion` 登录 —— **无需删除** `public_key` 身份数据(留着也过不了认证), 其他后端**无需感知** `public_key` 的存在.

---

## 七. 凭证格式与算法

- **`user_assertion` = JWS**(RFC 7515): header `alg` + `kid`(登录, 可选) 或 `alg` + `jwk`(首次开通); payload `sub`(登录时携带 = 身份 `subject`; 首次开通时无) + `jti`(客户端生成唯一值) + `iat` + `aud`(token 端点).
- **携带 `sub` → 名副其实的 assertion**: 登录后 assertion 体内带 `sub` 身份声明, 是 RFC 7521 意义的 user assertion(区别于早期无 claim、仅靠外部反查的 proof 形态).
- **多算法**: EC P-256 / RSA / Ed25519; 单把公钥用一个 JWK(RFC 7517) 表达; 服务端按 `alg` 分派 nimbus 验签器(`ECDSAVerifier` / `RSASSAVerifier` / `Ed25519Verifier`; Ed25519 依赖既有 provider).
- **域分离(关键)**: `user_assertion` 的新鲜性**不复用** App Attest 客户端认证的 challenge —— 两层信任独立. `user_assertion` 靠自带 `jti` + `NonceService` 防重放(DPoP / client-attestation PoP JWT 同款范式), App Attest 侧靠它自己的一次性 challenge. 二者互不依赖, 避免"用户层新鲜性寄生于客户端认证机制"及 challenge 消费次序耦合.
- **无 `signCount`**: `jti` 防重放已足够, 不引入 App Attest / WebAuthn 的计数器.

---

## 八. 决策记录 (ADR 摘要)

| 议题 | 选择 | 理由 / 否决项 |
|---|---|---|
| 凭证选型 | 客户端自生成非对称密钥(静默零 UI) | 否决 passkey: 无法静默(iOS 必弹系统框 + Face ID), 与静默零 UI 诉求冲突 |
| 与 App Attest `kid` 关系 | 完全独立的另一把密钥 | 否决复用 `kid`: 客户端证明与用户证明角色冲突(正是 `@Deprecated app_assertion` 根因) |
| `grant_type` 名 | `user_assertion` | 登录后体内携 `sub`(账号用户名)声明, 是名副其实的 user assertion; `user_` 前缀避让既有各类 assertion(`client_assertion` / `OAuth-Client-Attestation-Assertion` / 废弃 `app_assertion`). 否决无 claim 时的 `*_assertion`(overclaim) / `key_proof`(`key` 撞 App Attest KEY 且重心在钥匙非用户) / `pop`(撞 `-PoP` 头) |
| `identity_type` 名 | `public_key` | 身份本体是一把公钥; 与 grant 名是正交维度, 不跟改 |
| 凭证参数名 | `user_assertion` | 与 grant 同名; OAuth 现状已存在大量 assertion, 加 `user_` 前缀消歧与防冲突(此处优先于 `authorization_code`→`code` 那种省字惯例) |
| 身份寻址 | 账号由 `sub`(用户名)路由 → `user_id` → 该账号 `public_key` 身份; 身份自有独立 `subject`(公钥派生) | 否决 `app_attest_kid` / `client_id`(身份域不耦合客户端认证域); 否决"身份标识 = 账号 `sub`"(一账号可多 `public_key` 身份, `identifier` 须逐身份不同)(见〈二.2〉) |
| 开通准入 | JitProvisioning 层**客户端认证机制白名单** | 否决放 OAuth grant 层: 注册入口未来未必是 token 端点; 下沉后与入口无关, 契合 resolver 既有契约(见〈五〉5.2) |
| `kid` 角色 | 账号多 `public_key` 身份时的登录选择器 / 审计; **非全局寻址键** | 早期"(app_attest_kid, kid) 复合键 + 客户端本地唯一"论述随解绑作废 |
| 新鲜性 | `user_assertion` 自带 `jti`+`iat`+`aud` + `NonceService` | 否决复用 App Attest challenge(两层需域分离) |
| `signCount` | 不用 | `jti` 已防重放 |
| 注册/登录 | 合并, 首用 auto-provision | 同 OTP |
| 密钥轮换 | **不支持**; 一身份仅一把 `jwk`, 创建后只读 | 否决"多公钥集合 + `PUT` 整体替换": 试用凭证一次性、低保证, 轮换收益不抵三级表 / 集合 diff 的复杂度; 丢钥即换新号、旧号孤儿(与〈九〉一致) |

---

## 九. 后果与边界

- **App Attest `kid` 吊销 → 重注册**: 身份**与 `app_attest_kid` 解绑**、账号由 `sub`(用户名)路由, 故 `keyId` 变化**不再影响**该身份 —— 只要客户端仍持有私钥与 `sub`, 重注册后照常登录. 早期"重注册即孤儿"的后果**已消除**(见〈二.2〉).
- **可恢复性**: 私钥不可导出; 登录需**同时**持有私钥(验签)与 `sub`(用户名, 路由到账号). 二者能否跨退出 / 重装留存取决于客户端的生命周期策略(默认 `session` → 退出即清、账号放弃; 可选保留, 见接入文档〈七〉); **换机 / 丢私钥**则必然孤儿(相较 passkey 的 iCloud 钥匙串同步). 服务端可由后台任务按长期未使用清理孤儿账号.
- **无轮换 → 换钥即换号**: 一身份仅一把 `jwk` 且只读. 客户端丢失私钥后只能生成**新密钥对**重来 → 无 `sub` 重走开通 → 静默 auto-provision **新**账号(新用户名), 旧号成孤儿.
- **滥用防护**: 开通受**已认证客户端**门槛 + JitProvisioning **客户端认证机制白名单**(见〈五〉5.2); 限频按"已认证客户端"这一泛化维度(不再绑 `app_attest_kid`), 防单机批量刷账号. 防刷强度取决于客户端认证机制强度, 故白名单应只放行强机制(如 App Attest 真机证明).
- **私钥生命周期**: 客户端默认按 `session` 处理 —— 退出登录即清除私钥(引用)与 `sub`, 匿名账号随之放弃(成孤儿), 下次登录静默开通得新 `sub`; 若要“退出后仍可找回原账号”, 可选把私钥 + `sub` 当 `persistent` 保留(进阶策略). 客户端侧细节见接入细节文档〈七〉.

---

## 十. 落地清单 (非本文强约束)

- 领域: `PublicKeyUserIdentityService`(含 `createUserIdentity` 独占校验; 登录经 `loadUserByUsername` + `listUserIdentities(userId, "public_key")` 路由, 无 `findByAppAttestKidAndKid`; `findUserIdentityByRawSubject("public_key", 公钥派生 subject)` 供开通期唯一性校验).
- 持久化: `t_user_identity_public_key` **1:1** 实体 + 仓储; 身份键 = 父表 `subject`(无复合唯一键); 可选 `origin_client_auth` 审计列.
- 授权服务: `EulerAuthorizationGrantType.USER_ASSERTION` + `user_assertion` 的 converter + provider(解析 `sub`、JWS 多算法验签、`jti`/`NonceService`、auto-provision、认证侧独占校验).
- JitProvisioning: `JitProvisioningPolicy.allowedClientAuthenticationMethods` + `ClientAuthenticationMechanismExtractor`(OAuth2 实现映射 `ClientAuthenticationMethod.getValue()`) + `JitProvisioningGuard`; 配置 `euler.security.identity-type.public_key.jit-provisioning.allowed-client-authentication-methods`.
- `public_key` 身份**只读**: 无轮换, **不接入** `PUT /user/identities/{identity_id}` 更新语义; 更换密钥走 `user_assertion` 重新开通.

---

## 十一. 相关文档

- [user_assertion 接入细节](App-Attest-Login-%23-User-Assertion.md) — 面向客户端的协议与行为(本文的对外契约面)
- [Apple App Attest 登录完整流程文档](App-Attest-Login.md) — 上位文档
- [Attestation Based Client Authentication (Apple App Attest)](../../OAuth2-Client-Authentication-%23-Attestation-Based-%23-Apple-App-Attest.md) — 客户端认证(开通准入门槛)
- [Apple App Attest 实例注册](../../App-Attest-Registration.md) — `AppAttestAttestationRegistration` 的产生
- [绑定用户身份](../../APIs-%23-User-Identities-Create.md) / [更新已绑定的用户身份](../../APIs-%23-User-Identities-Update.md) — 身份 CRUD(`public_key` 只读, 不经更新接口)
- [RFC 7515: JWS](https://datatracker.ietf.org/doc/html/rfc7515) / [RFC 7517: JWK](https://datatracker.ietf.org/doc/html/rfc7517)
