# 技术栈升级路线

## 目标

当前后端基线已升级为 Java 21 / Spring Boot 2.7，为 AgentHub 后续私有化交付预留三条升级路径：Spring Boot 3、Spring AI MCP、工具层韧性。

## Spring Boot 3 迁移分支

迁移分支建议命名为 `upgrade/springboot3`，只承载技术栈升级，不混入产品功能。

验收顺序：

1. 保持 Maven compiler 基线为 Java 21，现有测试全部通过。
2. 升级 Spring Boot 3.x，完成 `javax.*` 到 `jakarta.*` 包迁移。
3. 引入虚拟线程配置，用于 Agent I/O、工具调用、RAG 检索等阻塞型任务。
4. 保持 REST API、数据库表结构、前端契约不变。

## Spring AI MCP 接入评估

当前 FunctionDefinition 已支持 `implementation = mcp` 和外部 endpoint。后续 Spring AI MCP 接入只作为适配层，不改变 Function API。

评估验收：

1. 保留现有 `/api/mcp/tools/import` 契约。
2. MCP Streamable HTTP 工具导入后仍落到 FunctionDefinition。
3. Function invoke、Trace、StepRecord、tenant scope 不发生行为退化。

## 工具层韧性路线

当前项目已有 timeout、workflow retry/fallback、外部 Embedding/Rerank/Milvus fallback。下一步可在 Function invoke 层引入 Resilience4j。

实施顺序：

1. FunctionDefinition 增加 retry、circuitBreaker、fallback 响应字段。
2. FunctionRegistry adapter 在 HTTP 调用外包一层 retry/circuit breaker。
3. 熔断、降级、超时统一写入 StepRecord error 和 Trace status。
4. 管理控制台在 Trace 步骤中显示失败类型。

## 不变约束

- 当前可演示版本以 Java 21 运行，接口和数据库契约保持不变。
- 升级分支不改变数据库迁移历史。
- 所有新能力必须继续保留租户隔离、RBAC、审计留痕。
