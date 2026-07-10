# AgentHub 技术架构文档

## 定位

AgentHub 是企业级 AI Agent 开发、部署、管理平台。LLM 模型调用通过 HTTP 与外部 LLM 网关集成，AgentHub 不直接调用 LLM 提供商 SDK。

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
│                    Backend (COLA Architecture)               │
│  ┌─────────────────────────────────────────────────────────┐│
│  │                    App Layer (Application)              ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │AgentAppSvc  │  │FunctionApp  │  │GovernanceApp    │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Domain Layer (Domain)                 ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │ Agent       │  │ Function    │  │ Governance      │ ││
│  │  │ Aggregate   │  │ Aggregate   │  │ Aggregate       │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Infrastructure Layer (Infrastructure)       ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │LLM Gateway  │  │ Vector DB   │  │ Sandbox         │ ││
│  │  │  Client     │  │  Adapter    │  │  Adapter        │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
       ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
       │  LLM    │    │ Vector  │    │ Docker  │
       │ Gateway │    │ Store   │    │Sandbox  │
       └─────────┘    └─────────┘    └─────────┘
```

### COLA 四层架构映射

| COLA 层 | 对应 | 职责 |
|---------|------|------|
| **App Layer** | `agenthub-app` | 应用服务编排、用例实现、DTO 转换 |
| **Domain Layer** | `agenthub-domain` | 领域模型、业务规则、聚合根、领域服务 |
| **Infrastructure Layer** | `agenthub-infra` | 外部依赖实现、持久化、消息队列、缓存 |
| **Client Layer** | `agenthub-client` | DTO、Mapper |

---

## 后端模块结构

```
agent-hub/
├── agent-hub-app/                    # 应用层
│   └── src/main/java/com/agenthub/app/
│       ├── agent/          # AgentAppService、DTO
│       ├── function/       # FunctionRegistryService
│       └── governance/     # Audit、Permission、Cost
│
├── agent-hub-domain/                 # 领域层
│   └── src/main/java/com/agenthub/domain/
│       ├── agent/
│       │   ├── model/      # Agent（聚合根）、AgentId、AgentConfig
│       │   ├── repository/ # AgentRepository 接口
│       │   ├── service/    # AgentRuntimeService
│       │   └── event/      # AgentCreatedEvent
│       ├── function/
│       │   ├── model/      # FunctionDefinition（聚合根）
│       │   └── repository/ # FunctionRepository 接口
│       ├── governance/
│       │   ├── model/      # AuditLog、Permission、CostRecord
│       │   └── service/    # AuditService、CostCalculationService
│       └── common/
│           ├── BaseEntity.java
│           └── ValueObject.java
│
├── agent-hub-infra/                  # 基础设施层
│   └── src/main/java/com/agenthub/infra/
│       ├── llm/            # LLM Gateway Client
│       ├── vector/         # VectorStore、QdrantVectorStore
│       ├── sandbox/        # SandboxManager、DockerSandboxManager
│       └── config/         # MybatisPlusConfig、RedisConfig
│
├── agent-hub-client/                 # 客户端层
│   └── src/main/java/com/agenthub/client/
│       ├── agent/          # AgentDTO
│       └── function/       # FunctionDTO
│
└── pom.xml
```

## 前端模块结构

```
agent-hub-frontend/
├── src/
│   ├── api/                # API 接口
│   ├── components/         # 通用组件
│   ├── views/              # 页面
│   │   ├── agents/AgentStudio/Index.vue
│   │   ├── functions/FunctionRegistry/Index.vue
│   │   └── sessions/Index.vue
│   ├── router/
│   ├── store/              # Pinia
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
| 框架 | Spring Boot 3.2 + COLA Archetype | DDD 架构 |
| ORM | MyBatis-Plus | COLA 标配 |
| 数据库 | MySQL 8.0 + Redis 7 | 主从架构 |
| 向量数据库 | Qdrant / Milvus | 开源高性能 |
| 沙箱 | Docker + gVisor | 安全隔离 |
| 链路追踪 | OpenTelemetry + Jaeger | 云原生标准 |
| 监控 | Prometheus + Grafana | 指标监控 |

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

## 实施计划

### Phase 0 — 基础设施（第 1 周）

使用 COLA Archetype 生成项目骨架，前端脚手架初始化，CI/CD 流水线，Docker Compose 开发环境。

### Phase 1 — 核心领域层（第 2-3 周）

领域模型设计（Agent、Function、Session），Repository 接口定义，DO 与 Mapper 实现，单元测试（覆盖率 > 80%）。

### Phase 2 — Agent Runtime（第 4-6 周）

LLM 客户端、ReAct Engine、Tool Execution、Sandbox 沙箱。

### Phase 3 — 应用服务（第 7-8 周）

Agent CRUD、Agent Chat、Function Registry、Session 管理。

### Phase 4 — 治理与可观测性（第 9-10 周）

审计日志、成本归因、权限控制、监控告警。

### Phase 5 — 前端开发（第 7-11 周，并行）

Agent Studio、Agent 管理、Function Market、治理面板。

### Phase 6 — 集成测试与部署（第 12 周）

端到端测试、性能测试、安全测试、生产部署。

---

## 部署架构

### 开发环境（Docker Compose）

```yaml
services:
  agent-hub-server:   # Spring Boot 后端
  frontend:           # Vite 前端
  mysql:              # MySQL 8.0
  redis:              # Redis 7
  qdrant:             # 向量数据库
```

### 生产环境（K8s）

```
Nginx (Reverse Proxy)
  → Frontend (Vue 3)
  → Backend API (Spring Boot + COLA, 3 replicas)
  ↓
Service Mesh (Nacos)
  → MySQL (1主3从)
  → Redis (6主)
  → Qdrant (3节点)
```
