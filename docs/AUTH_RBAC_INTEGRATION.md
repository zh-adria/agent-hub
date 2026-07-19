# 认证 / RBAC 外部系统集成契约

## 目标

AgentHub 不自建企业统一认证中心，也不维护组织、用户、角色的事实源。生产环境中，认证和 RBAC 由外部 IAM / SSO / 权限平台提供，AgentHub 只消费身份、租户、角色、权限决策，并在自身资源访问时强制执行。

## 支持的外部身份提供方

| Provider | 激活配置 | 说明 |
| --- | --- | --- |
| Logto Cloud (推荐生产) | `agenthub.identity.provider=logto` | OIDC + Organization RBAC，JWKS 离线校验 + Management API 拉取权限 |
| 自建 IAM | `agenthub.identity.provider=iam` | 兼容原有本地 IAM，introspection + /rbac/authorize |
| Mock (本地开发) | `agenthub.identity.provider=mock` | 内置多角色测试 token |

## Logto Cloud 集成（生产方案）

### 架构

```
  Frontend                    AgentHub Backend               Logto Cloud
     |                             |                            |
     |--- 1. redirect to --------->|                            |
     |    /api/auth/logto/          |                            |
     |    authorize                 |                            |
     |                             |--- 2. redirect ----------->|
     |                             |    to Logto /oidc/auth     |
     |                             |                            |
     |<-- 3. login in Logto -------|                            |
     |                             |                            |
     |<-- 4. Logto redirect -------|                            |
     |    to /api/auth/logto/       |                            |
     |    callback?code=xxx         |                            |
     |                             |                            |
     |--- 5. POST /oidc/token ----->|                            |
     |    (exchange code)           |                            |
     |                             |                            |
     |<-- 6. access_token ----------|                            |
     |                             |                            |
     |--- 7. Bearer token --------->|                            |
     |    for API requests          |                            |
     |                             |--- 8. JWKS validate ----->|
     |                             |    (local, no network)     |
     |                             |                            |
     |                             |--- 9. GET /api/org-roles -->|
     |                             |    (M2M token)              |
     |                             |                            |
     |<-- 10. RBAC result ----------|                            |
```

### Logto Cloud 前置配置

1. **创建 Machine-to-Machine (M2M) Application**
   - Logto Console → Applications → Create Application → Machine-to-Machine
   - API Resource 选择（或创建）包含 Organization Roles Management 权限的资源
   - 记录 `clientId` 和 `clientSecret`

2. **创建 Organization（对应 AgentHub tenant）**
   - Logto Console → Organizations → Create Organization
   - Organization ID 即为 AgentHub 的 `tenantId`
   - 例：`tenant-001`、`tenant-002`

3. **创建 Organization Roles**
   - 在 Organization 内定义 Roles，每个 Role 分配 Scopes
   - Scopes 即 AgentHub permissions：
     ```
     agent:create  agent:read  agent:update  agent:delete
     function:create  function:read  function:invoke
     session:create  session:read  session:message  session:delete
     knowledge:create  knowledge:read  knowledge:search
     workflow:read  workflow:execute
     ```
   - 也可自定义 role code（如 `agent_admin`）作为权限标识

4. **分配用户到 Organization**
   - 将用户加入对应 Organization 并赋予 Roles

### AgentHub 配置

```yaml
agenthub:
  identity:
    provider: logto
    logto:
      issuer: https://your-tenant.logto.app       # Logto Cloud 实例地址
      client-id: ${AGENTHUB_LOGTO_CLIENT_ID}      # M2M Application Client ID
      client-secret: ${AGENTHUB_LOGTO_CLIENT_SECRET}
      expected-audience: ${AGENTHUB_LOGTO_EXPECTED_AUDIENCE:}  # 可选，默认用 client-id
      enable-userinfo-fallback: true
      fallback-to-scope-only: false
      jwks-cache-ttl-ms: 3600000                  # 1 hour
      management-api-base-url: ${AGENTHUB_LOGTO_MGMT_API:}      # 可选
```

环境变量：
```bash
export AGENTHUB_LOGTO_ISSUER=https://your-tenant.logto.app
export AGENTHUB_LOGTO_CLIENT_ID=your-m2m-client-id
export AGENTHUB_LOGTO_CLIENT_SECRET=your-m2m-client-secret
export AGENTHUB_LOGTO_EXPECTED_AUDIENCE=https://your-tenant.logto.app/api
```

### Token 结构

Logto 签发的 JWT access token 包含以下关键 claims：

| Claim | 说明 | AgentHub 映射 |
| --- | --- | --- |
| `sub` | Logto 用户 ID (UUID) | `userId` |
| `name` / `preferred_username` | 用户显示名 | `username` |
| `org_id` | 当前 token 关联的 Organization ID | `tenantId` |
| `scope` | 空格分隔的 scope 列表 | `permissions`（兜底） |
| `roles` (custom) | 用户在 org 中的角色名 | `roles` |

### Logto API 端点映射

| AgentHub 能力 | Logto Cloud 端点 | 方式 |
| --- | --- | --- |
| Token 校验 | `GET {issuer}/oidc/jwks` | 本地 JWKS 离线校验（零调用） |
| Token 校验（兜底） | `POST {issuer}/oidc/introspect` | 按需 fallback |
| 用户信息 | `GET {issuer}/oidc/userinfo` | 补充 org_id（仅当 JWT 无此 claim） |
| 权限判定 | `GET {mgmt}/api/organization-roles?orgId=xxx&userId=yyy` | M2M token 调用 |
| OAuth2 登录 | `GET {issuer}/oidc/auth` → `/api/auth/logto/callback` | 前端 OAuth2 flow |
| Token 撤销 | `POST {issuer}/oidc/revoke` | 登出时 |

### RBAC 流程

1. 请求到达，`RequestContextFilter` 提取 `Authorization: Bearer <token>`
2. `LogtoIdentityService.introspect()` 用 JWKS 本地校验 JWT
3. 从 JWT claims 提取 `sub`(userId)、`org_id`(tenantId)、`scope`(permissions)
4. 调用 `LogtoOrganizationRoleMapper.getPermissions(orgId, userId)`
   - 使用 M2M client_credentials 获取 management access token
   - 调用 Logto Management API 拉取用户在 Organization 中的所有 Organization Roles
   - 聚合所有 Role 的 scopes 作为 permissions
5. 构造 `AuthenticatedPrincipal` 存入 `AuthContext`
6. `RbacInterceptor` 根据 URL path/method 映射为 action，调用 `authorize()` 判定

### 多租户（Multi-Organization）支持

- 每个 Logto Organization 对应一个 AgentHub tenant
- 同一用户可属于多个 Organization
- 用户需在请求头中指定当前租户：`X-Tenant-Id: tenant-001`
- `RequestContextFilter` 校验 `X-Tenant-Id` 是否在用户的 tenant 列表中

### OAuth2 Authorization Code Flow

**前端登录流程：**

```javascript
// 1. 获取授权 URL
const res = await fetch('/api/auth/logto/authorize?redirectUri=https://app.example.com/callback&state=xyz');
const { authorizeUrl } = await res.json();
// 跳转
window.location.href = authorizeUrl;

// 2. 用户在 Logto 登录后，Logto 重定向到配置的 redirectUri
//    携带 ?code=xxx&state=xyz

// 3. 前端将 code 传给后端（或由后端直接处理 callback）
//    后端端点：GET /api/auth/logto/callback?code=xxx
//    返回 { access_token, id_token, refresh_token, token_type, expires_in }

// 4. 前端存储 access_token，后续请求携带：
//    Authorization: Bearer <access_token>
```

### 前端 Logto SDK 集成（推荐）

```typescript
// 使用 @logto/react 前端 SDK
import { LogtoProvider, LogtoClient } from '@logto/react';

const logtoConfig = {
  endpoint: 'https://your-tenant.logto.app',
  appId: 'your-spa-app-id',
  scopes: ['openid', 'profile', 'email', 'organization'],
};

// 在 App 外层包裹 LogtoProvider
<LogtoProvider config={logtoConfig}>
  <App />
</LogtoProvider>

// 获取 token 后调用 AgentHub API
const token = await logtoClient.getAccessToken();
fetch('/api/agents', {
  headers: { Authorization: `Bearer ${token}` }
});
```

## 外部系统需要提供的接口

## 本地 Mock 接口

本地开发环境已在 AgentHub 后端提供 mock 版本，统一前缀为 `/mock`。

| 能力 | Mock 接口 |
| --- | --- |
| JWKS | `GET /mock/.well-known/jwks.json` |
| Token introspection | `POST /mock/oauth2/introspect` |
| 用户信息 | `GET /mock/userinfo` |
| 权限判定 | `POST /mock/rbac/authorize` |
| 用户角色 | `GET /mock/rbac/users/{userId}/roles?tenantId=tenant-001` |
| 角色权限 | `GET /mock/rbac/roles/{roleCode}/permissions?tenantId=tenant-001` |
| 租户信息 | `GET /mock/tenants/{tenantId}` |
| 用户租户列表 | `GET /mock/users/{userId}/tenants` |

这些接口只用于本地联调和前端/后端集成测试，不作为真实认证源。

本地可用 token：

| Token | 用户 | 租户 | 权限 |
| --- | --- | --- | --- |
| `mock-token` | `user-001` | `tenant-001`、`tenant-002` | 管理员全量权限 |
| `reader-token` | `user-reader` | `tenant-001` | 只读权限 |
| `knowledge-token` | `user-knowledge` | `tenant-001` | 知识库编辑权限 |
| `tenant-002-token` | `user-002` | `tenant-002` | tenant-002 管理员权限 |
| `expired` | anonymous | - | inactive token |

### 1. Token 校验

用于校验前端或调用方传入的访问令牌。

推荐方式一：JWKS。

```
GET /.well-known/jwks.json
```

AgentHub 用该接口获取公钥，离线校验 JWT 签名。

推荐方式二：Token introspection。

```
POST /oauth2/introspect
```

请求：

```json
{
  "token": "access_token"
}
```

响应：

```json
{
  "active": true,
  "sub": "user-001",
  "tenantId": "tenant-001",
  "username": "zhangsan",
  "roles": ["agent_admin"],
  "permissions": ["agent:create", "agent:read"]
}
```

### 2. 用户信息

用于获取当前用户基础信息。

```
GET /userinfo
```

请求头：

```
Authorization: Bearer <access_token>
```

响应：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "username": "zhangsan",
  "displayName": "张三",
  "email": "zhangsan@example.com"
}
```

### 3. 权限判定

用于细粒度判断某个用户是否允许执行某个动作。

```
POST /rbac/authorize
```

请求：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "resourceType": "agent",
  "resourceId": "123",
  "action": "agent:update"
}
```

响应：

```json
{
  "allowed": true,
  "reason": "role agent_admin grants agent:update"
}
```

### 4. 用户角色查询

用于页面控制和后端缓存用户角色。

```
GET /rbac/users/{userId}/roles?tenantId=tenant-001
```

响应：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "roles": ["agent_admin", "knowledge_editor"]
}
```

### 5. 角色权限查询

用于启动时或缓存刷新时同步权限定义。

```
GET /rbac/roles/{roleCode}/permissions?tenantId=tenant-001
```

响应：

```json
{
  "role": "agent_admin",
  "permissions": [
    "agent:create",
    "agent:read",
    "agent:update",
    "agent:delete",
    "session:read"
  ]
}
```

### 6. 租户信息查询

用于校验租户是否存在、启用、允许访问 AgentHub。

```
GET /tenants/{tenantId}
```

响应：

```json
{
  "tenantId": "tenant-001",
  "name": "示例企业",
  "status": "ACTIVE",
  "features": ["agent", "rag"]
}
```

### 7. 用户租户列表

用于多租户用户选择当前租户。

```
GET /users/{userId}/tenants
```

响应：

```json
{
  "userId": "user-001",
  "tenants": [
    { "tenantId": "tenant-001", "name": "示例企业" },
    { "tenantId": "tenant-002", "name": "测试组织" }
  ]
}
```

## 外部系统需要提供的接口

## 本地 Mock 接口

本地开发环境已在 AgentHub 后端提供 mock 版本，统一前缀为 `/mock`。

| 能力 | Mock 接口 |
| --- | --- |
| JWKS | `GET /mock/.well-known/jwks.json` |
| Token introspection | `POST /mock/oauth2/introspect` |
| 用户信息 | `GET /mock/userinfo` |
| 权限判定 | `POST /mock/rbac/authorize` |
| 用户角色 | `GET /mock/rbac/users/{userId}/roles?tenantId=tenant-001` |
| 角色权限 | `GET /mock/rbac/roles/{roleCode}/permissions?tenantId=tenant-001` |
| 租户信息 | `GET /mock/tenants/{tenantId}` |
| 用户租户列表 | `GET /mock/users/{userId}/tenants` |

这些接口只用于本地联调和前端/后端集成测试，不作为真实认证源。

本地可用 token：

| Token | 用户 | 租户 | 权限 |
| --- | --- | --- | --- |
| `mock-token` | `user-001` | `tenant-001`、`tenant-002` | 管理员全量权限 |
| `reader-token` | `user-reader` | `tenant-001` | 只读权限 |
| `knowledge-token` | `user-knowledge` | `tenant-001` | 知识库编辑权限 |
| `tenant-002-token` | `user-002` | `tenant-002` | tenant-002 管理员权限 |
| `expired` | anonymous | - | inactive token |

### 1. Token 校验

用于校验前端或调用方传入的访问令牌。

推荐方式一：JWKS。

```
GET /.well-known/jwks.json
```

AgentHub 用该接口获取公钥，离线校验 JWT 签名。

推荐方式二：Token introspection。

```
POST /oauth2/introspect
```

请求：

```json
{
  "token": "access_token"
}
```

响应：

```json
{
  "active": true,
  "sub": "user-001",
  "tenantId": "tenant-001",
  "username": "zhangsan",
  "roles": ["agent_admin"],
  "permissions": ["agent:create", "agent:read"]
}
```

### 2. 用户信息

用于获取当前用户基础信息。

```
GET /userinfo
```

请求头：

```
Authorization: Bearer <access_token>
```

响应：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "username": "zhangsan",
  "displayName": "张三",
  "email": "zhangsan@example.com"
}
```

### 3. 权限判定

用于细粒度判断某个用户是否允许执行某个动作。

```
POST /rbac/authorize
```

请求：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "resourceType": "agent",
  "resourceId": "123",
  "action": "agent:update"
}
```

响应：

```json
{
  "allowed": true,
  "reason": "role agent_admin grants agent:update"
}
```

### 4. 用户角色查询

用于页面控制和后端缓存用户角色。

```
GET /rbac/users/{userId}/roles?tenantId=tenant-001
```

响应：

```json
{
  "userId": "user-001",
  "tenantId": "tenant-001",
  "roles": ["agent_admin", "knowledge_editor"]
}
```

### 5. 角色权限查询

用于启动时或缓存刷新时同步权限定义。

```
GET /rbac/roles/{roleCode}/permissions?tenantId=tenant-001
```

响应：

```json
{
  "role": "agent_admin",
  "permissions": [
    "agent:create",
    "agent:read",
    "agent:update",
    "agent:delete",
    "session:read"
  ]
}
```

### 6. 租户信息查询

用于校验租户是否存在、启用、允许访问 AgentHub。

```
GET /tenants/{tenantId}
```

响应：

```json
{
  "tenantId": "tenant-001",
  "name": "示例企业",
  "status": "ACTIVE",
  "features": ["agent", "rag"]
}
```

### 7. 用户租户列表

用于多租户用户选择当前租户。

```
GET /users/{userId}/tenants
```

响应：

```json
{
  "userId": "user-001",
  "tenants": [
    { "tenantId": "tenant-001", "name": "示例企业" },
    { "tenantId": "tenant-002", "name": "测试组织" }
  ]
}
```

## AgentHub 需要暴露/消费的约定

### 请求头

前端或调用方访问 AgentHub API 时必须带：

```
Authorization: Bearer <access_token>
```

多租户用户需要显式传当前租户：

```
X-Tenant-Id: tenant-001
```

AgentHub 必须校验 `X-Tenant-Id` 是否属于 token 对应用户。

### 权限动作命名

建议最小权限集合：

| 资源 | 动作 |
| --- | --- |
| Agent | `agent:create`、`agent:read`、`agent:update`、`agent:delete` |
| Function | `function:create`、`function:read`、`function:update`、`function:delete`、`function:invoke` |
| Session | `session:create`、`session:read`、`session:message`、`session:delete` |
| KnowledgeBase | `knowledge:create`、`knowledge:read`、`knowledge:update`、`knowledge:delete`、`knowledge:search` |
| Audit | `audit:read` |

## 租户 enforcement 是什么

租户 enforcement 指：AgentHub 在每一次资源读写时，都必须强制按租户隔离数据，不能只在页面或查询参数上“看起来区分租户”。

具体要求：

1. 从已认证 token 和 `X-Tenant-Id` 解析当前租户上下文。
2. 创建资源时，后端写入当前 `tenant_id`，不能相信前端传入的 `tenantId`。
3. 查询资源时，所有 repository 查询必须带 `tenant_id` 条件。
4. 更新、删除、调用资源前，必须校验该资源属于当前租户。
5. 用户即使猜到其他租户的资源 ID，也只能得到 `404` 或 `403`。
6. 异步任务、审计日志、RAG 检索、会话消息也必须携带并校验 `tenant_id`。
7. 缓存 key 必须包含 `tenantId`，避免跨租户缓存污染。

简单说：租户 enforcement 是“后端强制隔离”，不是“前端下拉框过滤”。

## AgentHub 后续实现任务

- 已添加认证过滤器：解析 mock token、构造请求身份上下文
- 已添加基础租户上下文：`TenantContext`
- 已对关键 API 增加 RBAC 拦截器
- 已增加 mock 契约、401/403、跨租户访问测试
- 已实现 Logto Cloud 集成（`LogtoIdentityService`）
  - JWKS 本地 JWT 签名校验 + Nimbus JOSE+JWT
  - Organization Roles Management API 拉取 RBAC 数据
  - M2M client_credentials 获取 management access token
  - Logto OAuth2 authorization code callback 端点
  - 多 Organization 多租户映射（org_id → tenantId）
- 已清理核心写路径中的硬编码创建用户和前端 `userId` 信任
- 所有写操作忽略前端传入 tenantId，统一使用请求上下文
