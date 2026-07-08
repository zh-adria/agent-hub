# AgentHub Operations Manual

## 1. Health Checks

### 1.1 Endpoints
- `GET /health` - Basic health check
- `GET /health/ready` - Readiness check (DB connection)
- `GET /health/live` - Liveness check

### 1.2 Expected Response
```json
{
  "status": "UP",
  "timestamp": "2026-07-08T10:00:00Z",
  "version": "2.0.0"
}
```

## 2. Log Standards

### 2.1 Log Levels
- `ERROR`: System errors requiring immediate attention
- `WARN`: Potential issues, degraded performance
- `INFO`: Normal operations, key events
- `DEBUG`: Detailed diagnostic information

### 2.2 Log Format (JSON)
```json
{
  "timestamp": "2026-07-08T10:00:00Z",
  "level": "INFO",
  "service": "agent-hub-backend",
  "traceId": "abc-123",
  "message": "Agent created",
  "userId": "user-456",
  "agentId": "agent-789",
  "duration": 45
}
```

### 2.3 Key Events to Log
- Authentication success/failure
- API requests (method, path, status, duration)
- Agent/Function CRUD operations
- Session creation/deletion
- LLM invocations through Token Router, correlated by agent/session/step metadata
- Error stack traces

## 3. Metrics Collection

### 3.1 Business Metrics
- `agent.created.count` - Number of agents created
- `agent.updated.count` - Number of agents updated
- `function.invoked.count` - Function invocation count
- `session.created.count` - New sessions
- `llm.request.count` - AgentHub LLM calls sent to Token Router
- `llm.latency.p99` - AgentHub-to-Token-Router response latency (p99)

Provider/model routing, quota, budget, billing, and provider health metrics remain Token Router source-of-truth metrics.

### 3.2 System Metrics
- `jvm.memory.used` - JVM heap usage
- `jvm.gc.pause` - GC pause time
- `datasource.connection.active` - Active DB connections
- `http.request.active` - Active HTTP requests
- `http.request.duration.p99` - HTTP request latency (p99)

### 3.3 Implementation
```java
// Micrometer + Prometheus
Counter.builder("agent.created.count")
  .description("Total agents created")
  .register(meterRegistry);

Timer.builder("llm.request.duration")
  .publishPercentiles(0.5, 0.95, 0.99)
  .register(meterRegistry);
```

## 4. Alert Rules

### 4.1 Critical Alerts (PagerDuty)
- `ServiceDown` - Service unavailable for 2 minutes
- `HighErrorRate` - Error rate > 5% for 5 minutes
- `DatabaseDown` - Database connection lost
- `DiskFull` - Disk usage > 85%

### 4.2 Warning Alerts (Slack)
- `HighLatency` - p99 latency > 2s for 10 minutes
- `HighMemory` - JVM heap > 80% for 15 minutes
- `LowConnectionPool` - DB connection pool > 80% for 10 minutes
- `RateLimitHit` - Rate limit exceeded 100 times/hour

### 4.3 Alert Response Runbook
1. Check Grafana dashboard for trends
2. Review application logs for errors
3. Check database performance
4. Verify external dependencies (LLM provider, Redis)
5. Scale horizontally if needed
6. Escalate to on-call engineer if unresolved in 15 minutes

## 5. Deployment

### 5.1 Prerequisites
- Kubernetes cluster (v1.24+)
- Prometheus + Grafana stack
- Redis cluster (for rate limiting)
- PostgreSQL database

### 5.2 Deployment Commands
```bash
# Build
mvn clean package -DskipTests

# Deploy to Kubernetes
kubectl apply -f k8s/

# Verify
kubectl get pods -n agent-hub
kubectl logs -f deployment/backend -n agent-hub
```

### 5.3 Rollback Procedure
```bash
kubectl rollout undo deployment/backend -n agent-hub
kubectl rollout status deployment/backend -n agent-hub
```

## 6. Dashboard URLs

- **Grafana**: http://grafana.agent-hub.internal
- **Prometheus**: http://prometheus.agent-hub.internal
- **Jaeger** (Tracing): http://jaeger.agent-hub.internal

## 7. On-Call Responsibilities

- Respond to alerts within 15 minutes
- Check dashboards and logs
- Document incidents in runbook
- Escalate if cannot resolve within 30 minutes
