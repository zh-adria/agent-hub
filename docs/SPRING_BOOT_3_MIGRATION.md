# Spring Boot 3 迁移指南

## 迁移分支

分支名：`upgrade/springboot3`

只承载技术栈升级，不混入产品功能。与 `master` 并行维护。

## 验收顺序

1. 保持 Maven compiler 基线为 Java 21，现有测试全部通过
2. 升级 Spring Boot 3.x，完成 `javax.*` → `jakarta.*` 包迁移
3. 引入虚拟线程配置，用于 Agent I/O、工具调用、RAG 检索等阻塞型任务
4. 保持 REST API、数据库表结构、前端契约不变

## 变更清单

### 1. pom.xml 变更

| 变更项 | 当前 (Spring Boot 2.7) | 目标 (Spring Boot 3.3) |
|--------|------------------------|------------------------|
| Parent version | 2.7.18 | 3.3.0 |
| MySQL driver | mysql-connector-java 8.0.33 | mysql-connector-j (Jakarta) |
| JPA namespace | javax.persistence.* | jakarta.persistence.* |
| Validation | javax.validation.* | jakarta.validation.* |
| Servlet | javax.servlet.* | jakarta.servlet.* |

参考文件：`backend/pom-springboot3.xml`

### 2. 源码包迁移（javax.* → jakarta.*）

以下 22 个文件包含 `javax.*` 导入，需批量替换：

**Entity 文件（18 个）：**
- `DifyMigrationResultEntity.java`
- `WorkflowDefinitionEntity.java`
- `AgentEntity.java`
- `BotBindingEntity.java`
- `BotWebhookEventEntity.java`
- `DocumentChunkEntity.java`
- `EvaluationCaseResultEntity.java`
- `EvaluationRunEntity.java`
- `FunctionDefinitionEntity.java`
- `KnowledgeBaseEntity.java`
- `LLMUsageAuditEntity.java`
- `RagDocumentEntity.java`
- `SessionEntity.java`
- `StepRecordEntity.java`
- `TraceEntity.java`
- `VectorEmbeddingEntity.java`

**非 Entity 文件（4 个）：**
- `BotApiImpl.java`
- `HealthController.java`
- `RequestContextFilter.java`
- `RbacInterceptor.java`

**替换命令：**

```bash
# 批量替换 javax.persistence → jakarta.persistence
find backend/src/main/java -name "*.java" -exec sed -i 's/import javax\.persistence\./import jakarta.persistence./g' {} +

# 批量替换 javax.servlet → jakarta.servlet
find backend/src/main/java -name "*.java" -exec sed -i 's/import javax\.servlet\./import jakarta.servlet./g' {} +

# 批量替换 javax.validation → jakarta.validation
find backend/src/main/java -name "*.java" -exec sed -i 's/import javax\.validation\./import jakarta.validation./g' {} +
```

### 3. application.properties / application.yml 变更

Spring Boot 3 中部分配置键已变更：

| 配置 | 当前 | 目标 |
|------|------|------|
| server.servlet.context-path | 不变 | 不变 |
| spring.datasource.* | 不变 | 不变 |
| spring.jpa.* | 不变 | 不变 |
| spring.redis.* | 不变 | 不变 |

### 4. 虚拟线程配置

在 `application.yml` 中启用虚拟线程：

```yaml
spring:
  threads:
    virtual:
      enabled: true
```

或在代码中配置：

```java
@Configuration
public class VirtualThreadConfig {
    @Bean
    public TaskExecutor taskExecutor() {
        return new VirtualThreadTaskExecutorBuilder()
                .virtual(true)
                .threadNamePrefix("agenthub-io-")
                .build();
    }
}
```

### 5. 不变量

- REST API 端点路径不变
- 数据库表结构不变（JPA/Hibernate 自动处理）
- 前端 API 契约不变
- 功能行为不变

## 验证步骤

```bash
# 1. 编译
./mvnw clean compile

# 2. 测试
./mvnw test

# 3. 启动
java -jar target/agent-hub-backend.jar --spring.profiles.active=prod

# 4. 健康检查
curl http://localhost:8080/health/ready

# 5. API  smoke test
curl http://localhost:8080/api/agents
curl http://localhost:8080/api/observability/summary
```

## 回滚方案

如果迁移后出现问题，切回 `master` 分支即可。数据库 schema 不变，数据不受影响。
