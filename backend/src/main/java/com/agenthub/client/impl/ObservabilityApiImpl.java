package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.client.audit.LLMUsageAuditRecord;
import com.agenthub.domain.context.TenantContext;
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
    private final LLMUsageAuditService auditService;

    public ObservabilityApiImpl(
            TraceJpaRepository traceRepository,
            StepRecordJpaRepository stepRecordRepository,
            LLMUsageAuditService auditService) {
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.auditService = auditService;
    }

    @GetMapping("/summary")
    public Map<String, Object> summary() {
        List<LLMUsageAuditRecord> auditRecords = auditService.listRecords();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("traceCount", traceRepository.findByTenantIdOrderByStartedAtDesc(TenantContext.tenantId()).size());
        response.put("stepRecordCount", stepRecordRepository.countByTenantId(TenantContext.tenantId()));
        response.put("llmAuditRecordCount", auditRecords.size());
        response.put("llmPromptTokens", sumPromptTokens(auditRecords));
        response.put("llmCompletionTokens", sumCompletionTokens(auditRecords));
        response.put("llmTotalTokens", sumTotalTokens(auditRecords));
        response.put("llmTotalCost", sumCost(auditRecords));
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
}
