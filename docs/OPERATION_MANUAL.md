# AgentHub 操作手册

## 启动与停止

本地启动：

```bat
start.bat
```

停止：

```bat
stop.bat
```

高级用法：如需指定端口，可直接调用内部脚本。

```bat
scripts\start.bat 18091 15191
```

```bat
scripts\stop.bat 18091 15191
```

启动后访问：

- 前端：`http://127.0.0.1:5173`
- 后端健康检查：`http://127.0.0.1:8080/api/health`

## 页面使用

### Agent 工作台

用于创建和管理 Agent。

1. 点击“新建 Agent”。
2. 填写 Agent 名称、描述、系统提示词、模型、温度。
3. 选择可用函数。
4. 点击“保存 Agent”。
5. 已创建 Agent 可编辑或删除。

### 函数注册中心

用于注册可被 Agent 调用的 HTTP 函数。

1. 点击“注册函数”。
2. 填写名称、描述、端点地址、请求方法。
3. 点击“保存”。
4. 可对函数执行测试、编辑、删除。

### 知识库

用于管理 RAG 数据。

1. 创建知识库。
2. 选择知识库后添加文档。
3. 选择文档后添加分块。
4. 添加分块后系统会自动生成本地 embedding。
5. 在检索框输入查询内容，查看综合分、向量分、关键词分。

### 会话管理

用于创建会话并向 Agent 发送消息。

1. 选择 Agent。
2. 输入用户 ID。
3. 点击“新建会话”。
4. 在输入框发送消息。
5. 系统会调用 Agent runtime，返回 assistant 消息。

## 常用命令

后端测试：

```bat
cd backend
mvn test
```

前端构建：

```bat
cd frontend
npm run build
```

查看日志：

```bat
type .run\logs\backend.out.log
type .run\logs\backend.err.log
type .run\logs\frontend.out.log
type .run\logs\frontend.err.log
```

## 当前限制

- 本地启动默认使用 `dev` profile 和 H2 内存库，重启后数据会清空。
- RAG 默认使用本地向量表，配置 `agenthub.ai.milvus.enabled=true` 后可接 Milvus HTTP adapter。
- Embedding / Rerank 默认本地 fallback，配置外部 URL 后走 LLM gateway 或兼容网关。
- WebSocket 当前按最终回复切 chunk 推送；Token Router 真增量流式可作为后续增强。
- 飞书/企微 webhook 当前使用通用 payload 和 shared secret，真实平台签名验签可作为后续增强。

# Runtime Extensions

## WebSocket Chat

本地联调用：

```
ws://127.0.0.1:8080/ws/sessions?token=mock-token&tenantId=tenant-001
```

消息格式：

```json
{"sessionId":"<sessionId>","content":"hello"}
```

## Bot Webhook

飞书/企微/通用 webhook 共用：

```
POST /api/bots/webhooks/{channel}
```

若 BotBinding 设置了 `secret`，调用方必须提供 `X-Bot-Secret` header 或 payload `secret`。

## Offline Evaluation

评估入口：

```
POST /api/evaluations/runs
```

当前 MVP 使用 `expected` 子串命中作为通过标准，后续可扩展为模型裁判或指标插件。
