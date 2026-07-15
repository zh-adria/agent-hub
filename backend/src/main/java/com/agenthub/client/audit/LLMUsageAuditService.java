package com.agenthub.client.audit;

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
}
