# 轮转客户端密钥

用于在密钥疑似泄露或到达运维周期时, 由服务端重新签发一份强随机密钥. 通用约定见 [home.md](home.md#通用约定); 密钥生成规则见 [客户端密钥生命周期](APIs-%23-Admin-OAuth2-Client-Secret-Lifecycle.md).

## Request

### Url

```http
POST /admin/oauth2/client/{registrationId}/client-secret
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 路径参数

|参数名|类型|说明|
|---|---|---|
|registrationId|string|要轮转密钥的客户端主键|

无请求体.

## Response

**Success Response (200):**

```json
{
    "clientSecret": "euler_sk_Kt1mPqz1nB7Vw8X2Yl3kU4oA5dJhI6pR7eS9qZt3C9m",
    "clientSecretExpiresAt": null
}
```

|属性名|类型|说明|
|---|---|---|
|clientSecret|string|新生成的一次性明文密钥, 格式见 [客户端密钥生命周期](APIs-%23-Admin-OAuth2-Client-Secret-Lifecycle.md#生成策略)|
|clientSecretExpiresAt|timestamp(3)|密钥过期时间, 沿用客户端原有值; `null` 表示不过期|

**Error Response (400):**

```json
{
    "error": "invalid_request",
    "error_description": "Client {id} does not use a shared secret (token_endpoint_auth_method=none)"
}
```

触发条件:

* 目标客户端的 `tokenEndpointAuthMethod` 不使用共享密钥 (如 `none` / `private_key_jwt` / `tls_client_auth` / `self_signed_tls_client_auth`)
* `registrationId` 不存在

> **重要**: 新密钥返回后, 旧密钥立即失效. 调用方必须先完成新密钥的分发和切换, 再执行本接口; 否则可能导致线上业务中断.
