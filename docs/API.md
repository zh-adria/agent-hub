# AgentHub API 契约

**服务地址**：`http://localhost:8080/api`

---

## Function 管理

### 注册 Function

```
POST /functions
```

请求体：

```json
{
  "name": "weather_lookup",
  "description": "查询城市天气",
  "endpoint": "http://weather-service/api/lookup",
  "method": "GET",
  "parameters": [
    { "name": "city", "type": "string", "required": true }
  ]
}
```

响应 `201 Created`：

```json
{
  "id": "func-001",
  "name": "weather_lookup",
  "description": "查询城市天气",
  "endpoint": "http://weather-service/api/lookup",
  "method": "GET",
  "parameters": [
    { "name": "city", "type": "string", "required": true }
  ],
  "createdAt": "2026-07-09T10:00:00Z",
  "updatedAt": "2026-07-09T10:00:00Z"
}
```

### 列出 Function

```
GET /functions
```

响应 `200 OK`：Function 数组。

### 调用 Function

```
POST /functions/{id}/invoke
```

请求体：

```json
{
  "input": { "city": "Shanghai" }
}
```

响应 `200 OK`：

```json
{
  "functionId": "1",
  "status": 200,
  "result": { "temperature": 28, "condition": "sunny" }
}
```

---

## Agent 管理

### 创建 Agent

```
POST /agents
```

请求体：

```json
{
  "name": "客服助手",
  "description": "处理客户咨询的智能助手",
  "prompt": "你是一个专业的客服代表...",
  "model": "gpt-4.1-mini",
  "temperature": 0.2,
  "functionIds": ["1", "2"]
}
```

响应 `201 Created`。

### 列出 Agent

```
GET /agents
```

响应 `200 OK`：Agent 数组。

### 获取 Agent 详情

```
GET /agents/{id}
```

响应 `200 OK`：

```json
{
  "id": "agent-123",
  "name": "客服助手",
  "description": "处理客户咨询的智能助手",
  "prompt": "你是一个专业的客服代表...",
  "model": "gpt-4.1-mini",
  "temperature": 0.2,
  "functionIds": ["1", "2"],
  "createdAt": "2026-07-09T10:00:00Z",
  "updatedAt": "2026-07-09T10:00:00Z"
}
```

### 更新 Agent

```
PUT /agents/{id}
```

请求体同创建，响应 `200 OK`。

### 删除 Agent

```
DELETE /agents/{id}
```

响应 `204 No Content`。

### 绑定 Agent Function

```
POST /agents/{id}/functions/{functionNameOrId}
```

响应 `200 OK`。

### 解绑 Agent Function

```
DELETE /agents/{id}/functions/{functionNameOrId}
```

响应 `200 OK`。

### 获取 Agent 已绑定 Function

```
GET /agents/{id}/functions
```

响应 `200 OK`：Function 数组。

---

## Session 管理

### 创建会话

```
POST /sessions
```

请求体：

```json
{
  "agentId": "agent-123",
  "userId": "user-456"
}
```

响应 `201 Created`：

```json
{
  "id": "session-789",
  "agentId": "agent-123",
  "userId": "user-456",
  "status": "active",
  "createdAt": "2026-07-09T10:00:00Z",
  "updatedAt": "2026-07-09T10:00:00Z"
}
```

### 获取会话消息列表

```
GET /sessions/{id}/messages
```

响应 `200 OK`：Message 数组。

```json
[
  {
    "id": "msg-001",
    "sessionId": "session-789",
    "role": "user",
    "content": "你好",
    "timestamp": "2026-07-09T10:00:01Z"
  },
  {
    "id": "msg-002",
    "sessionId": "session-789",
    "role": "assistant",
    "content": "您好！有什么可以帮您的？",
    "timestamp": "2026-07-09T10:00:02Z"
  }
]
```

### 发送消息

```
POST /sessions/{id}/messages
```

请求体：

```json
{
  "content": "帮我查一下上海的天气",
  "role": "user"
}
```

响应 `201 Created`：

```json
{
  "id": "msg-003",
  "sessionId": "session-789",
  "role": "assistant",
  "content": "上海今天 28°C，晴天。",
  "timestamp": "2026-07-09T10:00:03Z"
}
```

---

## RAG 知识库

### 创建知识库

```
POST /knowledge-bases
```

请求体：

```json
{
  "name": "政策知识库",
  "description": "企业制度和流程文档",
  "embeddingProvider": "llm-gateway",
  "embeddingModel": "text-embedding",
  "chunkSize": 800,
  "chunkOverlap": 120
}
```

### 列出知识库

```
GET /knowledge-bases
```

### 更新知识库

```
PUT /knowledge-bases/{knowledgeBaseId}
```

请求体同创建。

### 删除知识库

```
DELETE /knowledge-bases/{knowledgeBaseId}
```

### 创建文档

```
POST /knowledge-bases/{knowledgeBaseId}/documents
```

请求体：

```json
{
  "title": "员工手册.md",
  "sourceUri": "file://handbook.md",
  "mimeType": "text/markdown",
  "contentHash": "sha256..."
}
```

### 更新文档

```
PUT /knowledge-bases/{knowledgeBaseId}/documents/{documentId}
```

请求体同创建。

### 删除文档

```
DELETE /knowledge-bases/{knowledgeBaseId}/documents/{documentId}
```

### 创建文档分块

```
POST /knowledge-bases/{knowledgeBaseId}/documents/{documentId}/chunks
```

请求体：

```json
{
  "chunkIndex": 0,
  "content": "分块正文",
  "tokenCount": 128,
  "embeddingId": "vec-001"
}
```

### 更新文档分块

```
PUT /knowledge-bases/{knowledgeBaseId}/documents/{documentId}/chunks/{chunkId}
```

请求体同创建。

### 删除文档分块

```
DELETE /knowledge-bases/{knowledgeBaseId}/documents/{documentId}/chunks/{chunkId}
```

### 获取文档分块

```
GET /knowledge-bases/{knowledgeBaseId}/documents/{documentId}/chunks
```

### 检索知识库

```
POST /knowledge-bases/{knowledgeBaseId}/search
```

请求体：

```json
{
  "query": "报销流程",
  "topK": 5
}
```

响应 `200 OK`：

```json
[
  {
    "score": 0.82,
    "vectorScore": 0.76,
    "keywordScore": 1.0,
    "chunkId": 1,
    "documentId": 1,
    "chunkIndex": 0,
    "content": "分块正文",
    "embeddingId": "1"
  }
]
```

---

## 错误响应

统一错误格式：

```json
{
  "error": "RESOURCE_NOT_FOUND",
  "message": "Agent not found: agent-123",
  "timestamp": "2026-07-09T10:00:00Z"
}
```

| HTTP 状态码 | 说明 |
|-------------|------|
| 400 | 请求参数无效 |
| 401 | 未认证 |
| 403 | 无权限 |
| 404 | 资源不存在 |
| 429 | 请求频率超限 |
| 500 | 服务端错误 |
# 一级 AI 能力 API 摘要

## Workflow

- `POST /api/workflows`：创建 Multi-Agent DAG workflow
- `GET /api/workflows`：按租户列出 workflow
- `GET /api/workflows/{workflowId}`：读取 workflow
- `PUT /api/workflows/{workflowId}`：更新 workflow
- `DELETE /api/workflows/{workflowId}`：删除 workflow
- `POST /api/workflows/{workflowId}/execute`：执行 workflow，返回 `traceId`、`steps`、`outputs`

Workflow definition 示例：

```json
{
  "nodes": [
    { "id": "research", "agentId": "1", "input": "research topic" },
    { "id": "write", "agentId": "2", "dependsOn": ["research"] }
  ]
}
```

## MCP

- `POST /api/mcp/tools/import`：导入 MCP tool 到 FunctionDefinition
- `GET /api/mcp/readiness`：返回 MCP schema 映射、参数校验、RBAC、超时、错误归类 readiness 证据

## Dify Migration

- `POST /api/migrations/dify/preflight`：解析 Dify app/workflow/tool/knowledge 导出物，返回迁移预检摘要、阻塞项、告警和 AgentHub 映射预览
- `POST /api/migrations/dify/import`：在预检无阻塞时导入 Dify app、tool、workflow、knowledge/document 到 AgentHub 基础资源，并返回导入 ID

## Trace / Observability

- `GET /api/traces`：列出当前租户 trace
- `GET /api/traces/{traceId}/steps`：列出 trace step records
- `GET /api/observability/summary`：返回 trace、step、LLM audit 计数
- `GET /api/observability/delivery-readiness`：返回 Dify 替代 MVP 交付就绪度
- `GET /api/observability/production-readiness`：返回生产级交付缺口、P0 阻塞项和验收口径
- `GET /api/observability/alerts`：返回 Trace/Step 失败率、LLM token/cost、Webhook 事件计数和外部监控阈值建议
- `GET /api/observability/security-baseline`：返回生产安全检查表、租户隔离证据和密钥配置要求
- `GET /api/health/ready`：返回 MySQL、Redis、Milvus 依赖可用性，生产部署可作为 readiness probe

## Workflow

- `POST /api/workflows/{workflowId}/resume`：携带 checkpoint 和审批输入恢复 WAITING_APPROVAL workflow

## Evaluation

- `GET /api/evaluations/metrics`：返回可用评估指标插件（contains、exact、regex）
- `POST /api/evaluations/runs`：case 可通过 `metrics` 指定评估指标插件

## Enterprise Bot

- `POST /api/bots/webhooks/feishu`：支持 `X-Lark-Request-Timestamp` + `X-Lark-Signature` 飞书签名验签
- `POST /api/bots/webhooks/wecom`：支持 `timestamp`、`nonce`、`msg_signature` 企微回调签名验签
- `GET /api/health`：返回服务状态和外部 AI adapter 开关状态

## WebSocket Chat

- `WS /ws/sessions?token=mock-token&tenantId=tenant-001`

客户端消息：

```json
{
  "sessionId": "123",
  "content": "hello"
}
```

服务端事件：

- `started`
- `chunk`
- `completed`
- `error`

## Offline Evaluation

- `POST /api/evaluations/runs`：运行 golden dataset
- `GET /api/evaluations/runs`：列出评估批次
- `GET /api/evaluations/runs/{runId}`：读取批次和 case 结果

请求示例：

```json
{
  "name": "agent-smoke",
  "agentId": "1",
  "cases": [
    { "id": "case-1", "input": "hello", "expected": "hi" }
  ]
}
```

## Enterprise Bot

- `POST /api/bots/bindings`：创建 channel 到 Agent 的绑定
- `GET /api/bots/bindings`：列出绑定
- `DELETE /api/bots/bindings/{bindingId}`：删除绑定
- `POST /api/bots/webhooks/{channel}`：通用 webhook，`channel` 可用 `feishu`、`wecom` 或自定义值

Webhook 请求示例：

```json
{
  "botId": "bot-a",
  "conversationId": "chat-1",
  "content": { "text": "hello" }
}
```
