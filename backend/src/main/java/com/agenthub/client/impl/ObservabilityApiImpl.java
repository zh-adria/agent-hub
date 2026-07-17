package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.client.audit.LLMUsageAuditRecord;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.BotWebhookEventJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/observability")
public class ObservabilityApiImpl {
    private final TraceJpaRepository traceRepository;
    private final StepRecordJpaRepository stepRecordRepository;
    private final BotWebhookEventJpaRepository webhookEventRepository;
    private final LLMUsageAuditService auditService;

    public ObservabilityApiImpl(
            TraceJpaRepository traceRepository,
            StepRecordJpaRepository stepRecordRepository,
            BotWebhookEventJpaRepository webhookEventRepository,
            LLMUsageAuditService auditService) {
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.webhookEventRepository = webhookEventRepository;
        this.auditService = auditService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<LLMUsageAuditRecord> auditRecords = auditService.listRecords();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("traceCount", traceRepository.findByTenantIdOrderByStartedAtDesc(TenantContext.tenantId()).size());
        response.put("traceSucceededCount", traceRepository.countByTenantIdAndStatus(TenantContext.tenantId(), "SUCCEEDED"));
        response.put("traceFailedCount", traceRepository.countByTenantIdAndStatus(TenantContext.tenantId(), "FAILED"));
        response.put("stepRecordCount", stepRecordRepository.countByTenantId(TenantContext.tenantId()));
        response.put("stepSucceededCount", stepRecordRepository.countByTenantIdAndStatus(TenantContext.tenantId(), "SUCCEEDED"));
        response.put("stepFailedCount", stepRecordRepository.countByTenantIdAndStatus(TenantContext.tenantId(), "FAILED"));
        response.put("llmAuditRecordCount", auditRecords.size());
        response.put("llmPromptTokens", sumPromptTokens(auditRecords));
        response.put("llmCompletionTokens", sumCompletionTokens(auditRecords));
        response.put("llmTotalTokens", sumTotalTokens(auditRecords));
        response.put("llmTotalCost", sumCost(auditRecords));
        return response;
    }

    @GetMapping("/alerts")
    public Map<String, Object> alerts() {
        long tenantId = TenantContext.tenantId();
        long traceCount = traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).size();
        long traceFailed = traceRepository.countByTenantIdAndStatus(tenantId, "FAILED");
        long stepCount = stepRecordRepository.countByTenantId(tenantId);
        long stepFailed = stepRecordRepository.countByTenantIdAndStatus(tenantId, "FAILED");
        List<LLMUsageAuditRecord> auditRecords = auditService.listRecords();

        Map<String, Object> metrics = new LinkedHashMap<>();
        metrics.put("traceFailureRate", rate(traceFailed, traceCount));
        metrics.put("stepFailureRate", rate(stepFailed, stepCount));
        metrics.put("llmTotalTokens", sumTotalTokens(auditRecords));
        metrics.put("llmTotalCost", sumCost(auditRecords));
        metrics.put("webhookEventCount", webhookEventRepository.countByTenantId(tenantId));

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

    @GetMapping("/security-baseline")
    public Map<String, Object> securityBaseline() {
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

    private int sumPromptTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getPromptTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private int sumCompletionTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getCompletionTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private int sumTotalTokens(List<LLMUsageAuditRecord> records) {
        return records.stream().map(LLMUsageAuditRecord::getTotalTokens).filter(value -> value != null).mapToInt(Integer::intValue).sum();
    }

    private BigDecimal sumCost(List<LLMUsageAuditRecord> records) {
        BigDecimal total = BigDecimal.ZERO;
        for (LLMUsageAuditRecord record : records) {
            if (record.getCost() != null) {
                total = total.add(record.getCost());
            }
        }
        return total;
    }

    private double rate(long failed, long total) {
        if (total <= 0) {
            return 0d;
        }
        return Math.round((failed * 10000.0d / total)) / 10000.0d;
    }
}
