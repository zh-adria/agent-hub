# Dify 替代交付计划

## 定位

AgentHub 的近期产品化切口是 **Dify 替代迁移 + Java 私有化 Agent 运行治理平台**。

目标客户不是个人或小团队原型验证场景，而是已经使用 Dify/Coze/自研 Agent 原型、但进入企业生产后需要私有化、审计、租户隔离、权限治理、企业系统接入的客户。

## 核心卖点

- Java / Spring 技术栈，贴近企业现有后端和微服务体系。
- LLM 调用统一走外部网关，AgentHub 专注 Agent 运行、治理、审计和集成。
- Tenant-scoped 资源隔离，覆盖 Agent、Function、RAG、Workflow、Trace、Bot Binding。
- Trace、StepRecord、LLM usage audit 支撑全链路可追溯。
- Function Registry 与 MCP adapter 支撑企业工具接入。
- RAG、Workflow、Bot channel 已有 MVP 闭环，可快速包装成私有化交付包。

## 交付包

### 轻量版

适合单业务线、单知识库、少量工具接入。

- Agent / Function / Session 主流程部署
- 一个 RAG 知识库闭环
- 一个 Bot channel 或 Web chat 入口
- 基础 trace 与 LLM usage audit
- 基础租户隔离

### 标准版

适合 Dify 替代迁移主推场景。

- Dify Agent / Workflow / Tool / Knowledge 现状盘点
- AgentHub Agent、Function、Workflow、KnowledgeBase 迁移
- MCP tool schema 映射与参数校验
- 企业 IAM / RBAC mock 或对接契约落地
- 全链路 Trace / StepRecord / LLM usage audit 查询
- Milvus / Redis / MySQL 私有化部署

### 全栈信创版

适合金融、政务、制造等高合规场景。

- 标准版全部能力
- 国产化 OS / 数据库 / NPU 适配评估
- 更严格的租户隔离策略
- 运维、日志、监控、告警对接
- 安全基线与验收材料

## 8-12 周交付流

| 阶段 | 周期 | 产物 |
|------|------|------|
| 迁移审计 | 1 周 | 现有 Dify/Agent 资产清单、风险清单、迁移映射表 |
| 核心迁移 | 4-6 周 | Agent、Function、RAG、Workflow 主流程可运行 |
| 治理补齐 | 2 周 | RBAC、租户隔离、Trace、StepRecord、LLM audit 验收 |
| 私有化适配 | 1-3 周 | 部署脚本、环境配置、健康检查、运维文档 |

## 近期执行任务

### P0：生产治理闭环

- RBAC / tenant enforcement 覆盖所有 API 和后台执行路径。
- LLM usage audit 持久化查询按租户、Agent、Session、Trace 过滤。
- Function invoke 写入 trace step，记录入参摘要、状态、耗时、错误类型。
- Observability summary 增加成本、token、失败率聚合。

### P1：MCP / Tool 标准化

- 完整映射 MCP tool schema 到 FunctionDefinition。
- Function 参数使用 JSON Schema 校验。
- Function invoke 增加超时、错误归类、重试策略。
- 工具权限按租户、角色、Agent 绑定关系过滤。

### P2：RAG 生产化

- Milvus adapter 增加生产配置、健康检查和失败策略。
- 明确租户隔离策略：小租户 collection 隔离，大租户 partition/schema 策略。
- 文档权限过滤走外部权限事实源契约。
- chunk 策略配置化，支持按文档类型调整。

### P3：Workflow 企业化

- DAG 并行执行。
- 节点级超时、重试、失败补偿。
- checkpoint / resume。
- human-in-the-loop 审批节点。

## Demo 主线

1. 创建一个企业租户和业务 Agent。
2. 导入一个 MCP tool，绑定到 Agent。
3. 创建知识库，导入文档，完成 RAG 检索。
4. 编排一个 Workflow：检索、工具调用、Agent 总结。
5. 通过 WebSocket 或 Bot channel 触发执行。
6. 展示 Trace、StepRecord、LLM usage audit。
7. 切换租户，验证资源不可见。

## 验收口径

- 客户能在私有环境启动完整系统。
- Dify 原型中的关键 Agent / Workflow / Tool / Knowledge 能映射到 AgentHub。
- 每次 Agent 执行都能追踪到租户、用户、Agent、Session、Trace、StepRecord、LLM usage。
- 越权访问返回 403 或租户内 404，不泄露跨租户资源。
- 工具调用有参数校验、超时控制、错误归类。
- RAG 检索结果受租户和文档权限约束。

## 风险

- Dify 法务与价格对比材料必须在对外销售前补充正式来源，不在代码仓库中写成事实承诺。
- Spring Boot 3 / Spring AI 迁移是技术路线，不应阻塞当前 Java 21 / Spring Boot 2.7 MVP 交付。
- 多租户物理隔离要按客户规模选型，不能默认所有租户一个 Milvus collection 策略。
