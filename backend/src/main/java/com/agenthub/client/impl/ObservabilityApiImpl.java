package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("traceCount", traceRepository.findByTenantIdOrderByStartedAtDesc(TenantContext.tenantId()).size());
        response.put("stepRecordCount", stepRecordRepository.countByTenantId(TenantContext.tenantId()));
        response.put("llmAuditRecordCount", auditService.listRecords().size());
        return response;
    }
}
