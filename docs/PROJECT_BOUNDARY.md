# AgentHub 项目边界

## 定位

AgentHub 是企业级 AI Agent 基础设施与运行平台，核心职责是把 Agent 从“可配置聊天机器人”推进到“可治理、可观测、可编排、可接入企业业务系统的运行单元”。

AgentHub 不直接管理底层模型供应商，也不建设第二套模型网关。所有 LLM、Embedding、Rerank、评估模型调用都通过外部 LLM 网关完成。

## 一级产品域

### 1. Agent Runtime

拥有：

- Agent 生命周期：创建、更新、删除、版本、部署状态
- Agent 配置：prompt、model hint、temperature、工具绑定、最大循环次数
- Session 生命周期：会话创建、消息历史、运行状态
- ReAct 执行：推理、工具调用、观察、最终回答
- 流式会话：WebSocket 会话入口与事件输出

不拥有：

- 模型供应商 SDK 直连
- 模型路由策略决策
- 模型预算、降级、账单事实源

### 2. Tool / MCP Layer

拥有：

- Function Registry：工具注册、发现、Schema、HTTP endpoint 元数据
- Agent 与 Function 的绑定关系
- Function invoke 编排与结果回传
- MCP tool 导入 FunctionDefinition 的 adapter

不拥有：

- 外部业务系统本身
- 企业级 API 网关
- 通用低代码集成平台

### 3. Knowledge / RAG Layer

拥有：

- KnowledgeBase、Document、Chunk 生命周期
- 本地向量表 MVP 与 Milvus adapter
- Embedding gateway 接入与 fallback
- Hybrid search、rerank 编排
- RAG 检索结果返回给 Agent Runtime

不拥有：

- 企业内容管理系统
- 文档权限事实源
- 跨企业全局知识图谱

### 4. Multi-Agent Workflow

拥有：

- WorkflowDefinition 生命周期
- Multi-Agent DAG 节点定义
- 拓扑校验与执行
- Workflow 执行链路 Trace / StepRecord

当前阶段是顺序执行 MVP。

后续增强方向：

- 并行 DAG 执行
- 重试、超时、失败补偿
- checkpoint / resume
- human-in-the-loop 审批节点

不拥有：

- 通用 BPM / OA 流程平台
- 企业任务调度系统

### 5. AgentOps / Governance

拥有：

- Tenant-scoped 资源隔离
- 外部认证 / RBAC 集成契约与 mock
- Trace、StepRecord、LLM usage audit
- Observability summary
- Offline evaluation golden dataset MVP

不拥有：

- 企业 IAM 主数据
- 全局权限中心
- LLM 账单结算系统
- 完整 APM / 日志平台

### 6. Enterprise Channels

拥有：

- BotBinding：channel 到 Agent 的绑定
- channel 级 Session 隔离
- 飞书、企微、通用 webhook adapter MVP

不拥有：

- IM 平台账号体系
- 第三方平台消息可靠投递事实源
- 复杂客服工单系统

## Memory 边界

AgentHub 将 Memory 拆为三类，避免概念混用：

| 类型 | 当前归属 | 说明 |
|------|----------|------|
| 短期上下文 | Session | 当前会话消息历史、运行上下文 |
| 长期记忆 | 后续增强 | 用户偏好、跨会话摘要、可撤销记忆 |
| 外部知识 | Knowledge / RAG | 企业文档、知识库、检索分块 |

当前实现重点是 Session 短期上下文与 Knowledge / RAG。长期记忆暂不作为生产级事实源。

## 集成方式

LLM 模型调用通过 HTTP 与外部 LLM 网关集成。集成契约（请求字段、端点、响应字段）见 [ARCHITECTURE.md](ARCHITECTURE.md#llm-集成) 中的 LLM 集成章节。

Embedding、Rerank、Offline Evaluation 中涉及模型能力的调用也必须走同一外部 LLM 网关边界。

## 总 Non-Goals

- 不实现企业统一 LLM Provider 密钥库
- 不作为模型路由、Provider 健康、预算降级、LLM 账单的事实源
- 不建设第二套 OpenAI-compatible LLM 网关
- 不建设通用 BPM、API 网关、IAM、APM、客服工单或企业内容管理系统
- 不把长期记忆作为当前阶段的生产级事实源

## 决策原则

1. AgentHub 只拥有 Agent 运行与治理上下文，不拥有底层模型治理事实源。
2. 新能力必须落在六个一级产品域之一，否则默认不进入当前项目边界。
3. MCP 优先于自研工具生态协议。
4. Trace、StepRecord、Evaluation 必须围绕 Agent / Session / Workflow，不发展成独立 APM 产品。
5. RAG 服务于 Agent 运行，不发展成独立企业知识管理平台。
