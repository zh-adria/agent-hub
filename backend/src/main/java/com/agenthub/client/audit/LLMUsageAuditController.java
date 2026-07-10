package com.agenthub.client.audit;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class LLMUsageAuditController {
    private final LLMUsageAuditService auditService;

    public LLMUsageAuditController(LLMUsageAuditService auditService) {
        this.auditService = auditService;
    }

    @GetMapping("/api/audit/llm-usage")
    public List<LLMUsageAuditRecord> listRecords() {
        return auditService.listRecords();
    }
}
