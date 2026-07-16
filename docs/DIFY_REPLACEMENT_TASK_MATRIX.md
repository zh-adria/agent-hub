# Dify 替代任务矩阵

## 任务来源

本矩阵来自 Java Agent 趋势、技术栈 BOM、Dify 替代论证三类材料的共识：AgentHub 应优先成为企业 Java 私有化 Agent 运行治理平台，而不是泛聊天或低代码工具。

## P0：可演示交付闭环

| 启发 | 当前项目映射 | 任务 | 验收 |
|------|--------------|------|------|
| Dify 替代要能 8-12 周交付 | Agent、Function、RAG、Workflow、Trace 已有 MVP | 增加交付就绪度视图 | 已落地：管理控制台能看到 Agent、Tool、RAG、Workflow、Trace、Step、Channel 就绪状态 |
| 企业更关心可审计 | Trace、StepRecord、LLM audit | 每次工具调用返回 traceId / stepRecordId | 已落地：Function 测试后可在 Trace 页面看到步骤 |
| 私有化不能依赖外部 IAM | mock identity + dev profile | dev 默认本地 mock 登录 | 已落地：前端可直接用 tenant-001/admin 登录 |

## P1：治理产品化

| 启发 | 当前项目映射 | 任务 | 验收 |
|------|--------------|------|------|
| 多租户隔离是 Dify 替代卖点 | TenantContext + tenant scoped repository | 覆盖所有 API 和后台执行路径 | 已落地：Agent、Function、Session、Workflow、KnowledgeBase 跨租户读取返回 404 |
| 成本和 token 要可查 | LLM usage audit | 查询按 agent/session/trace/user 过滤 | 已落地：审计页支持过滤，后端提供同条件汇总 |
| 操作留痕要可追溯 | TraceService | Function、Workflow、Session 统一写 StepRecord | 已落地：三类执行路径写入 StepRecord，Session 发送响应返回 traceId / stepRecordId |

## P2：RAG 生产化

| 启发 | 当前项目映射 | 任务 | 验收 |
|------|--------------|------|------|
| RAG 从能召回到可控 | HybridSearch + Rerank | 文档权限过滤契约 | 已落地：文档/chunk metadata 支持 accessTags，检索按请求 accessTags 过滤 |
| 私有化需要向量库策略 | Milvus adapter | collection / partition 策略文档和配置 | 已落地：健康检查显示 Milvus collection 策略、partition 策略和可用性 |
| 垂直模板更容易成交 | KnowledgeBase | 增加法律/金融/制造样例数据包 | 已落地：Demo package API 可一键创建法律/金融/制造知识库 |

## P3：Workflow 企业化

| 启发 | 当前项目映射 | 任务 | 验收 |
|------|--------------|------|------|
| 生产流程要可恢复 | DAG 顺序执行 MVP | checkpoint / resume | 已落地：Workflow 响应返回 completed/pending/waiting checkpoint |
| 人机协作是标配 | WebSocket + Workflow | human-in-the-loop 节点 | 已落地：human/approval 节点进入 WAITING_APPROVAL 并返回 checkpoint |
| 高并发要有韧性 | Function timeout 字段 | 节点级 timeout/retry/fallback | 已落地：节点支持 timeout/retry/fallback，失败与兜底写 StepRecord |

## P4：技术栈升级路线

| 启发 | 当前项目映射 | 任务 | 验收 |
|------|--------------|------|------|
| Java 21 虚拟线程适合 Agent I/O | 当前 Java 8 | 制定 Java 21 / Spring Boot 3 迁移分支 | 已落地：技术栈升级路线文档明确迁移分支、顺序和不变约束 |
| Spring AI 1.1 强化 MCP | 当前自研 LLM 网关 client + MCP adapter | 评估 Spring AI MCP 接入 | 已落地：路线文档明确 Spring AI MCP 作为适配层，不改变 Function API |
| Resilience4j 是工具层标配 | 当前 RestTemplate timeout | 增加 retry/circuit breaker | 已落地：路线文档明确 Function retry/circuit breaker 与审计接入顺序 |
