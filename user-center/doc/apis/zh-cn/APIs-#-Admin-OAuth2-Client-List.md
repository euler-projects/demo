# 列出客户端

按分页方式列出所有 OAuth 2.0 客户端. 通用约定见 [home.md](home.md#通用约定), 客户端完整结构定义见 [OAuth2 客户端](Model-%23-OAuth2-Client.md).

## Request

### Url

```http
GET /admin/oauth2/client?offset={offset}&limit={limit}
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### 查询参数

|参数名|类型|说明|是否必填|默认值|
|---|---|---|---|---|
|offset|int|起始偏移 (从 0 开始)|是|无|
|limit|int|本次最多返回条数|是|无|

## Response

**Success Response (200):**

响应体为数组, 每个元素是完整的 [OAuth2 客户端](Model-%23-OAuth2-Client.md) 模型.

```json
[
    {
        "registrationId": "5f9a1c2d-3b4e-4f6a-8d9c-1e2f3a4b5c6d",
        "clientId": "r3j8F/2pTqk0C9hZwN4vJQ==",
        "clientIdIssuedAt": 1777709730123,
        "clientSecret": null,
        "clientSecretExpiresAt": null,
        "clientName": "example-web-app",
        "tokenEndpointAuthMethod": "client_secret_basic",
        "grantTypes": ["authorization_code", "refresh_token"],
        "responseTypes": ["code"],
        "redirectUris": ["https://app.example.com/callback"],
        "postLogoutRedirectUris": ["https://app.example.com/"],
        "scopes": ["openid", "profile"],
        "jwksUri": null,
        "jwks": null,
        "tokenEndpointAuthSigningAlgorithm": null,
        "idTokenSignedResponseAlgorithm": null,
        "tlsClientAuthSubjectDN": null,
        "tlsClientCertificateBoundAccessTokens": false,
        "clientSettings": {
            "requireProofKey": true,
            "requireAuthorizationConsent": true,
            "settings": {
                "settings.client.require-proof-key": true,
                "settings.client.require-authorization-consent": true
            }
        },
        "tokenSettings": {
            "authorizationCodeTimeToLive": 300,
            "accessTokenTimeToLive": 300,
            "accessTokenFormat": "self-contained",
            "deviceCodeTimeToLive": 300,
            "reuseRefreshTokens": false,
            "refreshTokenTimeToLive": 604800,
            "settings": {
                "settings.token.authorization-code-time-to-live": 300,
                "settings.token.access-token-time-to-live": 300,
                "settings.token.access-token-format": "self-contained",
                "settings.token.device-code-time-to-live": 300,
                "settings.token.reuse-refresh-tokens": false,
                "settings.token.refresh-token-time-to-live": 604800
            }
        }
    }
]
```

数组元素结构参见 [OAuth2 客户端](Model-%23-OAuth2-Client.md).

> 此接口**不返回** `clientSecret` 的明文值 (字段始终为 `null`). 服务端仅保存密钥的哈希形式, 明文仅在[创建](APIs-%23-Admin-OAuth2-Client-Create.md)和[轮转](APIs-%23-Admin-OAuth2-Client-Rotate-Secret.md)时一次性返回.
