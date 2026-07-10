# AgentHub 任务清单

## 剩余产品工作

- 补充端到端 API/前端测试，覆盖 Agent/Function/Session 主流程
- 如果需要实时会话 UI，实现完整的 WebSocket/聊天流式传输
- 在核心功能稳定后添加生产级认证/授权（RBAC）和租户 enforcement
- 生产部署准备（监控、日志、健康检查等运维基础设施）

## 当前进度

- 已完成：Agent / Function / Session 的 JPA 持久化基础设施
- 已完成：Agent 主要配置字段持久化（description、prompt、model、temperature、functionIds）
- 已完成：Agent 与 Function 的基础绑定/解绑/查询接口
- 已完成：Function endpoint/method 字段持久化，并通过注册的 HTTP endpoint 执行 invoke
- 已完成：Session 消息写入 `context` JSON，发送消息会触发 ReAct 并持久化 assistant 回复
- 已完成：前端 Agent 列表/创建/编辑/删除、Function 删除、Session 创建/消息发送入口
- 部分完成：ReAct runtime 已接 LLM 网关；外部网关不可用时返回失败消息，尚未做流式输出
- 已完成：RAG 知识库、文档、分块模型与基础 API
- 已完成：RAG 本地 Embedding/向量表/相似度检索基础闭环
- 已完成：RAG Hybrid search / RerankService MVP
- 已完成：统一 API 错误响应基础结构
- 已完成：RAG 资源更新/删除与前端删除操作
- 已完成：外部认证/RBAC 集成契约文档
- 已完成：外部认证/RBAC/租户接口 mock
- 已完成：基础 TenantContext 和核心资源 tenant scoped 查询
- 已完成：认证请求过滤器、RBAC 拦截器、前端 mock token 联调头
- 已完成：认证/RBAC/租户 mock 契约、401/403、跨租户 404 集成测试
- 已完成：Milvus HTTP adapter、外部 Embedding 网关接入、外部 Rerank 网关接入（均带本地 fallback）
- 已完成：Multi-Agent WorkflowDefinition / DAG 拓扑执行 MVP
- 已完成：MCP tool 导入 FunctionDefinition adapter
- 已完成：Trace / StepRecord 持久化与 Session/Workflow 执行链路记录
- 已完成：生产部署基础观测接口（健康检查集成状态、trace 查询、observability summary）
- 已完成：WebSocket/聊天流式传输 MVP
- 已完成：离线评估批处理（golden dataset）MVP
- 已完成：Enterprise Bot Binding + 飞书/企微/通用 webhook adapter MVP
- 后续增强：WebSocket 直连 Token Router 真流式增量、评估指标插件化、真实飞书/企微签名验签

## AI 业务方向路线图

### Phase 0 — 基础设施（必须先做，1-2 周）

- P0-1：持久层（JPA + MySQL，替换内存 mock）— 已完成基础落地
- P0-2：依赖升级（pom.xml 加入 JPA、MySQL driver）— 已完成
- P0-3：核心 API 与前端基础闭环（Agent / Function / Session）— 已完成 MVP，并补充主流程集成测试

### Phase 1 — RAG 能力（2-3 周）

- P1-1：领域模型（KnowledgeBase、Document、Chunk）— 已完成基础落地
- P1-2：向量存储集成（Milvus + Embedding）— 已完成本地向量表 MVP + Milvus HTTP adapter + 外部 Embedding gateway fallback
- P1-3：检索编排（RerankService、Hybrid search）— 已完成 MVP + 外部 Rerank gateway fallback

### Phase 2 — Multi-Agent DAG（2-3 周）

- P2-1：WorkflowDefinition 领域模型 — 已完成 MVP
- P2-2：DAG 执行引擎（拓扑排序 + 并行策略）— 已完成拓扑排序顺序执行 MVP，并记录 Trace/StepRecord

### Phase 3 — MCP 集成（1 周）

- P3-1：McpFunctionAdapter（MCP tool → FunctionDefinition）— 已完成 MVP

### Phase 4 — 可观测性（1-2 周）

- P4-1：Trace / StepRecord 持久化 — 已完成 MVP
- P4-2：LLM 用量/成本写入 audit — 已完成 in-memory audit 与查询入口
- P4-3：离线评估批处理（golden dataset）— 已完成 MVP

### Phase 5 — Enterprise Bot（1 周）

- P5-1：BotBinding + channel 级 Session 隔离 — 已完成 MVP
- P5-2：飞书/企微 webhook adapter — 已完成 MVP

### 决策原则

1. **边界内优先**：所有方向均在项目边界声明的所有权内
2. **LLM 网关是唯一 LLM 入口**：Embedding、Rerank、评估用的模型调用全部走 LLM 网关
3. **持久层先做**：Phase 0 是所有上游的前提
4. **MCP 优先于自研工具发现**：优先适配行业标准
5. **可观测性放在业务能力之后**：先有可运行的 Agent / RAG / DAG，再评估可观测性需求
