# AgentHub 项目边界

## 定位

AgentHub 是企业级 AI Agent 基础设施平台，拥有以下领域：

- Agent 生命周期：CRUD、版本、部署、状态管理
- Agent Studio：可视化创建和管理界面
- Function Registry：工具注册、发现、Schema 管理
- 工具执行：函数调用、沙箱隔离、结果处理
- Session 和 Memory：会话生命周期、消息历史、上下文压缩
- 工作流/运行时：ReAct/工作流运行时、步骤编排
- 向量存储/RAG：文档生命周期、检索、rerank 编排
- 审计关联：agent/session/step 关联追踪

## 集成方式

LLM 模型调用通过 HTTP 与外部 LLM 网关集成。集成契约（请求字段、端点、响应字段）见 [ARCHITECTURE.md](ARCHITECTURE.md#llm-%E9%9B%86%E6%88%90) 中的 LLM 集成章节。

## Non-Goals

- 不实现企业统一 LLM Provider 密钥库
- 不作为模型路由、Provider 健康、预算降级、LLM 账单的事实源
- 不建设第二套 OpenAI-compatible LLM 网关
