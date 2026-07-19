package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.LLMUsageAuditJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
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
        LLMUsageAuditService auditService = mock(LLMUsageAuditService.class);
        LLMUsageAuditJpaRepository auditRepository = mock(LLMUsageAuditJpaRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
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
                botBindingRepository,
                auditService,
                auditRepository,
                objectMapper);

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
                mock(BotBindingJpaRepository.class),
                mock(LLMUsageAuditService.class),
                mock(LLMUsageAuditJpaRepository.class),
                new ObjectMapper());

        Map<String, Object> response = api.productionReadiness();

        assertThat(response.get("tenantId")).isEqualTo("tenant-007");
        assertThat(response.get("mvpReady")).isEqualTo(true);
        assertThat(response.get("productionReady")).isEqualTo(true);
        assertThat(response.get("blockingGapCount")).isEqualTo(0L);
        assertThat((List<?>) response.get("gaps")).hasSize(7);
        assertThat((List<?>) response.get("nextActions")).isNotEmpty();
    }

    @Test
    void buildsDeliveryEvidenceBundle() {
        TenantContext.set(7L, "tenant-007", "user-1");
        AgentJpaRepository agentRepository = mock(AgentJpaRepository.class);
        FunctionDefinitionJpaRepository functionRepository = mock(FunctionDefinitionJpaRepository.class);
        KnowledgeBaseJpaRepository knowledgeBaseRepository = mock(KnowledgeBaseJpaRepository.class);
        WorkflowDefinitionJpaRepository workflowRepository = mock(WorkflowDefinitionJpaRepository.class);
        TraceJpaRepository traceRepository = mock(TraceJpaRepository.class);
        StepRecordJpaRepository stepRecordRepository = mock(StepRecordJpaRepository.class);
        BotBindingJpaRepository botBindingRepository = mock(BotBindingJpaRepository.class);
        LLMUsageAuditService auditService = mock(LLMUsageAuditService.class);
        LLMUsageAuditJpaRepository auditRepository = mock(LLMUsageAuditJpaRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(agentRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(functionRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(knowledgeBaseRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(workflowRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(traceRepository.findByTenantIdOrderByStartedAtDesc(7L)).thenReturn(Collections.singletonList(null));
        when(stepRecordRepository.countByTenantId(7L)).thenReturn(1L);
        when(botBindingRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(auditRepository.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(Collections.emptyList());
        DeliveryReadinessApiImpl api = new DeliveryReadinessApiImpl(
                agentRepository,
                functionRepository,
                knowledgeBaseRepository,
                workflowRepository,
                traceRepository,
                stepRecordRepository,
                botBindingRepository,
                auditService,
                auditRepository,
                objectMapper);

        Map<String, Object> response = api.deliveryEvidence();

        assertThat(response.get("tenantId")).isEqualTo("tenant-007");
        assertThat(response.get("generatedAt")).isNotNull();
        Map<?, ?> deliveryReadiness = (Map<?, ?>) response.get("deliveryReadiness");
        Map<?, ?> productionReadiness = (Map<?, ?>) response.get("productionReadiness");
        assertThat(deliveryReadiness.get("readyCount")).isEqualTo(7L);
        assertThat(productionReadiness.get("productionReady")).isEqualTo(true);
        assertThat((List<?>) response.get("evidence")).hasSize(5);
    }

    @Test
    void exportDeliveryEvidenceReturnsZipBytes() throws Exception {
        TenantContext.set(7L, "tenant-007", "user-1");
        AgentJpaRepository agentRepository = mock(AgentJpaRepository.class);
        FunctionDefinitionJpaRepository functionRepository = mock(FunctionDefinitionJpaRepository.class);
        KnowledgeBaseJpaRepository knowledgeBaseRepository = mock(KnowledgeBaseJpaRepository.class);
        WorkflowDefinitionJpaRepository workflowRepository = mock(WorkflowDefinitionJpaRepository.class);
        TraceJpaRepository traceRepository = mock(TraceJpaRepository.class);
        StepRecordJpaRepository stepRecordRepository = mock(StepRecordJpaRepository.class);
        BotBindingJpaRepository botBindingRepository = mock(BotBindingJpaRepository.class);
        LLMUsageAuditService auditService = mock(LLMUsageAuditService.class);
        LLMUsageAuditJpaRepository auditRepository = mock(LLMUsageAuditJpaRepository.class);
        ObjectMapper objectMapper = new ObjectMapper();
        when(agentRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(functionRepository.findByTenantId(7L)).thenReturn(Collections.singletonList(null));
        when(knowledgeBaseRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        when(workflowRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        when(traceRepository.findByTenantIdOrderByStartedAtDesc(7L)).thenReturn(Collections.emptyList());
        when(stepRecordRepository.countByTenantId(7L)).thenReturn(0L);
        when(botBindingRepository.findByTenantId(7L)).thenReturn(Collections.emptyList());
        when(auditRepository.findByTenantIdOrderByCreatedAtDesc(7L)).thenReturn(Collections.emptyList());
        DeliveryReadinessApiImpl api = new DeliveryReadinessApiImpl(
                agentRepository,
                functionRepository,
                knowledgeBaseRepository,
                workflowRepository,
                traceRepository,
                stepRecordRepository,
                botBindingRepository,
                auditService,
                auditRepository,
                objectMapper);

        var response = api.exportDeliveryEvidence();

        assertThat(response.getStatusCodeValue()).isEqualTo(200);
        assertThat(response.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_OCTET_STREAM);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().length).isGreaterThan(0);
    }
}
