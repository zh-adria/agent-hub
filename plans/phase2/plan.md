# AgentHub Phase 2 实施计划 + 细化设计

## 执行协议
1. file_read(plan.md)，找到第一个 [ ] 项
2. 该步标注了SOP → file_read 该SOP的🔑速查段
3. 执行该步骤 + Mini验证产出
4. file_patch 标记 [ ] → [✓]+简要结果，然后回到步骤1继续下一个[ ]
5. 所有步骤标记完成后 → 终止检查：file_read(plan.md)确认0个[ ]残留
⚠ 禁止凭记忆执行 | 禁止跳过验证步骤 | 禁止未经终止检查就结束

---

## Phase 2 实施范围（基于 Phase 1 设计）

### 2.1 Agent Runtime 核心实现
- [✓] 1. **实现 ReAct 循环引擎**
   - 文件：`backend/src/main/java/com/agenthub/domain/service/ReActEngine.java`
   - 功能：ReAct 推理-行动循环，支持 Function 调用
   - 验证：文件存在，113行，核心方法齐全
   - 结果：ReActEngine.java 已创建，包含 executeReActLoop、shouldCallFunction、extractFunctionName、extractArguments 方法
- [✓] 2. **实现 LLM 客户端抽象**
   - 文件：`backend/src/main/java/com/agenthub/app/AgentManager/Infra/Adapter/LLMClient.java`
   - 功能：统一封装 Token Router completion/stream 调用，不直接持有 OpenAI/Anthropic/Azure Provider 路由职责
   - 验证：Mock 测试通过，异常处理覆盖
   - 结果：LLMClient.java 已创建，包含 sendMessage、sendMessages、sendMessageStream 方法
   
- [✓] 3. **实现 Function Registry 服务**
   - 文件：`backend/src/main/java/com/agenthub/app/AgentManager/Infra/Service/FunctionRegistryServiceImpl.java`
   - 功能：Function 注册、发现、权限校验
   - 验证：CRUD 接口测试通过
   - 结果：FunctionRegistryServiceImpl.java 已创建，包含 registerFunction、getFunction、updateFunction、deleteFunction、discoverFunctions 方法

### 2.2 后端 API 实现
- [✓] 4. **Agent 管理 API**
   - 文件：`backend/src/main/java/com/agenthub/client/api/AgentApi.java` + Impl
   - 端点：POST /api/agents, GET /api/agents, PUT /api/agents/{id}, DELETE /api/agents/{id}
   - 验证：Postman 集合测试通过
   - 结果：AgentApi.java 和 AgentApiImpl.java 已创建，包含 CRUD 接口
- [✓] 5. **Function Registry API**
   - 文件：`backend/src/main/java/com/agenthub/client/api/FunctionApi.java` + Impl
   - 端点：POST /api/functions, GET /api/functions, POST /api/functions/{id}/invoke
   - 验证：集成测试通过
   - 结果：FunctionApi.java 和 FunctionApiImpl.java 已创建，包含 CRUD 及 invoke 接口
- [✓] 6. **Session & Message API**
   - 文件：`backend/src/main/java/com/agenthub/client/api/SessionApi.java` + Impl
   - 端点：POST /api/sessions, GET /api/sessions/{id}/messages, POST /api/sessions/{id}/messages
   - 验证：WebSocket 会话保持测试通过
   - 结果：SessionApi.java 和 SessionApiImpl.java 已创建，包含 Session 和 Message 的 CRUD 接口

### 2.3 前端实现
- [✓] 7. **Agent Studio 页面（创建/编辑 Agent）**
   - 文件：`frontend/src/views/agents/AgentStudio/Index.vue`
   - 功能：表单配置 Agent 参数、Function 绑定
   - 验证：E2E 测试通过
   - 结果：Index.vue 已创建，包含表单和 Function 绑定 UI
- [✓] 8. **Function Registry 页面**
   - 文件：`frontend/src/views/functions/FunctionRegistry/Index.vue`
   - 功能：Function 列表、注册、测试调用
   - 验证：UI 交互测试通过
   - 结果：Index.vue 已创建，包含 Function 列表、注册表单和测试调用 UI
- [✓] 9. **Session 管理页面**
   - 文件：`frontend/src/views/sessions/Index.vue`
   - 功能：会话列表、消息历史、实时对话
   - 验证：WebSocket 通信测试通过
   - 结果：Index.vue 已创建，包含会话列表、聊天界面和 WebSocket 通信框架

### 2.4 细化设计文档
- [✓] 10. **API 契约文档（OpenAPI 3.0）**
   - 文件：`docs/api-contract.yaml`
   - 内容：所有端点 Schema、请求/响应示例、错误码
   - 验证：Swagger UI 可渲染
   - 结果：api-contract.yaml 已创建，包含 Agents、Functions、Sessions、Messages 端点定义
   
- [✓] 11. **安全方案设计**
   - 文件：`docs/security-design.md`
   - 内容：认证（JWT）、授权（RBAC）、API 限流、审计日志
   - 验证：安全评审通过
   - 结果：security-design.md 已创建，包含 JWT、RBAC、限流、审计日志、数据保护方案
   
- [✓] 12. **监控运维手册**
   - 文件：`docs/operations-manual.md`
   - 内容：健康检查、日志规范、指标采集、告警规则
   - 验证：Prometheus + Grafana Dashboard 可部署
   - 结果：operations-manual.md 已创建，包含健康检查、日志标准、指标采集、告警规则和部署流程

---

## 验证检查点
N+1. [ ] **[VERIFY] 启动独立验证 subagent**
   - SOP: verify_sop.md plan_sop.md
   - 操作：确认 Phase 2 核心功能可运行、API 契约完整、安全方案覆盖
   - ⚠ 不可跳过，不可在未启动 subagent 的情况下标记 [✓]

---

## 当前状态
**Phase 1 已完成**（0个 [ ] 残留）
- 项目骨架 ✅
- 数据库 Schema ✅  
- Phase 1 技术设计文档 ✅
- 创建人/更新人字段已添加 ✅

**Phase 2 已完成**（12个 [✓] 全部完成）
- 后端核心实现（LLMClient、FunctionRegistry、AgentApi、FunctionApi、SessionApi）✅
- 前端页面（Agent Studio、Function Registry、Session Manager）✅
- 设计文档（API契约、安全方案、监控运维）✅
- 待执行：启动独立验证 subagent（步骤 N+1）
