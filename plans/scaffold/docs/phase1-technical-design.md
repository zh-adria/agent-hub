# AgentHub Phase 1 详细技术设计文档

## 1. Agent Runtime 详细设计

### 1.1 ReAct 循环实现

Agent Runtime 采用 **ReAct (Reasoning + Acting)** 模式实现智能体推理与执行循环。

核心组件：
1. **AgentExecutor**：执行引擎，管理 ReAct 循环生命周期
   - 状态机：IDLE → THINKING → ACTING → OBSERVING → (循环) → FINISHED/ERROR
2. **LLM Client**：大模型调用抽象
   - 支持 OpenAI 兼容接口，流式/阻塞调用，自动重试
3. **Tool Invoker**：工具执行器
   - 沙箱隔离：Docker 容器或 gVisor
   - 资源限制：CPU/内存/网络/磁盘配额
   - 超时控制：单工具调用最长 30s

### 1.2 LLM 调用策略

**Prompt 模板管理**：
- 系统提示词：角色定义、输出格式、安全约束
- 少样本示例：根据 Agent 类型动态加载
- 工具描述注入：Function Registry 返回的 JSON Schema

**调用流程**：
User Query → Prompt Assembly → LLM API Call → Parse Thought/Action → Tool Execution → Observation FeedBack → Loop until FINISH

**错误处理**：
- LLM 返回格式异常：重试最多 3 次
- 工具执行失败：错误信息作为 Observation 反馈
- 循环次数超限：默认最多 10 轮

### 1.3 工具执行沙箱

**安全隔离级别**：
- Level 1：纯函数（无副作用）——直接执行
- Level 2：网络访问——限制域名白名单
- Level 3：文件系统操作——容器内临时目录
- Level 4：系统命令——Docker 沙箱，资源受限

**资源配额（默认）**：
- CPU：1 Core
- 内存：512MB
- 网络：仅出站，白名单控制
- 磁盘：1GB 临时存储
- 超时：30s/调用

---

## 2. Function Registry 详细设计

### 2.1 注册机制

Function 生命周期：CREATE → VALIDATE → PUBLISH → VERSIONING → DEPRECATE → ARCHIVE

注册信息模型包含：
- 基础信息：id, name, display_name, version, description
- 权限控制：visibility, required_permissions, rate_limit
- 技术定义：type, endpoint, input_schema, output_schema
- 审计字段：created_by, updated_by, created_at, updated_at

### 2.2 发现机制

**查询维度**：
1. 按名称/标签模糊搜索（支持中文分词）
2. 按分类过滤（内置分类 + 自定义标签）
3. 按权限过滤（仅返回当前用户有权限调用的 Function）
4. 按热度排序（调用次数、成功率、平均响应时间）

**缓存策略**：
- 热点数据（Top 100）：Redis 缓存，TTL 5 分钟
- 全量索引：PostgreSQL + 触发器同步到 Elasticsearch（Phase 2）
- 实时更新：Function 变更后 1s 内失效缓存

### 2.3 权限模型

**RBAC + ABAC 混合**：
- 角色（Role）：ADMIN、DEVELOPER、USER
- 属性（Attribute）：租户 ID、Function 分类、调用时间
- 策略（Policy）：effect, actions, resources, conditions

**审计日志**：
- 每次 Function 注册/修改/删除记录操作者、时间、IP
- 每次 Function 调用记录调用者、参数、返回结果、耗时

---

## 3. 核心 API 契约定义（OpenAPI 3.0）

### 3.1 Agent 管理 API

主要端点：
- GET /api/v1/agents：获取 Agent 列表（分页、搜索）
- POST /api/v1/agents：创建 Agent
- GET /api/v1/agents/{agentId}：获取 Agent 详情
- PUT /api/v1/agents/{agentId}：更新 Agent
- DELETE /api/v1/agents/{agentId}：删除 Agent
- POST /api/v1/agents/{agentId}/execute：执行 Agent（单轮工具调用）

### 3.2 Function Registry API

主要端点：
- GET /api/v1/functions：获取 Function 列表（支持搜索/过滤）
- POST /api/v1/functions：注册 Function
- POST /api/v1/functions/{functionId}/invoke：调用 Function

### 3.3 核心 Schema 定义

关键对象：
- Agent：id, name, description, system_prompt, llm_config, functions, status, 审计字段
- ExecutionStep：step, thought, action, action_input, observation, duration_ms
- TokenUsage：prompt_tokens, completion_tokens, total_tokens
- FunctionCreateReq：name, display_name, description, type, endpoint, input_schema, output_schema, visibility, required_permissions, rate_limit

---

## 4. 部署架构和运维手册

### 4.1 部署架构

```
Nginx (Reverse Proxy)
    │
    ├── Frontend (Vue 3 + Vite)
    ├── Backend API (Spring Boot + COLA)
    └── Admin UI (Vue 3 + Element)
    │
    ▼
Service Mesh (Consul/Nacos)
    │
    ├── MySQL (主从复制)
    ├── Redis (集群模式)
    ├── Qdrant (向量存储)
    └── RocketMQ (异步任务队列)
```

**环境说明**：
- 开发环境：Docker Compose，单机部署
- 测试环境：K8s 集群，MySQL 1主2从，Redis 3主3从
- 生产环境：K8s 集群，MySQL 1主3从，Redis 6主，Qdrant 3节点

### 4.2 运维手册

**监控指标**：
- 应用层：QPS、响应时间（P50/P95/P99）、错误率
- 资源层：MySQL 连接数/慢查询、Redis 命中率、Qdrant 查询延迟
- 业务层：Function 调用成功率、Agent 执行轮次分布、Token 消耗趋势

**告警规则**：
- CPU/内存使用率 > 80% 持续 5 分钟
- HTTP 错误率 > 1% 持续 2 分钟
- LLM 调用 P95 延迟 > 10s
- 工具执行失败率 > 5%
- MySQL 主从延迟 > 30s

**备份策略**：
- MySQL：每日全量备份 + Binlog 实时备份，保留 7 天
- Redis：AOF 每秒刷盘，RDB 每日备份
- Qdrant：每日快照，保留 7 天

**扩容策略**：
- 水平扩容：Backend 服务无状态，K8s HPA 自动扩缩
- 垂直扩容：Qdrant/Redis 按内存使用率触发
- 数据库扩容：读流量增加时添加从库，写流量增加时分库分表（Phase 2）

### 4.3 CI/CD 流程

代码提交 → GitHub Actions
  - Lint & Unit Test (Java 17 + Maven)
  - Build Docker Image
  - Push to Harbor
  - 开发环境：自动部署到 Docker Compose
  - 测试环境：自动部署到 K8s（develop 分支）
  - 生产环境：手动审批后部署（main 分支）

**发布检查清单**：
- [ ] 数据库 Migration 脚本已审查
- [ ] 配置变更已同步到 Nacos
- [ ] 功能测试通过（Postman/Newman）
- [ ] 性能测试通过（JMeter，目标：P95 < 500ms）
- [ ] 安全扫描通过（OWASP Dependency-Check）
- [ ] 发布公告已通知相关团队

---

## 5. 附录

### 5.1 术语表

| 术语 | 说明 |
|------|------|
| Agent | 智能体，由 System Prompt + LLM + Function 组合而成 |
| Function | 可被 Agent 调用的工具，定义输入/输出 Schema |
| ReAct | Reasoning + Acting，推理与执行交替的循环模式 |
| COLA | Clean Object-Oriented & Layered Architecture，阿里架构规范 |
| Runtime | Agent 执行引擎，管理 ReAct 循环生命周期 |

### 5.2 参考文档

- [COLA 架构官方文档](https://www.yuque.com/taoshengyuzhou/cola4)
- [OpenAPI 3.0 规范](https://swagger.io/specification/)
- [ReAct 论文](https://arxiv.org/abs/2210.03629)
- [Spring Boot 官方文档](https://docs.spring.io/spring-boot/docs/current/reference/html/)

---

**文档状态**：Draft  
**最后更新**：2026-07-08  
**作者**：GenericAgent
