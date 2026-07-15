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
- 存活检查：`http://127.0.0.1:8080/api/health/live`
- 就绪检查：`http://127.0.0.1:8080/api/health/ready`

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
- Embedding / Rerank 默认本地 fallback。生产可设置 `fallback-on-failure=false` 进入 strict 模式。
- WebSocket 已走 Token Router stream 接口；如果外部网关不支持流式，会回退为一次性 chunk。
- 飞书/企微 webhook 当前支持通用 HMAC-SHA256 签名、messageId 幂等、secret 轮换；平台原生事件结构仍走 adapter 层映射。

## 生产关键配置

LLM 网关：

```yaml
token-router:
  base-url: https://token.sensenova.cn/v1
  completion-path: /api/chat/completions
  stream-completion-path: /api/chat/completions/stream
```

密钥通过环境变量或部署平台 secret 注入，不写入仓库配置文件。建议由外部 LLM 网关或运行环境读取。

RAG 外部能力：

```yaml
agenthub:
  ai:
    embedding:
      external-enabled: true
      url: https://token.sensenova.cn/v1/embeddings
      timeout-ms: 3000
      fallback-on-failure: true
    rerank:
      external-enabled: true
      url: https://token.sensenova.cn/v1/rerank
      timeout-ms: 3000
      fallback-on-failure: true
    milvus:
      enabled: true
      url: http://milvus-adapter:8080
      timeout-ms: 3000
      fallback-on-failure: true
```

## 监控与告警

健康检查：

- `/api/health/live`：进程存活。
- `/api/health/ready`：数据库可连接，Milvus 可用性展示。
- `/api/health`：服务和 AI adapter 配置摘要。

建议告警：

- `ready.status != UP` 持续 2 分钟。
- `trace.status = FAILED` 比例 5 分钟内超过 5%。
- `stepRecord.status = FAILED` 比例 5 分钟内超过 10%。
- LLM audit `totalTokens` 或 `cost` 单租户异常上涨。
- Bot webhook duplicate 比例异常上涨，可能表示上游重放。

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

若 BotBinding 设置了 `secret`，调用方可使用旧兼容方式提供 `X-Bot-Secret` header 或 payload `secret`。

推荐签名方式：

- Header `X-Bot-Timestamp`：请求时间戳。
- Header `X-Message-Id`：上游消息 ID。
- Header `X-Bot-Signature`：`HMAC_SHA256(secret, timestamp + "." + messageId)` 的 hex 值。

重复 `messageId` 会被判定为幂等重放，接口返回 `duplicate=true`，不会再次触发 Agent。

密钥轮换：

```
POST /api/bots/bindings/{bindingId}/rotate-secret
```

请求体：

```json
{"newSecret":"<new-secret>"}
```

## Offline Evaluation

评估入口：

```
POST /api/evaluations/runs
```

当前 MVP 使用 `expected` 子串命中作为通过标准，后续可扩展为模型裁判或指标插件。
