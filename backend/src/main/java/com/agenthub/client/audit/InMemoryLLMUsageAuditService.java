package com.agenthub.client.audit;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class InMemoryLLMUsageAuditService implements LLMUsageAuditService {
    private final List<LLMUsageAuditRecord> records = Collections.synchronizedList(new ArrayList<>());

    @Override
    public void record(LLMUsageAuditRecord record) {
        if (record != null) {
            records.add(record);
        }
    }

    @Override
    public List<LLMUsageAuditRecord> listRecords() {
        synchronized (records) {
            return new ArrayList<>(records);
        }
    }
}
