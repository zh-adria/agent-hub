package com.agenthub.client.impl;

import com.agenthub.client.audit.InMemoryLLMUsageAuditService;
import com.agenthub.client.audit.LLMUsageAuditRecord;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.BotWebhookEventJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ObservabilityApiImplTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void summaryIncludesStatusAndLlmUsageAggregates() {
        TenantContext.set(7L, "tenant-007", "user-1");
        TraceJpaRepository traceRepository = mock(TraceJpaRepository.class);
        StepRecordJpaRepository stepRecordRepository = mock(StepRecordJpaRepository.class);
        BotWebhookEventJpaRepository webhookEventRepository = mock(BotWebhookEventJpaRepository.class);
        InMemoryLLMUsageAuditService auditService = new InMemoryLLMUsageAuditService();
        auditService.record(record(10, 20, 30, "0.12"));
        auditService.record(record(5, 15, 20, "0.18"));
        when(traceRepository.findByTenantIdOrderByStartedAtDesc(7L)).thenReturn(Collections.emptyList());
        when(traceRepository.countByTenantIdAndStatus(7L, "SUCCEEDED")).thenReturn(2L);
        when(traceRepository.countByTenantIdAndStatus(7L, "FAILED")).thenReturn(1L);
        when(stepRecordRepository.countByTenantId(7L)).thenReturn(9L);
        when(stepRecordRepository.countByTenantIdAndStatus(7L, "SUCCEEDED")).thenReturn(7L);
        when(stepRecordRepository.countByTenantIdAndStatus(7L, "FAILED")).thenReturn(2L);
        when(webhookEventRepository.countByTenantId(7L)).thenReturn(4L);
        ObservabilityApiImpl api = new ObservabilityApiImpl(traceRepository, stepRecordRepository, webhookEventRepository, auditService);

        Map<String, Object> summary = api.summary();
        Map<String, Object> alerts = api.alerts();
        Map<String, Object> security = api.securityBaseline();

        assertThat(summary.get("tenantId")).isEqualTo("tenant-007");
        assertThat(summary.get("traceSucceededCount")).isEqualTo(2L);
        assertThat(summary.get("traceFailedCount")).isEqualTo(1L);
        assertThat(summary.get("stepRecordCount")).isEqualTo(9L);
        assertThat(summary.get("stepSucceededCount")).isEqualTo(7L);
        assertThat(summary.get("stepFailedCount")).isEqualTo(2L);
        assertThat(summary.get("llmPromptTokens")).isEqualTo(15);
        assertThat(summary.get("llmCompletionTokens")).isEqualTo(35);
        assertThat(summary.get("llmTotalTokens")).isEqualTo(50);
        assertThat(summary.get("llmTotalCost")).isEqualTo(new BigDecimal("0.30"));
        assertThat(alerts.get("readyForExternalMonitor")).isEqualTo(true);
        assertThat(((Map<?, ?>) alerts.get("metrics")).get("webhookEventCount")).isEqualTo(4L);
        assertThat(security.get("ready")).isEqualTo(true);
        assertThat(security.get("checklist")).asList().isNotEmpty();
    }

    private LLMUsageAuditRecord record(int promptTokens, int completionTokens, int totalTokens, String cost) {
        LLMUsageAuditRecord record = new LLMUsageAuditRecord();
        record.setPromptTokens(promptTokens);
        record.setCompletionTokens(completionTokens);
        record.setTotalTokens(totalTokens);
        record.setCost(new BigDecimal(cost));
        return record;
    }
}
