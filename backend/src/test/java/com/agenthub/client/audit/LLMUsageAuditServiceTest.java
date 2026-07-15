package com.agenthub.client.audit;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class LLMUsageAuditServiceTest {

    @Test
    void filtersRecordsByAgentSessionTraceAndUser() {
        InMemoryLLMUsageAuditService service = new InMemoryLLMUsageAuditService();
        service.record(record("agent-1", "session-1", "trace-1", "user-1"));
        service.record(record("agent-2", "session-2", "trace-2", "user-2"));

        LLMUsageAuditFilter filter = new LLMUsageAuditFilter();
        filter.setAgentId("agent-1");
        filter.setAgentSessionId("session-1");
        filter.setTraceId("trace-1");
        filter.setUserId("user-1");

        assertThat(service.listRecords(filter))
                .hasSize(1)
                .first()
                .extracting(LLMUsageAuditRecord::getAgentId)
                .isEqualTo("agent-1");
    }

    @Test
    void emptyFilterKeepsAllRecords() {
        InMemoryLLMUsageAuditService service = new InMemoryLLMUsageAuditService();
        service.record(record("agent-1", "session-1", "trace-1", "user-1"));
        service.record(record("agent-2", "session-2", "trace-2", "user-2"));

        assertThat(service.listRecords(new LLMUsageAuditFilter())).hasSize(2);
    }

    private LLMUsageAuditRecord record(String agentId, String sessionId, String traceId, String userId) {
        LLMUsageAuditRecord record = new LLMUsageAuditRecord();
        record.setAgentId(agentId);
        record.setAgentSessionId(sessionId);
        record.setTraceId(traceId);
        record.setUserId(userId);
        record.setPromptTokens(10);
        record.setCompletionTokens(20);
        record.setTotalTokens(30);
        record.setCost(new BigDecimal("0.12"));
        return record;
    }
}
