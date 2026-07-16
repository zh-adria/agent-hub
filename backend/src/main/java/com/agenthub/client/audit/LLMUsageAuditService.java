package com.agenthub.client.audit;

import java.math.BigDecimal;
import java.util.List;

public interface LLMUsageAuditService {
    void record(LLMUsageAuditRecord record);

    List<LLMUsageAuditRecord> listRecords();

    default List<LLMUsageAuditRecord> listRecords(LLMUsageAuditFilter filter) {
        LLMUsageAuditFilter safeFilter = filter != null ? filter : new LLMUsageAuditFilter();
        return listRecords().stream()
                .filter(safeFilter::matches)
                .collect(java.util.stream.Collectors.toList());
    }

    default LLMUsageAuditSummary summarize(LLMUsageAuditFilter filter) {
        LLMUsageAuditSummary summary = new LLMUsageAuditSummary();
        BigDecimal totalCost = BigDecimal.ZERO;
        for (LLMUsageAuditRecord record : listRecords(filter)) {
            summary.setRecordCount(summary.getRecordCount() + 1);
            summary.setPromptTokens(summary.getPromptTokens() + safeInt(record.getPromptTokens()));
            summary.setCompletionTokens(summary.getCompletionTokens() + safeInt(record.getCompletionTokens()));
            summary.setTotalTokens(summary.getTotalTokens() + safeInt(record.getTotalTokens()));
            totalCost = totalCost.add(record.getCost() != null ? record.getCost() : BigDecimal.ZERO);
        }
        summary.setTotalCost(totalCost);
        return summary;
    }

    default int safeInt(Integer value) {
        return value != null ? value : 0;
    }
}
