# 客户端密钥生命周期

描述 Admin OAuth2 客户端接口中 `clientSecret` 的生成策略、存储方式和暴露窗口. 通用约定见 [home.md](home.md#通用约定).

---

## 生成策略

* **算法**: `SecureRandom` 生成 32 字节 (256 bits) 熵, 使用 URL-safe Base64 (无填充) 编码
* **格式**: `euler_sk_<44 位 URL-safe base64>`, 例如 `euler_sk_9qZt3C9m1s6cPqz1nB7Vw8X2Yl3kU4oA5dJhI6pR7eS`
* **前缀语义**: `euler_sk_` 表示 "Euler Secret Key", 对齐主流 LLM 厂商的 API Key 命名 (如 `sk-`, `sk_live_`), 便于运维人员肉眼识别以及密钥扫描工具 (如 GitHub secret scanning) 建立规则

## 存储与暴露

* 服务端仅存储密钥的 **哈希值** (由配置的 `PasswordEncoder` 生成)
* 明文仅在以下两种响应中一次性返回:
  * [创建客户端](APIs-%23-Admin-OAuth2-Client-Create.md) 时
  * [轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md) 时
* [查询客户端](APIs-%23-Admin-OAuth2-Client-Get.md) 与 [列出客户端](APIs-%23-Admin-OAuth2-Client-List.md) 响应中 `clientSecret` 字段**始终为 `null`** (服务端序列化前会强制掩码), 防止哈希值泄露

## 生成条件

仅当 `tokenEndpointAuthMethod` 使用共享密钥时才会生成, 可选值见 [OAuth2 客户端 - tokenEndpointAuthMethod 枚举值](Model-%23-OAuth2-Client.md#tokenendpointauthmethod-枚举值). 其他认证方式 (如 `none` / `private_key_jwt` / `tls_client_auth` / `self_signed_tls_client_auth`) 创建时 `clientSecret` 为 `null`, 且轮转接口会返回 400.

## 过期策略

当前默认 `clientSecretExpiresAt = null`, 即**不过期**. 运维通过 [轮转客户端密钥](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md) 接口主动更换; 强制有限期的策略由服务端生成机制的后续版本提供.
