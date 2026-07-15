# AgentHub 技术架构文档

## 定位

AgentHub 是企业级 AI Agent 基础设施与运行平台。它拥有 Agent Runtime、Tool / MCP、Knowledge / RAG、Multi-Agent Workflow、AgentOps / Governance、Enterprise Channels 六个一级产品域。

LLM 模型调用通过 HTTP 与外部 LLM 网关集成，AgentHub 不直接调用 LLM 提供商 SDK，也不承担模型路由、Provider 健康、预算降级、LLM 账单事实源职责。完整产品边界见 [PROJECT_BOUNDARY.md](PROJECT_BOUNDARY.md)。

## 整体架构

### 架构图

```
┌─────────────────────────────────────────────────────────────┐
│                        Frontend (Vue 3)                      │
│  ┌─────────────┐  ┌─────────────┐  ┌─────────────────────┐  │
│  │ Agent Studio│  │ Admin Panel │  │ Developer Portal    │  │
│  └─────────────┘  └─────────────┘  └─────────────────────┘  │
└───────────────────────────┬─────────────────────────────────┘
                            │ HTTPS/REST
┌───────────────────────────▼─────────────────────────────────┐
│               Backend (Spring Boot, package layering)        │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    API / App Layer                      ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │Agent API    │  │Workflow API │  │AgentOps API     │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Domain Layer (Domain)                 ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │ Runtime     │  │ Tool / MCP  │  │ RAG / Workflow  │ ││
│  │  │ Services    │  │ Services    │  │ Services        │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Infrastructure / Adapter Layer              ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │LLM Gateway  │  │ Vector DB   │  │ Webhook / MCP   │ ││
│  │  │  Client     │  │  Adapter    │  │  Adapter        │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                            │
       ┌────────▼───────┐ ┌──────▼──────┐ ┌──────▼──────┐
       │  LLM Gateway   │ │ Vector Store│ │ Enterprise  │
       │ Chat/Embed/Rer │ │ Milvus/local│ │ Webhooks/MCP│
       └────────────────┘ └─────────────┘ └─────────────┘
```

### 产品域映射

| 产品域 | 核心代码 | 职责 |
|--------|----------|------|
| Agent Runtime | `domain/service/ReActEngine.java`、`SessionMessageService.java`、`client/ws/` | Session、ReAct、流式会话 |
| Tool / MCP | `FunctionRegistryService*`、`client/impl/McpApiImpl.java` | Function Registry、工具导入、工具调用 |
| Knowledge / RAG | `KnowledgeBase*`、`RagDocument*`、`HybridSearchService.java`、`RerankService.java` | 知识库、文档、分块、检索、rerank |
| Multi-Agent Workflow | `WorkflowExecutionService.java`、`WorkflowDefinitionEntity.java` | DAG 定义、拓扑执行、workflow trace |
| AgentOps / Governance | `TraceService.java`、`client/audit/`、`client/auth/`、`ObservabilityApiImpl.java` | 租户、RBAC、Trace、StepRecord、LLM usage audit、评估 |
| Enterprise Channels | `BotApiImpl.java`、`BotBindingEntity.java` | Bot 绑定、飞书/企微/通用 webhook adapter |

### 后端分层映射

当前实现是单 Spring Boot 模块，按 package 分层，不是独立 Maven 多模块。

| 层 | 对应 package | 职责 |
|----|--------------|------|
| API / Client | `com.agenthub.client` | REST / WebSocket API、认证过滤、外部 adapter controller |
| Domain | `com.agenthub.domain` | 领域模型、Repository 接口、运行时服务、RAG/Workflow 编排 |
| Infrastructure | `com.agenthub.infra` | JPA Entity、Spring Data Repository、持久化实现 |
| Resources | `src/main/resources` | 配置、数据库初始化、mapper 资源 |

---

## 后端模块结构

```
agent-hub/
├── backend/
│   ├── pom.xml
│   └── src/main/java/com/agenthub/
│       ├── AgentHubBackendApplication.java
│       ├── client/
│       │   ├── api/          # REST API、异常处理、请求上下文
│       │   ├── auth/         # 外部认证、RBAC、TenantContext 接入
│       │   ├── audit/        # LLM usage audit 查询入口
│       │   ├── impl/         # Agent/Function/RAG/Workflow/Trace/Bot/Eval API 实现
│       │   ├── tokenrouter/  # 外部 LLM 网关 HTTP client 与 DTO
│       │   └── ws/           # Session WebSocket chat
│       ├── domain/
│       │   ├── context/      # TenantContext
│       │   ├── model/        # Agent、Function、Session、KnowledgeBase、RAG model
│       │   ├── port/         # LLMClient、FunctionRegistry 等端口
│       │   ├── repository/   # Repository 接口与领域侧 adapter
│       │   └── service/      # ReAct、RAG、Workflow、Trace 等领域服务
│       └── infra/
│           └── persistence/
│               ├── entity/     # JPA Entity
│               └── repository/ # Spring Data JPA Repository
├── frontend/
└── docs/
```

## 前端模块结构

```
frontend/
├── src/
│   ├── api.ts              # API 接口
│   ├── views/              # 页面
│   │   ├── agents/AgentStudio/Index.vue
│   │   ├── functions/FunctionRegistry/Index.vue
│   │   ├── knowledge/Index.vue
│   │   └── sessions/Index.vue
│   └── App.vue
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 领域模型设计

### Agent（聚合根）

```java
public class Agent extends BaseEntity<AgentId> {
    private TenantId tenantId;
    private String name;
    private String description;
    private AgentConfig config;       // 模型配置、系统提示词、工具列表
    private AgentStatus status;
    private Integer version;

    public static Agent create(AgentId id, TenantId tenantId, String name, AgentConfig config) {
        Agent agent = new Agent();
        agent.setId(id);
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setConfig(config);
        agent.setStatus(AgentStatus.CREATED);
        agent.setVersion(1);
        return agent;
    }

    public void deploy() {
        if (this.status != AgentStatus.CREATED && this.status != AgentStatus.STOPPED) {
            throw new BusinessException("Cannot deploy in status: " + this.status);
        }
        this.status = AgentStatus.RUNNING;
        this.version++;
    }
}
```

### FunctionDefinition（聚合根）

```java
public class FunctionDefinition extends BaseEntity<FunctionId> {
    private TenantId tenantId;
    private String name;
    private String description;
    private JsonSchema parameters;
    private FunctionStatus status;
    private Set<FunctionPermission> permissions;

    public void validateParameters(Map<String, Object> args) { /* 参数校验 */ }
    public boolean isExecutableBy(Tenant tenant) { /* 权限检查 */ }
}
```

### AgentSession（聚合根）

当前实现重点是 Session 消息历史与短期上下文。长期记忆、跨会话摘要、可撤销用户偏好属于后续增强，不作为当前生产级事实源。

```java
public class AgentSession extends BaseEntity<SessionId> {
    private AgentId agentId;
    private TenantId tenantId;
    private List<Message> messages;
    private Memory workingMemory;
    private SessionStatus status;
    private LocalDateTime expiresAt;

    public void addMessage(Message message) {
        this.messages.add(message);
        this.workingMemory = MemoryCompressor.compress(messages, workingMemory);
    }
}
```

---

## ReAct 运行时设计

### 循环状态机

```
IDLE → THINKING → ACTING → OBSERVING → (循环) → FINISHED / ERROR
```

核心组件：
- **AgentExecutor**：执行引擎，管理 ReAct 循环生命周期
- **LLM Client**：大模型调用抽象，统一走 LLM 网关
- **ToolInvoker**：工具执行器，沙箱隔离（Docker 容器）

### 安全隔离级别

| 级别 | 类型 | 隔离方式 |
|------|------|---------|
| Level 1 | 纯函数（无副作用） | 直接执行 |
| Level 2 | 网络访问 | 域名白名单 |
| Level 3 | 文件系统操作 | 容器内临时目录 |
| Level 4 | 系统命令 | Docker 沙箱，资源受限 |

默认配额：CPU 1 Core / 内存 512MB / 磁盘 1GB / 超时 30s

### Function Registry

生命周期：`CREATE → VALIDATE → PUBLISH → VERSIONING → DEPRECATE → ARCHIVE`

发现维度：名称/标签搜索、分类过滤、权限过滤、热度排序

---

## LLM 集成

AgentHub 统一通过 LLM 网关客户端调用外部 LLM 网关，不直接调用 LLM 提供商 SDK。

### 请求字段

| 字段 | 说明 |
|------|------|
| `businessTag` | 租户/业务/运行时标签 |
| `userId` | 当前企业用户或服务主体 |
| `policyId` | 可选网关治理策略 |
| `modelHint` | 可选偏好提供商/模型 |
| `messages` / `temperature` / `maxTokens` / `stream` | 标准补全参数 |
| `extensions.agentId` | Agent 关联 |
| `extensions.agentSessionId` | Session 关联 |
| `extensions.agentStepId` / `agentStepType` | 步骤关联 |
| `extensions.traceId` | 链路追踪 |
| `extensions.toolNames` | 当前调用的工具列表 |
| `extensions.knowledgeBaseId` | 知识库 ID |

### 端点

- `POST /api/chat/completions` — 非流式
- `POST /api/chat/completions/stream` — 流式

### 响应消费

AgentHub 消费：`provider`、`model`、`promptTokens`、`completionTokens`、`totalTokens`、`cost`、`routeDecision`、`routeReason`。

### 请求示例

```json
{
  "businessTag": "tenant-a-runtime",
  "userId": "user-1",
  "policyId": "policy-agent-prod",
  "modelHint": "gpt-4.1-mini",
  "messages": [
    { "role": "system", "content": "你是一个客服助手。" },
    { "role": "user", "content": "总结最新的工单。" }
  ],
  "temperature": 0.2,
  "maxTokens": 800,
  "stream": false,
  "extensions": {
    "agentId": "agent-123",
    "agentSessionId": "session-456",
    "agentStepId": "step-001",
    "agentStepType": "reason",
    "traceId": "trace-789",
    "toolNames": ["ticket_search"],
    "knowledgeBaseId": "kb-support"
  }
}
```

### 失败处理

AgentHub 将策略拒绝和预算拒绝响应映射为运行时步骤失败。

---

## 技术栈选型

### 后端

| 技术 | 选型 | 理由 |
|------|------|------|
| 框架 | Spring Boot 2.7.x + Java 8 | 当前 `backend/pom.xml` 实际选型 |
| ORM | Spring Data JPA | 当前持久层实现 |
| 数据库 | MySQL 8.0，测试使用 H2 | Agent、Session、RAG、Trace、Workflow 等持久化 |
| 缓存 | Redis | 限流、会话或后续分布式能力预留 |
| 向量存储 | 本地向量表 MVP + Milvus HTTP adapter | 本地闭环与外部向量库接入并存 |
| LLM 集成 | 外部 LLM 网关 HTTP client | Chat、Embedding、Rerank、Evaluation 统一入口 |
| 实时通道 | Spring WebSocket | Session chat 流式事件 MVP |
| 可观测性 | Trace / StepRecord / Observability API | 当前产品内 AgentOps 能力 |

### 前端

| 技术 | 选型 | 理由 |
|------|------|------|
| 框架 | Vue 3 + TypeScript | 主流，生态成熟 |
| 构建 | Vite | 快速构建 |
| UI | Element Plus / Arco Design | 企业级组件库 |
| 状态管理 | Pinia | Vue 3 推荐 |
| HTTP | Axios | 成熟稳定 |

---

## 数据库设计

### 核心表

```sql
-- Agent 表
CREATE TABLE agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id VARCHAR(64) UNIQUE NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    config JSON NOT NULL,
    status VARCHAR(32) NOT NULL,
    version INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant (tenant_id),
    INDEX idx_status (status)
);

-- Function 表
CREATE TABLE function_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    function_id VARCHAR(64) UNIQUE NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    parameters_schema JSON,
    return_schema JSON,
    status VARCHAR(32) NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

-- Session 表
CREATE TABLE agent_session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id VARCHAR(64) UNIQUE NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    tenant_id VARCHAR(64) NOT NULL,
    context JSON,
    status VARCHAR(32) NOT NULL,
    expires_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_accessed_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent (agent_id),
    INDEX idx_tenant (tenant_id)
);
```

---

## 安全设计

### 认证（JWT）

- Header：`Authorization: Bearer <token>`
- 过期时间：access token 2 小时，refresh token 7 天
- 存储：refresh token 存 HTTP-only cookie，access token 存 localStorage

### 授权（RBAC）

| 角色 | 权限 |
|------|------|
| **Admin** | 系统完全访问 |
| **User** | 创建/管理自己的 Agent 和 Session |
| **Guest** | 只读访问公开 Agent |

### 权限矩阵

| 资源 | Admin | User | Guest |
|------|-------|------|-------|
| Agent CRUD | 全部 | 自己创建的 | 读取公开 |
| Function 调用 | 全部 | 全部 | 全部 |
| Session | 全部 | 自己的 | 无权限 |

### API 限流

- `/api/llm/invoke`：每用户 10 次/分钟
- `/api/functions/{id}/invoke`：每用户 30 次/分钟
- `/api/sessions`：每用户 5 次/分钟

实现：Token bucket 算法，Redis 分布式计数器，响应头 `X-RateLimit-Limit`、`X-RateLimit-Remaining`。

### 审计日志

记录事件：认证（登录/登出）、授权失败、Agent CRUD 操作、Function 调用、Session 创建/删除、LLM 调用。

存储：Elasticsearch 用于搜索和分析，90 天保留策略。

### 数据保护

- 密码：bcrypt（12 轮）
- AgentHub 自有凭证：AES-256-GCM 加密
- PII：日志中脱敏
- OWASP Top 10 防护、SQL 注入防护、XSS 防护、CSRF token

### 安全响应头

- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security`

---

## 运维手册

### 健康检查

- `GET /health` — 基础健康检查
- `GET /health/ready` — 就绪检查（数据库连接）
- `GET /health/live` — 存活检查

### 日志规范

JSON 格式，包含 `timestamp`、`level`、`service`、`traceId`、`message`、`userId`、`agentId`、`duration`。

### 指标采集

业务指标：`agent.created.count`、`function.invoked.count`、`session.created.count`、`llm.request.count`、`llm.latency.p99`。

系统指标：`jvm.memory.used`、`jvm.gc.pause`、`datasource.connection.active`、`http.request.duration.p99`。

### 告警规则

严重告警：`ServiceDown`（2 分钟）、`HighErrorRate`（>5% 持续 5 分钟）、`DatabaseDown`、`DiskFull`（>85%）。

警告告警：`HighLatency`（p99 > 2s 持续 10 分钟）、`HighMemory`（JVM > 80% 持续 15 分钟）、`LowConnectionPool`、`RateLimitHit`。

### 部署

```bash
# 构建
mvn clean package -DskipTests

# 部署到 K8s
kubectl apply -f k8s/

# 回滚
kubectl rollout undo deployment/backend -n agent-hub
```

### 看板地址

- **Grafana**：http://grafana.agent-hub.internal
- **Prometheus**：http://prometheus.agent-hub.internal
- **Jaeger**：http://jaeger.agent-hub.internal

---

## 实施路线

当前项目已完成 Agent / Function / Session / RAG / Workflow / MCP / Trace / Evaluation / Bot Binding 的 MVP 闭环。后续实施不再按单纯基础设施阶段推进，而按六个产品域补齐生产能力。

### Phase A — Agent Runtime 生产化

- 结构化 tool call 协议，替换文本 `FUNCTION_CALL:` 标记解析
- WebSocket 直连 LLM 网关真流式增量
- 运行超时、最大 token、循环保护、错误归类
- Session 短期上下文压缩策略

### Phase B — Tool / MCP 标准化

- MCP tool schema 到 FunctionDefinition 的完整映射
- 工具权限、参数校验、执行超时、错误标准化
- Function invoke 审计与 trace step 关联

### Phase C — Knowledge / RAG 增强

- Milvus adapter 生产配置与健康检查
- Embedding / Rerank 网关失败策略
- 文档分块策略配置化
- 检索结果权限过滤的外部契约

### Phase D — Multi-Agent Workflow 增强

- DAG 并行执行
- 节点级重试、超时、失败补偿
- checkpoint / resume
- human-in-the-loop 节点

### Phase E — AgentOps / Governance 产品化

- RBAC / tenant enforcement 覆盖所有资源
- Evaluation 指标插件化
- LLM usage audit 持久化
- Trace / StepRecord 查询维度补全

### Phase F — Enterprise Channels 加固

- 飞书 / 企微真实签名验签
- channel 消息幂等与重放保护
- channel 级 session 生命周期策略
- BotBinding 密钥轮换

---

## 部署架构

### 开发环境（Docker Compose）

```yaml
services:
  agent-hub-server:   # Spring Boot 后端
  frontend:           # Vite 前端
  mysql:              # MySQL 8.0
  redis:              # Redis 7
  vector-store:       # Milvus 或本地向量存储 adapter
```

### 生产环境（K8s）

```
Nginx (Reverse Proxy)
  → Frontend (Vue 3)
  → Backend API (Spring Boot, 3 replicas)
  ↓
Service Mesh (Nacos)
  → MySQL (1主3从)
  → Redis (6主)
  → Milvus / Vector Store (3节点)
```
