package com.agenthub.client.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LLMUsageAuditController {
    private final LLMUsageAuditService auditService;

    public LLMUsageAuditController(LLMUsageAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/api/audit/llm-usage")
    public List<LLMUsageAuditRecord> listRecords(
            @RequestParam(required = false) String agentId,
            @RequestParam(required = false) String agentSessionId,
            @RequestParam(required = false) String traceId,
            @RequestParam(required = false) String userId) {
        LLMUsageAuditFilter filter = new LLMUsageAuditFilter();
        filter.setAgentId(agentId);
        filter.setAgentSessionId(agentSessionId);
        filter.setTraceId(traceId);
        filter.setUserId(userId);
        return auditService.listRecords(filter);
    }
}
