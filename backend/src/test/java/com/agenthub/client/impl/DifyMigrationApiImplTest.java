package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.service.FunctionRegistryService;
import com.agenthub.infra.persistence.entity.KnowledgeBaseEntity;
import com.agenthub.infra.persistence.entity.RagDocumentEntity;
import com.agenthub.infra.persistence.entity.WorkflowDefinitionEntity;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.RagDocumentJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class DifyMigrationApiImplTest {

    @AfterEach
    void clearTenantContext() {
        TenantContext.clear();
    }

    @Test
    void preflightReportsDifyExportMappings() {
        TenantContext.set(7L, "tenant-007", "user-1");
        DifyMigrationApiImpl api = api(
                mock(AgentRepository.class),
                mock(FunctionRegistryService.class),
                mock(WorkflowDefinitionJpaRepository.class),
                mock(KnowledgeBaseJpaRepository.class),
                mock(RagDocumentJpaRepository.class));

        Map<String, Object> response = api.preflight(payload());
        Map<String, Object> summary = cast(response.get("summary"));

        assertThat(response.get("ready")).isEqualTo(true);
        assertThat(summary.get("apps")).isEqualTo(1);
        assertThat(summary.get("tools")).isEqualTo(1);
        assertThat(summary.get("workflows")).isEqualTo(1);
        assertThat(summary.get("knowledgeBases")).isEqualTo(1);
        assertThat(summary.get("documents")).isEqualTo(1);
        assertThat((List<?>) response.get("blockers")).isEmpty();
        assertThat(cast(response.get("mappings")).get("agents")).isInstanceOf(List.class);
    }

    @Test
    void importPersistsMappedResources() {
        TenantContext.set(7L, "tenant-007", "user-1");
        AgentRepository agentRepository = mock(AgentRepository.class);
        FunctionRegistryService functionRegistryService = mock(FunctionRegistryService.class);
        WorkflowDefinitionJpaRepository workflowRepository = mock(WorkflowDefinitionJpaRepository.class);
        KnowledgeBaseJpaRepository knowledgeBaseRepository = mock(KnowledgeBaseJpaRepository.class);
        RagDocumentJpaRepository documentRepository = mock(RagDocumentJpaRepository.class);
        when(functionRegistryService.registerFunction(any(FunctionDefinition.class))).thenAnswer(invocation -> {
            FunctionDefinition function = invocation.getArgument(0);
            function.setId("101");
            return function;
        });
        when(agentRepository.save(any(Agent.class))).thenAnswer(invocation -> {
            Agent agent = invocation.getArgument(0);
            agent.setId("201");
            return agent;
        });
        when(workflowRepository.save(any(WorkflowDefinitionEntity.class))).thenAnswer(invocation -> {
            WorkflowDefinitionEntity workflow = invocation.getArgument(0);
            workflow.setId(301L);
            return workflow;
        });
        when(knowledgeBaseRepository.save(any(KnowledgeBaseEntity.class))).thenAnswer(invocation -> {
            KnowledgeBaseEntity knowledgeBase = invocation.getArgument(0);
            knowledgeBase.setId(401L);
            return knowledgeBase;
        });
        when(documentRepository.save(any(RagDocumentEntity.class))).thenAnswer(invocation -> {
            RagDocumentEntity document = invocation.getArgument(0);
            document.setId(501L);
            return document;
        });
        DifyMigrationApiImpl api = api(
                agentRepository,
                functionRegistryService,
                workflowRepository,
                knowledgeBaseRepository,
                documentRepository);

        Map<String, Object> response = api.importExport(payload());
        Map<String, Object> imported = cast(response.get("imported"));

        assertThat((List<?>) imported.get("functions")).hasSize(1);
        assertThat((List<?>) imported.get("agents")).hasSize(1);
        assertThat((List<?>) imported.get("workflows")).hasSize(1);
        assertThat((List<?>) imported.get("knowledgeBases")).hasSize(1);
        assertThat((List<?>) imported.get("documents")).hasSize(1);
        assertThat(first(imported, "agents").get("functionIds")).asList().contains("101");
    }

    private DifyMigrationApiImpl api(
            AgentRepository agentRepository,
            FunctionRegistryService functionRegistryService,
            WorkflowDefinitionJpaRepository workflowRepository,
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            RagDocumentJpaRepository documentRepository) {
        return new DifyMigrationApiImpl(
                agentRepository,
                functionRegistryService,
                workflowRepository,
                knowledgeBaseRepository,
                documentRepository,
                new ObjectMapper());
    }

    private Map<String, Object> payload() {
        Map<String, Object> app = map(
                "name", "Customer Support Bot",
                "description", "Dify app export",
                "prompt", "Answer with policy context",
                "model", "gpt-4o-mini");
        Map<String, Object> tool = map(
                "name", "lookup_order",
                "description", "Order lookup",
                "endpoint", "https://tools.example.test/orders",
                "inputSchema", map("type", "object"));
        Map<String, Object> workflow = map(
                "name", "Support Escalation",
                "nodes", Arrays.asList(map("id", "start", "type", "start")));
        Map<String, Object> knowledge = map(
                "name", "Support KB",
                "documents", Arrays.asList(map("title", "Refund policy", "sourceUri", "dify://doc/refund")));
        return map(
                "app", app,
                "tools", Arrays.asList(tool),
                "workflow", workflow,
                "knowledgeBases", Arrays.asList(knowledge));
    }

    private Map<String, Object> map(Object... pairs) {
        Map<String, Object> result = new LinkedHashMap<>();
        for (int i = 0; i < pairs.length; i += 2) {
            result.put(String.valueOf(pairs[i]), pairs[i + 1]);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> cast(Object value) {
        return (Map<String, Object>) value;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> first(Map<String, Object> imported, String key) {
        return (Map<String, Object>) ((List<?>) imported.get(key)).get(0);
    }
}
