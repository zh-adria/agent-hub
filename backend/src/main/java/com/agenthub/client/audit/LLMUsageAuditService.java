package com.agenthub.client.audit;

import java.util.List;

public interface LLMUsageAuditService {
    void record(LLMUsageAuditRecord record);

    List<LLMUsageAuditRecord> listRecords();
}
