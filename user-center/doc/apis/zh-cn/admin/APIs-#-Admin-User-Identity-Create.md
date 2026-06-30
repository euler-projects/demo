# 创建登录身份

为指定用户直接创建一条新的登录身份记录, **无需任何校验** (不检查唯一性、手机号/邮箱格式等). 通用约定见 [home.md](home.md#通用约定), 登录身份完整结构定义见 [User Identity](Model-%23-User-Identity.md).

> **支持的 `identityType`**: 本接口仅支持 `phone` 和 `email` 类型. 其他类型 (如 `wechat` / `apple` / `google`) 由各自的认证流程自动创建, 不支持通过本接口手动写入.

## Request

### Url

```http
POST /admin/api/users/{userId}/identities
```

### Authorities

需要以下任一权限: `root`, `admin`.

### Content Type

```
application/json
```

### 路径参数

|参数名|类型|说明|
|---|---|---|
|userId|string|目标用户 ID (UUID)|

### 请求体字段

请求体为 [User Identity](Model-%23-User-Identity.md) 模型的子集, 仅接受非只读字段: `identityType` 以及对应 `identityType` 的扩展字段 (与通用字段平铺在同一层级). 详细语义请参阅 [User Identity](Model-%23-User-Identity.md) 模型.

> 只读字段 (`identityId` / `userId` / `subject` / `boundAt`) 由服务端管理, 请求体中出现会被忽略.

> **注意**: 本接口为管理端直接写入, 不做任何业务校验 (如 subject 全局唯一性、手机号格式、邮箱格式等). 调用方需自行保证数据正确性.

### 请求示例

#### 示例 1: 绑定手机号身份

```json
{
    "identityType": "phone",
    "phone": "+8613800138000"
}
```

服务端将持久化原文并自动计算 `subject`; 响应中 `phone` 以脱敏形式返回.

#### 示例 2: 绑定邮箱身份

```json
{
    "identityType": "email",
    "email": "user@example.com"
}
```

服务端将持久化原文并自动计算 `subject`.

## Response

**Success Response (200):**

响应体为完整的 [User Identity](Model-%23-User-Identity.md) 模型, 包含服务端分配的 `identityId`、计算得到的 `subject` 和自动生成的 `boundAt`.

```json
{
    "identityId": "550e8400-e29b-41d4-a716-446655440000",
    "userId": "a1b2c3d4-e5f6-7890-abcd-ef1234567890",
    "identityType": "phone",
    "subject": "9c1b8e2a3f6d7e4b5a8c0d1e2f3a4b5c6d7e8f9a0b1c2d3e4f5a6b7c8d9e0f1a",
    "boundAt": 1778899139687,
    "phone": "+8613*******00"
}
```

> **敏感字段脱敏**: 响应中的敏感扩展字段 (如 `phone` / `email`) 以脱敏形式返回, 即使创建时提交了原文. 如需查询原文, 请使用 [查询脱敏字段原文](APIs-%23-Admin-User-Identity-Get-Raw-Field.md).
