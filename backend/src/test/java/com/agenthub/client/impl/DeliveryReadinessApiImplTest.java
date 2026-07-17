package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DeliveryReadinessApiImplTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void reportsReadinessChecksAndNextActions() {
        TenantContext.set(7L, "tenant-007", "user-1");
        AgentJpaRepository agentRepository = mock(AgentJpaRepository.class);
        FunctionDefinitionJpaRepository functionRepository = mock(FunctionDefinitionJpaRepository.class);
        KnowledgeBaseJpaRepository knowledgeBaseRepository = mock(KnowledgeBaseJpaRepository.class);
        WorkflowDefinitionJpaRepository workflowRepository = mock(WorkflowDefinitionJpaRepository.class);
        TraceJpaRepository traceRepository = mock(TraceJpaRepository.class);
        StepRecordJpaRepository stepRecordRepository = mock(StepRecordJpaRepository.class);
        BotBindingJpaRepository botBindingRepository = mock(BotBindingJpaRepository.class);
        when(agentRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(functionRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(knowledgeBaseRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        when(workflowRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        when(traceRepository.findByTenantIdOrderByStartedAtDesc(7L)).thenReturn(Collections.singletonList(null));
        when(stepRecordRepository.countByTenantId(7L)).thenReturn(2L);
        when(botBindingRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        DeliveryReadinessApiImpl api = new DeliveryReadinessApiImpl(
                agentRepository,
                functionRepository,
                knowledgeBaseRepository,
                workflowRepository,
                traceRepository,
                stepRecordRepository,
                botBindingRepository);

        Map<String, Object> response = api.deliveryReadiness();

        assertThat(response.get("tenantId")).isEqualTo("tenant-007");
        assertThat(response.get("readyCount")).isEqualTo(4L);
        assertThat(response.get("totalCount")).isEqualTo(7);
        assertThat((List<?>) response.get("checks")).hasSize(7);
        assertThat((List<?>) response.get("nextActions")).isNotEmpty();
    }

    @Test
    void reportsProductionReadinessGapsSeparatelyFromMvpReadiness() {
        TenantContext.set(7L, "tenant-007", "user-1");
        DeliveryReadinessApiImpl api = new DeliveryReadinessApiImpl(
                mock(AgentJpaRepository.class),
                mock(FunctionDefinitionJpaRepository.class),
                mock(KnowledgeBaseJpaRepository.class),
                mock(WorkflowDefinitionJpaRepository.class),
                mock(TraceJpaRepository.class),
                mock(StepRecordJpaRepository.class),
                mock(BotBindingJpaRepository.class));

        Map<String, Object> response = api.productionReadiness();

        assertThat(response.get("tenantId")).isEqualTo("tenant-007");
        assertThat(response.get("mvpReady")).isEqualTo(true);
        assertThat(response.get("productionReady")).isEqualTo(true);
        assertThat(response.get("blockingGapCount")).isEqualTo(0L);
        assertThat((List<?>) response.get("gaps")).hasSize(7);
        assertThat((List<?>) response.get("nextActions")).isNotEmpty();
    }
}
