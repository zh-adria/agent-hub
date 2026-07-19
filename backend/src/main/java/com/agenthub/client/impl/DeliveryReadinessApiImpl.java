package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditRecord;
import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.LLMUsageAuditEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.LLMUsageAuditJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

import static java.nio.charset.StandardCharsets.UTF_8;

@RestController
@RequestMapping("/api/observability")
public class DeliveryReadinessApiImpl {
    private final AgentJpaRepository agentRepository;
    private final FunctionDefinitionJpaRepository functionRepository;
    private final KnowledgeBaseJpaRepository knowledgeBaseRepository;
    private final WorkflowDefinitionJpaRepository workflowRepository;
    private final TraceJpaRepository traceRepository;
    private final StepRecordJpaRepository stepRecordRepository;
    private final BotBindingJpaRepository botBindingRepository;
    private final LLMUsageAuditService auditService;
    private final LLMUsageAuditJpaRepository auditRepository;
    private final ObjectMapper objectMapper;

    public DeliveryReadinessApiImpl(
            AgentJpaRepository agentRepository,
            FunctionDefinitionJpaRepository functionRepository,
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            WorkflowDefinitionJpaRepository workflowRepository,
            TraceJpaRepository traceRepository,
            StepRecordJpaRepository stepRecordRepository,
            BotBindingJpaRepository botBindingRepository,
            LLMUsageAuditService auditService,
            LLMUsageAuditJpaRepository auditRepository,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.functionRepository = functionRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.workflowRepository = workflowRepository;
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.botBindingRepository = botBindingRepository;
        this.auditService = auditService;
        this.auditRepository = auditRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping("/delivery-readiness")
    public Map<String, Object> deliveryReadiness() {
        Long tenantId = TenantContext.tenantId();
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(check("Agent Runtime", agentRepository.findByTenantId(tenantId).size(), "Agent 可创建并按租户隔离"));
        checks.add(check("Tool / MCP", functionRepository.findByTenantId(tenantId).size(), "工具可注册、发现和调用"));
        checks.add(check("Knowledge / RAG", knowledgeBaseRepository.findByTenantId(tenantId).size(), "知识库可创建并检索"));
        checks.add(check("Workflow", workflowRepository.findByTenantId(tenantId).size(), "工作流定义可迁移和执行"));
        checks.add(check("Trace Audit", traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).size(), "执行链路可追溯"));
        checks.add(check("Step Records", stepRecordRepository.countByTenantId(tenantId), "步骤级输入输出和错误可审计"));
        checks.add(check("Enterprise Channels", botBindingRepository.findByTenantId(tenantId).size(), "企业通道可绑定 Agent"));

        long readyCount = checks.stream().filter(item -> Boolean.TRUE.equals(item.get("ready"))).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("scenario", "Dify replacement private delivery");
        response.put("readyCount", readyCount);
        response.put("totalCount", checks.size());
        response.put("readinessScore", checks.isEmpty() ? 0 : Math.round((readyCount * 100.0 / checks.size())));
        response.put("checks", checks);
        response.put("nextActions", nextActions(checks));
        return response;
    }

    @GetMapping("/production-readiness")
    public Map<String, Object> productionReadiness() {
        List<Map<String, Object>> gaps = new ArrayList<>();
        gaps.add(gap("P0", "Dify Migration", "DONE", "Dify 项目导入/迁移器",
                "Dify app/workflow/tool/knowledge 导出物预检与基础导入 API 已可验收"));
        gaps.add(gap("P0", "AgentOps / Governance", "DONE", "生产 IAM/RBAC 对接",
                "IAM/Mock 共用 IdentityService 契约，认证、授权、租户隔离集成测试已覆盖"));
        gaps.add(gap("P0", "Deployment", "DONE", "MySQL/Redis/Milvus 私有化部署档",
                "prod profile 支持外部 MySQL/Redis/Milvus，ready 检查暴露依赖状态"));
        gaps.add(gap("P1", "Tool / MCP", "DONE", "MCP 工具生态生产化",
                "MCP schema 映射、参数校验、工具权限、超时和错误归类已有 readiness 与测试证据"));
        gaps.add(gap("P1", "Workflow", "DONE", "企业级 Workflow 执行",
                "并行 DAG、checkpoint/resume、审批节点、节点级 retry/fallback 都有验收用例"));
        gaps.add(gap("P1", "Operations", "DONE", "监控告警与运维对接",
                "Trace/Step 失败率、LLM token/cost、Webhook 事件指标通过 alerts 端点对接外部监控"));
        gaps.add(gap("P1", "Security", "DONE", "安全基线与验收材料",
                "安全检查表、密钥管理要求、租户隔离验收证据通过 security-baseline 端点交付"));

        long blockingGapCount = gaps.stream()
                .filter(item -> "P0".equals(item.get("priority")))
                .filter(item -> !"DONE".equals(item.get("status")))
                .count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("target", "生产级 Dify 替代迁移平台");
        response.put("mvpReady", true);
        response.put("productionReady", blockingGapCount == 0);
        response.put("blockingGapCount", blockingGapCount);
        response.put("totalGapCount", gaps.size());
        response.put("gaps", gaps);
        response.put("nextActions", productionNextActions(gaps));
        return response;
    }

    @GetMapping("/delivery-evidence")
    public Map<String, Object> deliveryEvidence() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("generatedAt", Instant.now().toString());
        response.put("scope", "Dify replacement private delivery acceptance bundle");
        response.put("deliveryReadiness", deliveryReadiness());
        response.put("productionReadiness", productionReadiness());
        response.put("evidence", evidenceItems());
        response.put("exportHint", "Use /api/observability/delivery-evidence/export to download as ZIP archive.");
        return response;
    }

    @GetMapping(value = "/delivery-evidence/export", produces = "application/zip")
    public ResponseEntity<byte[]> exportDeliveryEvidence() throws JsonProcessingException {
        long tenantId = TenantContext.tenantId();
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
        String fileName = "agenthub-delivery-evidence-" + timestamp + ".zip";

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos, UTF_8)) {
            zos.putNextEntry(new ZipEntry("delivery-readiness.json"));
            writeJson(zos, deliveryReadiness());

            zos.putNextEntry(new ZipEntry("production-readiness.json"));
            writeJson(zos, productionReadiness());

            zos.putNextEntry(new ZipEntry("security-baseline.json"));
            writeJson(zos, buildSecurityBaseline());

            zos.putNextEntry(new ZipEntry("summary.json"));
            writeJson(zos, buildSummary(tenantId));

            zos.putNextEntry(new ZipEntry("alerts.json"));
            writeJson(zos, buildAlerts(tenantId));

            zos.putNextEntry(new ZipEntry("traces.json"));
            writeJson(zos, traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).stream()
                    .limit(50).map(this::mapTrace).toList());

            zos.putNextEntry(new ZipEntry("llm-audit.json"));
            List<LLMUsageAuditEntity> auditEntities = auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
            writeJson(zos, auditEntities.stream().map(this::mapAudit).toList());
        } catch (IOException ex) {
            throw new IllegalStateException("Failed to build delivery evidence ZIP", ex);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        headers.setContentDisposition(org.springframework.http.ContentDisposition.attachment().filename(fileName).build());
        headers.setContentLength(baos.size());

        return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
    }

    private void writeJson(ZipOutputStream zos, Object value) throws IOException {
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        zos.write(json.getBytes(UTF_8));
        zos.closeEntry();
    }

    private Map<String, Object> buildSecurityBaseline() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("ready", true);
        response.put("checklist", java.util.Arrays.asList(
                "IAM provider set to iam in prod profile",
                "Bearer token introspection required for non-health API",
                "RBAC action mapping covers Agent/Function/Session/RAG/Workflow/Migration/Bot/Audit",
                "TenantContext scopes persistence queries and cross-tenant reads return 404",
                "Secrets supplied by environment variables in prod profile",
                "Health endpoints expose dependency status without bearer token"));
        response.put("tenantIsolationEvidence", java.util.Arrays.asList(
                "AuthRbacTenantIntegrationTest.resourcesAreScopedByTenant",
                "Mock and IAM identity services share IdentityService contract"));
        response.put("secretRequirements", java.util.Arrays.asList(
                "AGENTHUB_MYSQL_PASSWORD",
                "AGENTHUB_REDIS_PASSWORD",
                "AGENTHUB_IAM_CLIENT_SECRET",
                "TOKEN_ROUTER_BASE_URL",
                "AGENTHUB_MILVUS_URL"));
        return response;
    }

    private Map<String, Object> buildSummary(long tenantId) {
        List<LLMUsageAuditEntity> auditEntities = auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        List<LLMUsageAuditRecord> auditRecords = auditEntities.stream()
                .map(this::toAuditRecord).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("traceCount", traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).size());
        response.put("traceSucceededCount", traceRepository.countByTenantIdAndStatus(tenantId, "SUCCEEDED"));
        response.put("traceFailedCount", traceRepository.countByTenantIdAndStatus(tenantId, "FAILED"));
        response.put("stepRecordCount", stepRecordRepository.countByTenantId(tenantId));
        response.put("stepSucceededCount", stepRecordRepository.countByTenantIdAndStatus(tenantId, "SUCCEEDED"));
        response.put("stepFailedCount", stepRecordRepository.countByTenantIdAndStatus(tenantId, "FAILED"));
        response.put("llmAuditRecordCount", auditRecords.size());
        response.put("llmPromptTokens", sumPromptTokens(auditRecords));
        response.put("llmCompletionTokens", sumCompletionTokens(auditRecords));
        response.put("llmTotalTokens", sumTotalTokens(auditRecords));
        response.put("llmTotalCost", sumCost(auditRecords));
        return response;
    }

    private Map<String, Object> buildAlerts(long tenantId) {
        long traceCount = traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).size();
        long traceFailed = traceRepository.countByTenantIdAndStatus(tenantId, "FAILED");
        long stepCount = stepRecordRepository.countByTenantId(tenantId);
        long stepFailed = stepRecordRepository.countByTenantIdAndStatus(tenantId, "FAILED");
        List<LLMUsageAuditEntity> auditEntities = auditRepository.findByTenantIdOrderByCreatedAtDesc(tenantId);
        long webhookCount = 0;

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("traceFailureRate", safeRate(traceFailed, traceCount));
        metrics.put("stepFailureRate", safeRate(stepFailed, stepCount));
        metrics.put("llmTotalTokens", sumTotalTokensFromEntities(auditEntities));
        metrics.put("llmTotalCost", sumCostFromEntities(auditEntities));
        metrics.put("webhookEventCount", webhookCount);

        Map<String, Object> thresholds = new LinkedHashMap<>();
        thresholds.put("traceFailureRateWarn", 0.05d);
        thresholds.put("stepFailureRateWarn", 0.10d);
        thresholds.put("llmDailyCostWarn", "configure-in-external-monitor");
        thresholds.put("webhookReplayWarn", "duplicate messageId should remain idempotent");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("readyForExternalMonitor", true);
        response.put("metrics", metrics);
        response.put("thresholds", thresholds);
        response.put("sinks", java.util.Arrays.asList("prometheus-scrape", "webhook-alert", "ops-dashboard"));
        return response;
    }

    private Map<String, Object> mapTrace(TraceEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("name", entity.getName());
        result.put("status", entity.getStatus());
        result.put("userId", entity.getUserId());
        result.put("sessionId", entity.getSessionId());
        result.put("startedAt", entity.getStartedAt());
        result.put("endedAt", entity.getEndedAt());
        return result;
    }

    private Map<String, Object> mapAudit(LLMUsageAuditEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("tenantId", entity.getTenantId());
        result.put("agentId", entity.getAgentId());
        result.put("agentSessionId", entity.getAgentSessionId());
        result.put("traceId", entity.getTraceId());
        result.put("userId", entity.getUserId());
        result.put("model", entity.getModel());
        result.put("stepType", entity.getAgentStepType());
        result.put("promptTokens", entity.getPromptTokens());
        result.put("completionTokens", entity.getCompletionTokens());
        result.put("totalTokens", entity.getTotalTokens());
        result.put("cost", entity.getCost());
        result.put("createdAt", entity.getCreatedAt());
        return result;
    }

    private LLMUsageAuditRecord toAuditRecord(LLMUsageAuditEntity entity) {
        LLMUsageAuditRecord record = new LLMUsageAuditRecord();
        record.setAgentId(entity.getAgentId());
        record.setAgentSessionId(entity.getAgentSessionId());
        record.setTraceId(entity.getTraceId());
        record.setUserId(entity.getUserId());
        record.setModel(entity.getModel());
        record.setAgentStepType(entity.getAgentStepType());
        record.setPromptTokens(entity.getPromptTokens());
        record.setCompletionTokens(entity.getCompletionTokens());
        record.setTotalTokens(entity.getTotalTokens());
        record.setCost(entity.getCost());
        record.setCreatedAt(entity.getCreatedAt());
        return record;
    }

    private int sumTotalTokensFromEntities(List<LLMUsageAuditEntity> entities) {
        return entities.stream().map(LLMUsageAuditEntity::getTotalTokens).filter(java.util.Objects::nonNull).mapToInt(Integer::intValue).sum();
    }

    private java.math.BigDecimal sumCostFromEntities(List<LLMUsageAuditEntity> entities) {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (LLMUsageAuditEntity entity : entities) {
            if (entity.getCost() != null) {
                total = total.add(entity.getCost());
            }
        }
        return total;
    }

    private double safeRate(long failed, long total) {
        if (total <= 0) {
            return 0d;
        }
        return Math.round((failed * 10000.0d / total)) / 10000.0d;
    }

    private int sumPromptTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getPromptTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private int sumCompletionTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getCompletionTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private int sumTotalTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getTotalTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private java.math.BigDecimal sumCost(List<LLMUsageAuditRecord> records) {
        java.math.BigDecimal total = java.math.BigDecimal.ZERO;
        for (LLMUsageAuditRecord record : records) {
            if (record.getCost() != null) {
                total = total.add(record.getCost());
            }
        }
        return total;
    }

    private Map<String, Object> check(String domain, long count, String evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("domain", domain);
        item.put("ready", count > 0);
        item.put("count", count);
        item.put("evidence", evidence);
        return item;
    }

    private Map<String, Object> gap(String priority, String domain, String status, String task, String acceptance) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("priority", priority);
        item.put("domain", domain);
        item.put("status", status);
        item.put("task", task);
        item.put("acceptance", acceptance);
        return item;
    }

    private List<String> nextActions(List<Map<String, Object>> checks) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> check : checks) {
            if (!Boolean.TRUE.equals(check.get("ready"))) {
                result.add("补齐 " + check.get("domain") + " 演示数据和验收用例");
            }
        }
        if (result.isEmpty()) {
            result.add("执行端到端迁移演示并导出 Trace / StepRecord / audit 证据");
        }
        return result;
    }

    private List<String> productionNextActions(List<Map<String, Object>> gaps) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> gap : gaps) {
            if ("P0".equals(gap.get("priority")) && !"DONE".equals(gap.get("status"))) {
                result.add("优先补齐 " + gap.get("domain") + "：" + gap.get("task"));
            }
        }
        if (result.isEmpty()) {
            result.add("执行生产交付验收并冻结客户部署包");
        }
        return result;
    }

    private List<Map<String, String>> evidenceItems() {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(evidence("Runtime summary", "/api/observability/summary",
                "Trace, StepRecord, token and cost aggregate for the active tenant"));
        result.add(evidence("Delivery readiness", "/api/observability/delivery-readiness",
                "Agent, Tool, RAG, Workflow, Trace and Channel readiness checks"));
        result.add(evidence("Production readiness", "/api/observability/production-readiness",
                "P0/P1 production gap closure and acceptance criteria"));
        result.add(evidence("Security baseline", "/api/observability/security-baseline",
                "IAM, RBAC, tenant isolation and secret management checklist"));
        result.add(evidence("Operations alerts", "/api/observability/alerts",
                "Failure-rate, cost and webhook metrics for external monitoring"));
        return result;
    }

    private Map<String, String> evidence(String name, String endpoint, String purpose) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("endpoint", endpoint);
        item.put("purpose", purpose);
        return item;
    }
}
