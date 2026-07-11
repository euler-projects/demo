# 创建密钥

服务端生成一对全新的非对称密钥并以 `PENDING` 状态写入密钥仓库. **私钥仅在服务端密钥仓库内以 AES-256-GCM 封套加密存储, 任何情况下都不会出现在响应体中**. 通用约定见 [home.md](home.md).

> 创建后的新密钥默认处于 `PENDING` 状态, 不会自动用于签名; 如需提升为签名密钥, 请调用 [变更密钥状态 (PATCH)](APIs-%23-Admin-JWKS-Patch.md) 将其状态设为 `ACTIVE`.

## Request

### Url

```http
POST /admin/api/oauth2/jwks
```

### Authorization

```http
Authorization: Bearer <access_token>
```

需要 `root` 或 `admin` 角色.

### Content Type

```
application/json
```

### 请求体字段

请求体为 [JWK 创建请求](Model-%23-Jwk-Key-Create-Request.md) 模型. 字段说明:

| PROPERTY  | TYPE   | REQUIRED | DESCRIPTION                                                                                |
|-----------|--------|----------|--------------------------------------------------------------------------------------------|
| algorithm | enum   | No       | JWS 算法, 取值 `RS256` / `RS384` / `RS512` / `ES256` / `ES384` / `ES512` / `EDDSA`; 缺省为 `ES256` |
| keySize   | number | No       | 仅 RSA 算法生效, 可选 `2048` / `3072` / `4096`; EC / EdDSA 算法忽略, 缺省为 `2048`              |

### 请求示例

#### 示例 1: 默认算法 (推荐)

省略 `algorithm`, 使用默认 `ES256` 椭圆曲线密钥. EC 密钥更短, 签名/验签更快, 适合绝大多数生产场景.

```json
{}
```

#### 示例 2: 指定 RSA 3072

需要与仅支持 RSA 的旧客户端互通时使用.

```json
{
  "algorithm": "RS256",
  "keySize": 3072
}
```

#### 示例 3: Ed25519 (需 BouncyCastle)

```json
{
  "algorithm": "EDDSA"
}
```

> 选用 `EDDSA` 时要求服务端 classpath 含 BouncyCastle (`bcpkix-jdk18on`), 否则将以 `500` 失败. RSA 密钥的 `keySize` 不在白名单 (2048 / 3072 / 4096) 内时将以 `500` 失败.

## Response

**Success Response (201 Created):**

响应体为 [JWK Key](Model-%23-Jwk-Key.md). 新建密钥保证: `status="PENDING"` 且 `hasPrivate=true`, `signing` 通常为 `false`.

```json
{
  "kid": "9f1b3e2c-7d20-4ac6-9e88-3f0a4b9f6d11",
  "alg": "ES256",
  "use": "sig",
  "status": "PENDING",
  "hasPrivate": true,
  "issuedAt": 1746288000000,
  "signing": false
}
```

> 创建成功后, 服务端会立即重建运行时状态并广播至集群; 该密钥同步出现在 `/.well-known/jwks.json` 供验证方预热缓存. 后续提升为签名密钥需手动调用 [变更密钥状态 (PATCH)](APIs-%23-Admin-JWKS-Patch.md) 将其状态设为 `ACTIVE`.
