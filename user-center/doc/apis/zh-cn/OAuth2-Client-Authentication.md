# OAuth2 Client Authentication

### `none` — 公共客户端

公共客户端 (Public Client) 不持有 client secret, 不在 `/oauth2/token` 上做客户端身份验证. 由于无法区分合法客户端与攻击者, **必须** 至少叠加以下增强手段之一:

* 使用 `authorization_code` + PKCE (`code_challenge` / `code_verifier`);
* 使用 `attest_jwt_client_auth` 作为附加的客户端证明 (`OAuth-Client-Attestation-*` 头).

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=authorization_code&client_id=default&code=...&code_verifier=...&redirect_uri=...
```

### `client_secret_basic` — HTTP Basic

通过 `Authorization: Basic base64(client_id:client_secret)` 头部传递客户端凭据 ([RFC6749 §2.3.1]). 机密客户端推荐使用.

请求示例:

```http
POST /oauth2/token
Authorization: Basic YWRtaW46YWRtaW4=
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=...
```

### `client_secret_post` — 请求体携带

通过请求体 `client_id` / `client_secret` 参数传递客户端凭据. 仅在无法使用 `client_secret_basic` 时使用, 安全性弱于 Basic 方式 (易出现在访问日志中).

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded

grant_type=refresh_token&refresh_token=...&client_id=admin&client_secret=...
```

### `attest_jwt_client_auth` — 基于设备证明的客户端认证

定义见 [OAuth 2.0 Attestation-Based Client Authentication (draft) §13.4]. 客户端通过 `OAuth-Client-Attestation` / `OAuth-Client-Attestation-PoP` 等请求头携带设备证明与 PoP 数据, 可作为独立认证方式, 也可叠加在其他标准认证方式之上作为安全增强.

请求示例:

```http
POST /oauth2/token
Content-Type: application/x-www-form-urlencoded
OAuth-Client-Attestation: <Client Attestation JWT>
OAuth-Client-Attestation-PoP: <PoP JWT>

grant_type=authorization_code&client_id=default&code=...&code_verifier=...
```

> 详细设计 (请求头语义、PoP 类型与载体、独立 / 增强两种使用场景、各认证方式与 PKCE 的组合约束) 较为复杂, 具体使用方式参考: [Attestation Based Client Auth 子文档](OAuth2-Client-Authentication-%23-Attestation-Based-Client-Auth.md) (_待补充_).

## 注意事项

* `client_secret_post` 仅在无法使用 `client_secret_basic` 时使用, 避免 secret 出现在访问日志中.
* `client_secret_basic` 的 client_id / client_secret 需先经 URL form-encode, 再以 `:` 拼接做 Base64 编码, 详见 [RFC6749 §2.3.1].

[RFC6749 §2.3.1]: https://datatracker.ietf.org/doc/html/rfc6749v#section-2.3.1
[OAuth 2.0 Attestation-Based Client Authentication (draft) §13.4]: https://datatracker.ietf.org/doc/html/draft-ietf-oauth-attestation-based-client-auth-08#section-13.4
