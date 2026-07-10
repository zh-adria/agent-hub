# 认证 / RBAC 外部系统集成契约

## 目标

AgentHub 不自建企业统一认证中心，也不维护组织、用户、角色的事实源。生产环境中，认证和 RBAC 由外部 IAM / SSO / 权限平台提供，AgentHub 只消费身份、租户、角色、权限决策，并在自身资源访问时强制执行。

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
- 已将 Agent / Function / Session / RAG 核心查询切到 tenant scoped
- 已清理核心写路径中的硬编码创建用户和前端 `userId` 信任
- 所有写操作忽略前端传入 tenantId，统一使用请求上下文
- 已对关键 API 增加 RBAC 拦截器
- 已增加 mock 契约、401/403、跨租户访问测试
