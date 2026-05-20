# AppAttestScaffold

iOS 业务接入脚手架，对接 [`euler-uc` user-center](../user-center) 的 App Attest 登录与 OTP 体系，开发者可直接基于此工程继续叠加业务能力。

> 与同目录的 `app-attest-demo` 不同：本工程按"脚手架"标准实现，分层清晰、配置集中、UI 风格对齐 iOS 系统 APP。

## 协议参考

- [`App-Attest-Login.md`](../user-center/doc/apis/zh-cn/App-Attest-Login.md)
- [`App-Attest-Login-#-OTP.md`](../user-center/doc/apis/zh-cn/App-Attest-Login-#-OTP.md)

## 已实现能力

- 匿名登录（App Attestation usage 1）
- 手机号 OTP 登录（App Attestation usage 2）
- App Assertion 续期（透明触发；提前 60s 刷新）
- 手机号绑定 / 解绑（仅 `phone` 登录身份，未实现 email/wechat）
- Keychain 持久化：`access_token` / `kid` / 最近账号
- 卸载重装首启检测（`FirstLaunchFlag`）自动清理 Keychain 残留
- 运行时切换服务配置（首页/登录页 → 服务配置），同时维护 issuer 与 Account Service 基地址

## 目录结构

本工程严格按「授权域 (Auth) / 账号域 (Account) / 协议层 (Core)」三段分层，便于在生产中替换或独立部署其中任一服务。

```
AppAttestScaffold/
  App/                          # @main 入口、AppSession 编排、根路由
  Core/
    Configuration/              # AppConfiguration: issuer / accountServiceBaseURL / scope / 常量
    Networking/                 # HTTPClient + APIError + form-urlencoded 编码
    Discovery/                  # OIDCDiscoveryService: 仅做 metadata 拉取与缓存
    DeviceAttest/               # AppAttestService: 包装 DCAppAttestService
    Storage/                    # Keychain CRUD + Session/Account 持久化
  Auth/                         # 授权域：与 Authorization Server 直接通信
    Endpoints/                  # AuthorizationServerEndpoints + Provider（基于 OIDC Discovery 派生）
    Models/                     # TokenBundle
    Clients/                    # OAuthClient / OTPClient / UserInfoClient（端点由 Provider 注入）
    AuthService.swift           # 编排：anonymousSignIn / signInWithPhoneOTP / refreshIfNeeded / wipeSession
  Account/                      # 账号域：与 Account Service 直接通信，可独立部署
    Endpoints/                  # AccountServiceEndpoints（同步派生，不依赖 Discovery）
    Models/                     # Account / Identity（登录身份模型）
    Clients/                    # UserIdentitiesClient（端点由闭包注入）
    AccountService.swift        # 编排：listIdentities / bindPhone / unbind（接收外部 access_token）
  Components/                   # AvatarView / PhoneNumberField / OTPCodeField
  Features/
    Login/                      # 启动页（匿名进入 + 手机登录入口）
    Home/                       # 主页（NavigationStack + 工具栏头像）
    Settings/                   # 设置页 + BindPhoneView（绑定手机）
  Resources/                    # Assets.xcassets / Preview Content
  Info.plist
```

### 分层依赖图

```
         ┌──────────────────────────┐
         │     AppSession (UI)       │
         │  编排 auth + account 流程  │
         └─────────┬────────────┬────┘
                   │            │
         ┌─────────▼──┐   ┌─────▼──────────┐
         │ AuthService │   │ AccountService │
         │ (授权域)     │   │ (账号域)       │
         └────┬────────┘   └────┬───────────┘
              │                  │
   ┌──────────▼──────────┐   ┌──▼──────────────┐
   │ AuthorizationServer  │   │ AccountService    │
   │ Endpoints (Provider) │   │ Endpoints         │
   └──────────┬──────────┘   └──┬──────────────┘
              │                  │
   ┌──────────▼──────────┐       │
   │ OIDCDiscoveryService │       │
   │ (metadata 缓存)       │       │
   └─────────────────────┘       │
              │                  │
              ▼                  ▼
     /.well-known/* + token   /user/identities*
```

- `AccountService` 不感知 App Attest / Assertion，只接收外部传入的 `access_token`。token 续期由 `AppSession` 调用 `AuthService.refreshIfNeeded()` 完成。
- `AccountServiceEndpoints` 同步派生（`AccountServiceEndpoints.current()`），不依赖 OIDC Discovery —— 账号服务可与授权服务部署在不同域。
- `OIDCDiscoveryService` 退化为纯协议层 metadata 服务，仅 `Auth/` 使用。

## 运行环境

- Xcode 15+ / iOS 17+
- 真机调试：必须在 Apple Developer 账号下开启 App Attest capability，并替换工程的 `Team` 与 `Bundle Identifier`
- 模拟器：App Attest 仅在受支持的真机上可用，模拟器调用会返回 `appAttestNotSupported`

## 配置

绝大多数运行时配置位于 [`AppConfiguration.swift`](AppAttestScaffold/Core/Configuration/AppConfiguration.swift)：

| 项 | 含义 | 是否运行时可改 |
| --- | --- | --- |
| `defaultIssuer` | 首启时使用的 issuer | 服务配置弹框可覆盖, 持久化在 UserDefaults |
| `defaultAccountServiceBaseURL` | 首启时使用的账号服务基地址 (`/user/identities` 寻址根) | 服务配置弹框可覆盖, 持久化在 UserDefaults |
| `defaultScope` | `/oauth2/token` 默认 scope, 必含 `openid` | 否 |
| `attestationType` | `apple_app_attest` | 否 |
| `appAssertionGrantType` / `otpGrantType` | OAuth grant_type 字符串 | 否 |
| `otpChannelSMS` | OTP 渠道标识 | 否 |

修改 issuer 或 Account Service 基地址会清空当前会话并强制重新登录（详见 `AppSession.updateServiceConfiguration(issuer:accountServiceBaseURL:)`）。

## 状态机

`AppSession.Phase`：

```
bootstrapping ──► unauthenticated ──► signedIn(AuthResult)
                       ▲                     │
                       └────── signOut ──────┘
```

- 启动期 `AppAttestScaffoldApp.init` 先调用 `FirstLaunchFlag.bootstrapIfNeeded()`，再构造 `AppSession`
- `AppSession.bootstrap()` 调用 `AuthService.resumeFromStorage()` 决定恢复会话还是要求登录；恢复成功后再请求 `AccountService.listIdentities` 拼装 `AuthResult`
- 任意 OAuth 调用拿到 `kid_revoked` / `attestation_key_revoked` 都会自动 `AuthService.wipeSession()`

### 域内/域外模型边界

| 模型 | 所属域 | 含义 |
| --- | --- | --- |
| `TokenBundle` | Auth | OAuth 令牌三元组 + 过期时间 |
| `AuthSession` | Auth | `tokens + profile + appAttestKey`，`AuthService` 唯一对外产物 |
| `Account.Profile` / `Account.AppAttestKey` | Account | 持久化在 Keychain 的账号侧元数据 |
| `Identity` | Account | `/user/identities` 返回的登录身份记录 |
| `AuthResult` | App (UI 聚合) | `tokens + profile + identities`，供视图渲染 |

## 扩展指引

| 想增加 ... | 入口 |
| --- | --- |
| email 绑定 | 复用 `OTPClient`, 在 `UserIdentitiesClient` 增加 `bindEmail`, `AccountService` 暴露 `bindEmail`, `AppSession` 编排 `refreshIfNeeded → bindEmail → listIdentities`, UI 仿 `BindPhoneView` |
| WeChat 绑定 | 在 `OAuthClient` 增加 wechat code 交换, `Identity.IdentityType` 已有 `.wechat` |
| 新业务资源端点 | 在 `Account/Endpoints/AccountServiceEndpoints.swift` 增加 URL 派生, 新建 `Account/Clients/<X>Client.swift` 注入 endpoints, 由 `AccountService` 编排, `AppSession` 负责 access_token 续期 |
| 第三方业务服务 | 与上同理：在专属域目录（如 `Billing/Endpoints/`）单独定义 endpoints + clients + service, 与 `Account/` 平级 |
| 自定义 401 重试 | `HTTPClient` 不处理 401, 拦截在 service 层：`try await auth.refreshIfNeeded()` 后重发 |
| 第三方头像/昵称 | `Account.Profile` 已预留 `nickname` / `avatarUrl`, 后端补齐字段后 `UserInfoClient` 自动拾取 |
| 国际手机号 | `PhoneNumberField` 内置 `E164Phone.Country` 抽象, 详见下文 |

### 国际化手机号扩展

当前 `PhoneNumberField` 仅内置 `E164Phone.china`（+86, 11 位, 1 开头, 显示分组 `3-4-4`）。要支持更多国家有两条路径：

1. **轻量做法 — 继续用内置抽象**：在 `Components/PhoneNumberField.swift` 中扩充 `E164Phone` 的静态常量（例如 `.hongKong`, `.unitedStates`），各自填好 `dialCode` / `maxLength` / `placeholder` / `isValid` / `format` 闭包，配合一个国家选择器 UI 让 `country` 字段从 `let` 改 `@State`。适合校验规则不复杂的少数地区。
2. **标准做法 — 接入 [PhoneNumberKit](https://github.com/marmelroy/PhoneNumberKit)**（Swift 版 libphonenumber）：
   - 在 Xcode 工程中添加 SPM 依赖 `PhoneNumberKit`
   - 把 `E164Phone.Country` 的 `isValid` / `format` 闭包替换为 `PhoneNumberUtility.parse(_:withRegion:)` 与 `format(_:toType: .e164/.international)`
   - 调用方（`PhoneNumberField` 用法、`PhoneOTPSheet` 的 `isPhoneValid` 联动）无需改动

`PhoneNumberField` 输出的 `e164` binding 永远是规范化的 `+<dialCode><digits>`（无空格、无分隔符），可直接传给 `OTPClient.sendOTP` 与 `AuthService.signInWithPhoneOTP`，与 `format` 实现无关。

## 已知限制

- 不实现 OTP 绑定的 Method B Conflict Token 流程（仅 Method A: 提示用户换号）
- 仅支持单账号 Keychain 存储；多账号切换需要扩展 `AccountStore`
- 模拟器无法完成首次注册（依赖 App Attest），调试时建议用真机

## 调试日志

DEBUG 构建会通过 `os.Logger` 输出所有 HTTP 请求 / 响应 / 耗时：Release 构建编译期剔除。

- subsystem: bundle id（默认 `com.eulerframework.appattest.scaffold`）
- category: `HTTPClient`
- Xcode Console 中可直接看到 `→` / `←` / `×` 三种前缀；Console.app 可过滤
- 敏感字段默认脱敏：`Authorization` / `OAuth-Client-Attestation*` header，以及 body 中 `client_assertion` / `assertion` / `attestation` / `access_token` / `refresh_token` / `id_token` / `otp` / `password` / `client_secret` / `code` / `challenge` / `client_data_hash` 字段会被截取为前 6 位 + 长度
- 需要可见完整字段调试时，临时修改 [HTTPClient.swift](AppAttestScaffold/Core/Networking/HTTPClient.swift) 中 `HTTPRequestLog.sensitiveBodyKeys` / `sensitiveHeaderKeys`

## License

跟随仓库主 [`LICENSE`](../LICENSE)。
