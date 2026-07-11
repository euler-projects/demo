# JWK 创建请求

[创建密钥](APIs-%23-Admin-JWKS-Create.md) 接口的请求体模型. 描述「请服务端生成何种新密钥」, **不携带任何密钥材料**.

```json
{
  "algorithm": "ES256",
  "keySize": null
}
```

| PROPERTY  | TYPE   | REQUIRED | DESCRIPTION                                                                                |
|-----------|--------|----------|--------------------------------------------------------------------------------------------|
| algorithm | enum   | No       | JWS 算法名, 见下方 [枚举值对照表](#algorithm-枚举值); 缺省为 `ES256`                          |
| keySize   | number | No       | 仅 RSA 算法生效, 可选 `2048` / `3072` / `4096`; EC / EdDSA 算法忽略, 缺省 `2048`              |

> 本模型仅用于请求体. 创建成功后返回的密钥视图见 [JWK Key](Model-%23-Jwk-Key.md).

---

## `algorithm` 枚举值

| VALUE   | KEY TYPE | CURVE / SIZE                  | DESCRIPTION                                                  |
|---------|----------|-------------------------------|--------------------------------------------------------------|
| `RS256` | RSA      | `keySize` (2048 / 3072 / 4096) | RSASSA-PKCS1-v1_5 + SHA-256, 兼容性最好                       |
| `RS384` | RSA      | `keySize` (2048 / 3072 / 4096) | RSASSA-PKCS1-v1_5 + SHA-384                                  |
| `RS512` | RSA      | `keySize` (2048 / 3072 / 4096) | RSASSA-PKCS1-v1_5 + SHA-512                                  |
| `ES256` | EC       | secp256r1 (P-256)             | ECDSA + SHA-256, **推荐默认值**, 密钥短/性能好                 |
| `ES384` | EC       | secp384r1 (P-384)             | ECDSA + SHA-384                                              |
| `ES512` | EC       | secp521r1 (P-521)             | ECDSA + SHA-512                                              |
| `EDDSA` | OKP      | Ed25519                       | EdDSA (RFC 8037), JOSE 头记作 `alg: EdDSA`; **需 BouncyCastle** |

> EC / EdDSA 算法的曲线由算法本身固定, 因此 `keySize` 字段对它们无意义并被忽略. RSA 默认 `keySize=3072` 在安全性与性能间取得平衡.
