# AgentHub 任务清单

> 项目边界见 [PROJECT_BOUNDARY.md](PROJECT_BOUNDARY.md)。
> 所有任务必须落入六个一级产品域之一，否则默认不进入项目边界。

---

## 总览

| 状态 | 数量 | 说明 |
|------|------|------|
| ✅ 已完成 | 47+ | P0/P1 生产化缺口全部闭环，交付包、MCP 适配、韧性层、Spring Boot 3 路线全部落地 |
| 🔄 进行中 | 0 | — |
| 📋 待完成 | 0 | 当前迭代任务全部完成 |

---

## 一、Dify 替代交付包（已完成 4 项）

> 目标：把六个产品域包装成私有化迁移交付包。

| # | 优先级 | 任务 | 状态 | 验收口径 |
|---|--------|------|------|----------|
| 1 | P1 | 交付证据包导出归档 | ✅ DONE | 新增 `/api/observability/delivery-evidence/export` 端点，管理控制台支持 JSON 下载和 ZIP 归档导出，ZIP 包含 7 个证据快照文件 |
| 2 | P1 | Dify 导入结果可视化 | ✅ DONE | 新增 `dify_migration_result` 表和 `/api/migrations/dify/results` 端点，管理控制台新增"Dify 导入"和"导入结果"两个页签，支持导入执行、结果展示、成功/失败统计 |
| 3 | P1 | 迁移前置检查报告 | ✅ DONE | 增强 preflight 报告，新增风险项（HIGH/MEDIUM/LOW）、兼容性问题（knowledge base permissions、workflow node compatibility），管理控制台"迁移检查"页签可视化展示 |
| 4 | P2 | 客户交付模板包 | ✅ DONE | 新增 `/api/delivery/templates` 端点，支持轻量版/标准版/全栈信创版 JSON 和 ZIP 格式下载，包含部署步骤、功能清单、验收标准、运维手册 |

---

## 二、技术栈升级路线（已完成 3 项）

> 目标：Spring Boot 3 迁移、Spring AI MCP 接入、工具层韧性。

| # | 优先级 | 任务 | 状态 | 验收口径 |
|---|--------|------|------|----------|
| 5 | P2 | Spring Boot 3 迁移 | ✅ DONE | 创建 `pom-springboot3.xml` 参考配置和 `SPRING_BOOT_3_MIGRATION.md` 迁移指南，完整记录 `javax.*` → `jakarta.*` 变更清单、批量替换命令和验证步骤 |
| 6 | P2 | Spring AI MCP 适配层 | ✅ DONE | 新增 `SpringAiMcpAdapter` 组件（schema 映射 + 类路径检测），McpApiImpl 支持 Spring AI MCP 导入（检测到类路径时自动使用），保留 `/api/mcp/tools/import` 契约不变 |
| 7 | P2 | 工具层 Resilience4j | ✅ DONE | FunctionDefinition 增加 retry/circuitBreaker/fallback 字段，新增 `ResilientFunctionRegistry` + `ResilienceConfig`，Function invoke 外包 retry/circuit breaker/fallback 策略，新增数据库迁移脚本 |

---

## 三、后续增强方向（Roadmap，非当前迭代任务）

> 以下属于六个产品域的自然演进，已有设计文档，按需排期。

| 产品域 | 增强方向 | 参考文档 |
|--------|----------|----------|
| Agent Runtime | 结构化 tool call 协议、WebSocket 真流式增量、运行超时、Session 上下文压缩 | [ARCHITECTURE.md](ARCHITECTURE.md) Phase A |
| Tool / MCP | MCP schema 完整映射、工具权限、参数校验、执行超时 | [TASKS.md](TASKS.md) |
| Knowledge / RAG | Milvus 生产配置、Embedding/Rerank 失败策略、文档权限过滤、chunk 策略配置化 | [TASKS.md](TASKS.md) |
| Multi-Agent Workflow | DAG 并行执行、checkpoint/resume、human-in-the-loop | [TASKS.md](TASKS.md) |
| AgentOps / Governance | RBAC/tenant 全覆盖、评估指标插件化、Trace 查询维度补全 | [TASKS.md](TASKS.md) |
| Enterprise Channels | 消息幂等与重放保护、BotBinding 密钥轮换 | [TASKS.md](TASKS.md) |

---

## 四、已完成的里程碑

### Phase 0 — 基础设施 ✅
- JPA + MySQL 持久层（替换内存 mock）
- Agent / Function / Session 核心 API 与前端闭环
- 主流程集成测试

### Phase 1 — RAG 能力 ✅
- KnowledgeBase / Document / Chunk 领域模型
- Milvus HTTP adapter + 本地向量表 MVP
- Embedding / Rerank 网关接入（含本地 fallback）
- Hybrid search + RerankService MVP

### Phase 2 — Multi-Agent Workflow ✅
- WorkflowDefinition 领域模型
- DAG 拓扑执行（顺序执行 MVP + Trace/StepRecord）

### Phase 3 — MCP 集成 ✅
- McpFunctionAdapter（MCP tool → FunctionDefinition）

### Phase 4 — 可观测性 ✅
- Trace / StepRecord 持久化
- LLM usage audit（in-memory + JPA 持久化）
- 离线评估批处理（golden dataset）MVP

### Phase 5 — Enterprise Channels ✅
- BotBinding + channel 级 Session 隔离
- 飞书/企微/通用 webhook adapter MVP

### P0/P1 生产化缺口 ✅
- RBAC / tenant enforcement 覆盖所有 API
- 生产部署就绪检查（`/api/health/ready`）
- 交付就绪度可视化（管理控制台）
- MCP readiness、Workflow resume、Operations alerts、安全基线
- WebSocket 直连 Token Router 真流式增量
- 评估指标插件化
- 真实飞书/企微签名验签
- `/api/observability/production-readiness` 返回 `productionReady: true`

### Dify 交付包 ✅
- 交付证据包导出归档（ZIP 格式，7 个证据快照文件）
- Dify 导入结果可视化（持久化 + 管理控制台 3 个新页签）
- 迁移前置检查报告（风险等级、兼容性分析）
- 客户交付模板包（轻量版/标准版/全栈信创版）

### 技术栈升级 ✅
- Spring Boot 3 迁移指南 + `pom-springboot3.xml` 参考配置
- Spring AI MCP 适配层（`SpringAiMcpAdapter` + McpApiImpl 增强）
- 工具层 Resilience4j（retry/circuit breaker/fallback + StepRecord 接入）

---

## 五、决策原则

1. **六域内优先** — 所有方向必须落入六个一级产品域之一
2. **LLM 网关是唯一入口** — Embedding、Rerank、评估调用全部走网关
3. **持久层先做** — Phase 0 是所有上游的前提
4. **MCP 优先** — 优先适配行业标准
5. **AgentOps 围绕运行链路** — 不发展成独立 APM 产品
6. **RAG 服务 Agent 运行** — 不发展成独立企业知识管理平台
