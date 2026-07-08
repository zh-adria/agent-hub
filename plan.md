# AgentHub 项目规划文档

## 一、项目定位

**产品名称**：AgentHub  
**Slogan**：The Agent Infrastructure Layer  
**定位**：企业级 AI Agent 开发、部署、管理平台  
**目标用户**：企业开发者、AI 团队、业务部门  
**核心价值**：降低 Agent 开发门槛，提供开箱即用的 Agent 基础设施

### 1.1 与 Token Router 的项目边界

`C:\Users\16813\Downloads\token-router` 定位为企业级 LLM 网关和治理层；AgentHub 定位为企业级 Agent 基础设施平台。两者通过 HTTP/OpenAI-compatible 模型调用集成，不共享领域模型。

详细边界见 [PROJECT_BOUNDARY.md](PROJECT_BOUNDARY.md)。

AgentHub 主责：

- Agent CRUD、Agent Studio、Agent 版本、部署和运行状态。
- Function Registry、Function Market、工具 schema、工具执行和结果处理。
- Session 生命周期、消息历史、Memory、上下文压缩。
- ReAct/Workflow Runtime、步骤编排、Sandbox。
- Vector Store/RAG 的文档生命周期、召回、rerank，或对接外部 RAG 服务。

Token Router 主责：

- LLM Provider 接入、模型目录、模型能力、定价和健康状态。
- API Key 加密、Provider 探测、OpenAI-compatible `chat/completions`。
- 模型路由、熔断、降级、配额、预算、成本、账单、审计、模型评测。
- 对 AgentHub 传入的 `businessTag`、`policyId`、`agentSessionId`、`agentStepId`、`toolNames`、`knowledgeBaseId` 做网关级策略校验和审计。

AgentHub non-goals：

- 不实现企业统一 LLM Provider 密钥库。
- 不作为模型路由、Provider 健康、预算降级、LLM 账单的事实源。
- 不建设第二套 OpenAI-compatible LLM 网关。

---

## 二、技术架构总览

### 2.1 整体架构

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
│  │  │ AgentAppService│ │ FunctionApp│ │ GovernanceApp  │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │                   Domain Layer (Domain)                 ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │  Agent      │  │  Function   │  │  Governance     │ ││
│  │  │  Aggregate  │  │  Aggregate  │  │  Aggregate      │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
│  ┌─────────────────────────────────────────────────────────┐│
│  │              Infrastructure Layer (Infrastructure)       ││
│  │  ┌─────────────┐  ┌─────────────┐  ┌─────────────────┐ ││
│  │  │Token Router │  │  Vector DB  │  │  Sandbox        │ ││
│  │  │  Client     │  │  Adapter    │  │  Adapter        │ ││
│  │  └─────────────┘  └─────────────┘  └─────────────────┘ ││
│  └─────────────────────────────────────────────────────────┘│
└─────────────────────────────────────────────────────────────┘
                            │
            ┌───────────────┼───────────────┐
            │               │               │
       ┌────▼────┐    ┌────▼────┐    ┌────▼────┐
       │ Token   │    │ Vector  │    │ Docker  │
       │ Router  │    │ Store   │    │Sandbox  │
       └─────────┘    └─────────┘    └─────────┘
```

### 2.2 COLA 四层架构映射

| COLA 层 | AgentHub 对应 | 职责 |
|---------|--------------|------|
| **App Layer** | `agenthub-app` | 应用服务编排、用例实现、DTO 转换 |
| **Domain Layer** | `agenthub-domain` | 领域模型、业务规则、聚合根、领域服务 |
| **Infrastructure Layer** | `agenthub-infra` | 外部依赖实现、持久化、消息队列、缓存 |
| **Client Layer** | `agenthub-client` | DTO、Mapper、Feign Client |

---

## 三、模块划分（COLA Archetype 标准）

### 3.1 后端模块结构

```
agent-hub/
├── agent-hub-app/                    # 应用层
│   ├── src/main/java/com/agenthub/app/
│   │   ├── agent/
│   │   │   ├── AgentCreateServiceImpl.java
│   │   │   ├── AgentChatServiceImpl.java
│   │   │   ├── AgentExecuteServiceImpl.java
│   │   │   └── dto/
│   │   │       ├── AgentCreateCmd.java
│   │   │       ├── AgentChatCmd.java
│   │   │       └── AgentDTO.java
│   │   ├── function/
│   │   │   ├── FunctionRegistryServiceImpl.java
│   │   │   └── dto/
│   │   ├── governance/
│   │   │   ├── AuditServiceImpl.java
│   │   │   ├── PermissionServiceImpl.java
│   │   │   └── CostCalculationServiceImpl.java
│   │   └── AgentHubApplication.java
│   └── src/main/resources/
│       └── application.yml
│
├── agent-hub-domain/                 # 领域层
│   ├── src/main/java/com/agenthub/domain/
│   │   ├── agent/
│   │   │   ├── model/
│   │   │   │   ├── Agent.java           # 聚合根
│   │   │   │   ├── AgentId.java         # 值对象
│   │   │   │   ├── AgentConfig.java     # 值对象
│   │   │   │   ├── AgentStep.java       # 实体
│   │   │   │   └── AgentStatus.java     # 枚举
│   │   │   ├── repository/
│   │   │   │   └── AgentRepository.java
│   │   │   ├── service/
│   │   │   │   ├── AgentRuntimeService.java
│   │   │   │   └── AgentValidationService.java
│   │   │   └── event/
│   │   │       ├── AgentCreatedEvent.java
│   │   │       └── AgentExecutedEvent.java
│   │   ├── function/
│   │   │   ├── model/
│   │   │   │   ├── FunctionDefinition.java
│   │   │   │   ├── FunctionParameter.java
│   │   │   │   └── FunctionPermission.java
│   │   │   └── repository/
│   │   │       └── FunctionRepository.java
│   │   ├── governance/
│   │   │   ├── model/
│   │   │   │   ├── AuditLog.java
│   │   │   │   ├── Permission.java
│   │   │   │   └── CostRecord.java
│   │   │   └── service/
│   │   │       ├── AuditService.java
│   │   │       └── CostCalculationService.java
│   │   └── common/
│   │       ├── BaseEntity.java
│   │       └── ValueObject.java
│   └── src/main/resources/
│       └── META-INF/spring/org.springframework.imports
│
├── agent-hub-infra/                  # 基础设施层
│   ├── src/main/java/com/agenthub/infra/
│   │   ├── agent/
│   │   │   ├── repo/
│   │   │   │   ├── AgentDO.java
│   │   │   │   └── AgentMapper.java
│   │   │   └── persistence/
│   │   │       └── AgentRepositoryImpl.java
│   │   ├── function/
│   │   │   ├── repo/
│   │   │   │   └── FunctionDO.java
│   │   │   └── persistence/
│   │   │       └── FunctionRepositoryImpl.java
│   │   ├── llm/
│   │   │   ├── TokenRouterClient.java
│   │   │   ├── TokenRouterRequestMapper.java
│   │   │   ├── TokenRouterResponseMapper.java
│   │   │   └── TokenRouterProperties.java
│   │   ├── vector/
│   │   │   ├── VectorStore.java
│   │   │   └── QdrantVectorStore.java
│   │   ├── sandbox/
│   │   │   ├── SandboxManager.java
│   │   │   └── DockerSandboxManager.java
│   │   ├── config/
│   │   │   ├── MybatisPlusConfig.java
│   │   │   ├── RedisConfig.java
│   │   │   └── OpenTelemetryConfig.java
│   │   └── listener/
│   │       └── DomainEventPublisher.java
│   └── src/main/resources/
│       └── mapper/AgentMapper.xml
│
├── agent-hub-client/                 # 客户端层
│   ├── src/main/java/com/agenthub/client/
│   │   ├── agent/
│   │   │   ├── AgentDTO.java
│   │   │   └── AgentFeignClient.java
│   │   ├── function/
│   │   │   └── FunctionDTO.java
│   │   └── governance/
│   │       ├── AuditLogDTO.java
│   │       └── CostRecordDTO.java
│   └── src/main/java/com/agenthub/client/demo/
│       └── DemoApplication.java
│
├── agent-hub-starters/               # 启动器模块（可选）
│   ├── agent-hub-starter-llm/
│   ├── agent-hub-starter-vector/
│   └── agent-hub-starter-sandbox/
│
├── agent-hub-server/                 # 启动模块（Spring Boot Application）
│   └── src/main/java/com/agenthub/
│       └── AgentHubServerApplication.java
│
└── pom.xml                           # 父 POM
```

### 3.2 前端模块结构

```
agent-hub-frontend/
├── src/
│   ├── api/                          # API 接口
│   │   ├── agent.ts
│   │   ├── function.ts
│   │   └── governance.ts
│   ├── components/                   # 通用组件
│   │   ├── Agent/
│   │   ├── Function/
│   │   └── Common/
│   ├── views/                        # 页面
│   │   ├── Agent/
│   │   │   ├── Studio.vue           # Agent 开发工作台
│   │   │   ├── List.vue             # Agent 列表
│   │   │   └── Detail.vue           # Agent 详情
│   │   ├── Function/
│   │   │   ├── Registry.vue         # 工具注册
│   │   │   └── Market.vue           # 工具市场
│   │   ├── Governance/
│   │   │   ├── Audit.vue            # 审计日志
│   │   │   └── Cost.vue             # 成本分析
│   │   └── Dashboard/
│   │       └── Home.vue
│   ├── router/
│   │   └── index.ts
│   ├── store/                        # Pinia 状态管理
│   │   ├── agent.ts
│   │   └── auth.ts
│   └── App.vue
├── package.json
├── vite.config.ts
└── tsconfig.json
```

---

## 四、领域模型设计（DDD）

### 4.1 核心聚合：Agent

```java
// Agent.java - 聚合根
public class Agent {
    private AgentId id;                    // Agent ID
    private TenantId tenantId;             // 租户 ID
    private String name;                   // Agent 名称
    private String description;            // 描述
    private AgentConfig config;            // 配置（模型、提示词、工具列表）
    private AgentStatus status;            // 状态
    private Integer version;               // 版本
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // 领域行为
    public void create() { /* 业务规则验证 */ }
    public void deploy() { /* 部署逻辑 */ }
    public void execute(AgentCommand command) { /* 执行逻辑 */ }
    public void terminate() { /* 终止逻辑 */ }
}

// AgentConfig.java - 值对象
public class AgentConfig {
    private ModelInfo model;               // 模型配置
    private String systemPrompt;           // 系统提示词
    private List<FunctionRef> functions;   // 关联工具
    private MemoryConfig memory;           // 记忆配置
    private SandboxConfig sandbox;         // 沙箱配置
    
    // 不可变，通过 Builder 构造
    public static Builder builder() { return new Builder(); }
}
```

### 4.2 核心聚合：Function

```java
// FunctionDefinition.java - 聚合根
public class FunctionDefinition {
    private FunctionId id;
    private TenantId tenantId;
    private String name;
    private String description;
    private JsonSchema parameters;         // 参数 Schema
    private JsonSchema returnSchema;       // 返回 Schema
    private FunctionStatus status;
    private Set<FunctionPermission> permissions;
    
    // 领域行为
    public void validateParameters(Map<String, Object> args) {
        // 参数校验逻辑
    }
    
    public boolean isExecutableBy(Tenant tenant) {
        // 权限检查
    }
}
```

### 4.3 核心聚合：Session

```java
// AgentSession.java - 聚合根
public class AgentSession {
    private SessionId id;
    private AgentId agentId;
    private TenantId tenantId;
    private List<Message> messages;        // 对话历史
    private Memory workingMemory;          // 工作记忆
    private SessionStatus status;
    private LocalDateTime expiresAt;
    
    public void addMessage(Message message) {
        // 添加消息，触发上下文压缩
        this.messages.add(message);
        this.workingMemory = MemoryCompressor.compress(messages, workingMemory);
    }
}
```

---

## 五、应用服务设计（CQRS）

### 5.1 Agent 应用服务

```java
@Service
public class AgentAppServiceImpl implements AgentAppService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private AgentRuntimeService agentRuntimeService;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    // 创建 Agent
    @Transactional
    public AgentDTO createAgent(AgentCreateCmd cmd) {
        // 1. 创建聚合根
        Agent agent = Agent.create(
            AgentId.generate(),
            TenantId.of(cmd.getTenantId()),
            cmd.getName(),
            cmd.getConfig()
        );
        
        // 2. 持久化
        agentRepository.save(agent);
        
        // 3. 发布领域事件
        eventPublisher.publish(new AgentCreatedEvent(agent.getId()));
        
        // 4. 返回 DTO
        return AgentDTO.from(agent);
    }
    
    // Agent 对话（CQRS Query）
    public AgentChatResult chat(AgentChatCmd cmd) {
        // 1. 查询 Agent
        Agent agent = agentRepository.findById(AgentId.of(cmd.getAgentId()))
            .orElseThrow(() -> new AgentNotFoundException());
        
        // 2. 查询或创建会话
        AgentSession session = sessionRepository.findOrCreate(cmd.getSessionId());
        
        // 3. 执行对话（领域服务）
        return agentRuntimeService.chat(agent, session, cmd.getMessage());
    }
}
```

### 5.2 领域事件设计

```java
// AgentCreatedEvent.java
public class AgentCreatedEvent implements DomainEvent {
    private AgentId agentId;
    private TenantId tenantId;
    private LocalDateTime occurredOn;
    
    // 事件处理
    @EventHandler
    public void onAgentCreated(AgentCreatedEvent event) {
        // 1. 初始化向量集合
        vectorStore.createCollection(event.getAgentId());
        // 2. 初始化沙箱配置
        sandboxManager.initSandbox(event.getAgentId());
        // 3. 发送通知
        notificationService.notify(event.getTenantId(), "Agent created: " + event.getAgentId());
    }
}
```

---

## 六、技术栈选型

### 6.1 后端技术栈

| 技术 | 选型 | 理由 |
|------|------|------|
| **框架** | Spring Boot 3.2 + COLA Archetype | 阿里开源 DDD 架构，企业级 |
| **ORM** | MyBatis-Plus | COLA 标配，易于扩展 |
| **数据库** | MySQL 8.0 + Redis 7 | 主从架构，读写分离 |
| **向量数据库** | Qdrant | 开源、高性能、云原生 |
| **LLM 调用** | Token Router Client | 复用 Token Router 的 Provider、路由、配额、预算、审计和账单能力 |
| **沙箱** | Docker + gVisor | 安全隔离 |
| **消息队列** | RocketMQ | 阿里系，高可靠 |
| **配置中心** | Nacos | 阿里系，动态配置 |
| **链路追踪** | OpenTelemetry + Jaeger | 云原生标准 |
| **监控** | Prometheus + Grafana | 指标监控 |
| **容器** | Docker + Kubernetes | 云原生部署 |

### 6.2 前端技术栈

| 技术 | 选型 | 理由 |
|------|------|------|
| **框架** | Vue 3 + TypeScript | 主流选择，生态成熟 |
| **构建** | Vite | 快速构建 |
| **UI** | Element Plus / Arco Design | 企业级组件库 |
| **状态管理** | Pinia | Vue 3 推荐 |
| **路由** | Vue Router 4 | 官方路由 |
| **HTTP** | Axios | 成熟稳定 |
| **图表** | ECharts | 数据可视化 |

---

## 七、数据库设计

### 7.1 核心表结构

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

-- 审计日志表
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    user_id VARCHAR(64),
    action VARCHAR(64) NOT NULL,
    resource_type VARCHAR(64),
    resource_id VARCHAR(64),
    details JSON,
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_time (tenant_id, created_at)
);

-- 成本记录表
CREATE TABLE cost_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id VARCHAR(64) NOT NULL,
    agent_id VARCHAR(64),
    session_id VARCHAR(64),
    model VARCHAR(64),
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    cost DECIMAL(10, 6),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_tenant_time (tenant_id, created_at)
);
```

---

## 八、COLA 架构特性应用

### 8.1 使用 COLA Archetype 生成

```bash
# 使用 COLA  Archetype 生成项目
mvn archetype:generate \
  -DarchetypeGroupId=com.alibaba.cola \
  -DarchetypeArtifactId=cola-archetype-simple \
  -DarchetypeVersion=4.3.1 \
  -DgroupId=com.agenthub \
  -DartifactId=agent-hub \
  -Dversion=1.0.0-SNAPSHOT
```

### 8.2 扩展点设计

```java
// 扩展点接口
public interface AgentExtensiblePoint {
    
    // 执行前扩展
    default void beforeExecute(AgentContext context) {}
    
    // 执行后扩展
    default void afterExecute(AgentContext context, AgentResult result) {}
    
    // 工具调用拦截
    default ToolResult onToolCall(ToolCall call) {
        return null; // 返回 null 继续执行
    }
}

// 实现示例：成本统计扩展
@Component
public class CostCalculationExtPoint implements AgentExtensiblePoint {
    
    @Autowired
    private CostService costService;
    
    @Override
    public void afterExecute(AgentContext context, AgentResult result) {
        costService.record(
            context.getTenantId(),
            context.getAgentId(),
            result.getTokenUsage(),
            result.getCost()
        );
    }
}
```

---

## 九、实施计划

### Phase 0：项目初始化（第 1 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 0.1 使用 COLA Archetype 生成项目骨架 | agent-hub 后端骨架 | 架构师 |
| 0.2 前端脚手架初始化 | Vite + Vue 3 + Element Plus | 前端工程师 |
| 0.3 CI/CD 流水线搭建 | GitHub Actions / Jenkins | DevOps |
| 0.4 开发环境 Docker Compose | MySQL, Redis, Qdrant, RocketMQ | DevOps |

### Phase 1：核心领域层（第 2-3 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 1.1 领域模型设计 | Agent, Function, Session 聚合 | 架构师 |
| 1.2 Repository 接口定义 | 领域层接口 | 后端工程师 |
| 1.3 DO 与 Mapper 实现 | 基础设施层实现 | 后端工程师 |
| 1.4 单元测试 | 领域层测试覆盖率 > 80% | 后端工程师 |

### Phase 2：Agent Runtime（第 4-6 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 2.1 Token Router Client | 对接 Token Router completion/stream API，透传 Agent 元数据 | 后端工程师 |
| 2.2 ReAct Engine | ReAct 循环实现 | 后端工程师 |
| 2.3 Tool Execution | HTTP/代码/Shell 工具执行 | 后端工程师 |
| 2.4 Sandbox 沙箱 | Docker 隔离执行环境 | 后端工程师 |

### Phase 3：Agent 应用服务（第 7-8 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 3.1 Agent CRUD | 创建/查询/更新/删除 Agent | 后端工程师 |
| 3.2 Agent Chat | 对话接口（含流式） | 后端工程师 |
| 3.3 Function Registry | 工具注册/发现/导入 | 后端工程师 |
| 3.4 Session 管理 | 会话创建/查询/过期 | 后端工程师 |

### Phase 4：治理与可观测性（第 9-10 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 4.1 审计日志 | 操作审计、查询接口 | 后端工程师 |
| 4.2 成本归因 | 消费 Token Router 用量/成本结果，按 Agent/Session/Function 归因展示 | 后端工程师 |
| 4.3 权限控制 | RBAC + 工具级权限 | 后端工程师 |
| 4.4 监控告警 | Prometheus 指标、Grafana 看板 | DevOps |

### Phase 5：前端开发（第 7-11 周，并行）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 5.1 Agent Studio | 可视化 Agent 创建工作台 | 前端工程师 |
| 5.2 Agent 管理 | Agent 列表、详情、监控 | 前端工程师 |
| 5.3 Function Market | 工具浏览、导入、配置 | 前端工程师 |
| 5.4 治理面板 | 审计日志、成本分析、权限管理 | 前端工程师 |

### Phase 6：集成测试与部署（第 12 周）

| 任务 | 交付物 | 负责 |
|------|--------|------|
| 6.1 端到端测试 | POC 场景验证 | 全员 |
| 6.2 性能测试 | 压测、并发、延迟 | 测试工程师 |
| 6.3 安全测试 | 沙箱逃逸、权限绕过 | 安全工程师 |
| 6.4 生产部署 | K8s 部署、灰度发布 | DevOps |

---

## 十、关键代码示例

### 10.1 聚合根示例（Agent.java）

```java
package com.agenthub.domain.agent.model;

import com.agenthub.domain.common.BaseEntity;
import com.agenthub.domain.agent.event.AgentCreatedEvent;
import com.agenthub.domain.agent.service.AgentValidationService;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(chain = true)
public class Agent extends BaseEntity<AgentId> {
    private TenantId tenantId;
    private String name;
    private String description;
    private AgentConfig config;
    private AgentStatus status;
    private Integer version;
    
    public static Agent create(AgentId id, TenantId tenantId, String name, AgentConfig config) {
        // 业务规则验证
        AgentValidationService.validateName(name);
        AgentValidationService.validateConfig(config);
        
        Agent agent = new Agent();
        agent.setId(id);
        agent.setTenantId(tenantId);
        agent.setName(name);
        agent.setConfig(config);
        agent.setStatus(AgentStatus.CREATED);
        agent.setVersion(1);
        
        // 发布领域事件
        agent.registerEvent(new AgentCreatedEvent(id, tenantId));
        
        return agent;
    }
    
    public void deploy() {
        if (this.status != AgentStatus.CREATED && this.status != AgentStatus.STOPPED) {
            throw new BusinessException("Agent cannot be deployed in current status: " + this.status);
        }
        this.status = AgentStatus.RUNNING;
        this.version++;
    }
    
    public AgentExecutionResult execute(AgentCommand command) {
        if (this.status != AgentStatus.RUNNING) {
            throw new BusinessException("Agent is not running");
        }
        // 委托给领域服务
        return AgentRuntimeService.execute(this, command);
    }
}
```

### 10.2 应用服务示例

```java
@Service
public class AgentAppServiceImpl implements AgentAppService {
    
    @Autowired
    private AgentRepository agentRepository;
    
    @Autowired
    private EventPublisher eventPublisher;
    
    @Override
    @Transactional
    public AgentDTO createAgent(AgentCreateCmd cmd) {
        // 1. 创建聚合根
        Agent agent = Agent.create(
            AgentId.generate(),
            TenantId.of(cmd.getTenantId()),
            cmd.getName(),
            AgentConfig.builder()
                .model(ModelInfo.of(cmd.getModel()))
                .systemPrompt(cmd.getSystemPrompt())
                .functions(cmd.getFunctionIds())
                .build()
        );
        
        // 2. 持久化
        agentRepository.save(agent);
        
        // 3. 发布事件（最终一致性）
        eventPublisher.publish(agent.getDomainEvents());
        
        return AgentDTO.from(agent);
    }
    
    @Override
    public AgentChatResult chat(AgentChatCmd cmd) {
        // 1. 查询聚合根
        Agent agent = agentRepository.findById(AgentId.of(cmd.getAgentId()))
            .orElseThrow(() -> new AgentNotFoundException(cmd.getAgentId()));
        
        // 2. 查询会话
        AgentSession session = sessionRepository.findOrCreate(
            SessionId.of(cmd.getSessionId()),
            agent.getId()
        );
        
        // 3. 执行领域行为
        AgentExecutionResult result = agent.execute(
            AgentCommand.chat(cmd.getMessage())
        );
        
        // 4. 保存会话
        session.addMessage(result.getMessages());
        sessionRepository.save(session);
        
        // 5. 返回 DTO
        return AgentChatResult.of(result.getFinalAnswer(), result.getSteps());
    }
}
```

---

## 十一、部署架构

### 11.1 容器化部署

```yaml
# docker-compose.yml
version: '3.8'
services:
  agent-hub-server:
    image: agenthub/server:latest
    ports:
      - "8080:8080"
    environment:
      - SPRING_PROFILES_ACTIVE=prod
      - NACOS_ADDR=nacos:8848
    depends_on:
      - mysql
      - redis
      - qdrant
      
  mysql:
    image: mysql:8.0
    environment:
      MYSQL_ROOT_PASSWORD: root
      MYSQL_DATABASE: agent_hub
    volumes:
      - mysql-data:/var/lib/mysql
      
  redis:
    image: redis:7-alpine
    volumes:
      - redis-data:/data
      
  qdrant:
    image: qdrant/qdrant:latest
    ports:
      - "6333:6333"
    volumes:
      - qdrant-data:/qdrant/storage
      
  jaeger:
    image: jaeger:latest
    ports:
      - "16686:16686"
      
volumes:
  mysql-data:
  redis-data:
  qdrant-data:
```

### 11.2 K8s 部署清单

```yaml
# deployment.yaml
apiVersion: apps/v1
kind: Deployment
metadata:
  name: agent-hub-server
spec:
  replicas: 3
  selector:
    matchLabels:
      app: agent-hub-server
  template:
    metadata:
      labels:
        app: agent-hub-server
    spec:
      containers:
      - name: agent-hub-server
        image: agenthub/server:1.0.0
        ports:
        - containerPort: 8080
        resources:
          requests:
            cpu: "500m"
            memory: "512Mi"
          limits:
            cpu: "2000m"
            memory: "2Gi"
        livenessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
        readinessProbe:
          httpGet:
            path: /actuator/health
            port: 8080
```

---

## 十二、总结

**AgentHub** 项目基于 **COLA 架构** 设计，具有以下优势：

1. **DDD 领域驱动**：清晰的领域模型，业务逻辑内聚
2. **COLA 标准分层**：App/Domain/Infra/Client 四层分离
3. **前后端分离**：Vue 3 + Spring Boot，职责清晰
4. **云原生就绪**：Docker + K8s，可观测性完善
5. **企业级特性**：多租户、权限、审计、成本核算

**下一步行动**：
1. 确认项目启动
2. 我可以立即生成 COLA 项目骨架代码
3. 配置开发环境 Docker Compose
4. 开始 Phase 1 领域层开发

需要我立即生成项目骨架代码吗？
