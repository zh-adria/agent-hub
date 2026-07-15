package com.agenthub.client.audit;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.LLMUsageAuditEntity;
import com.agenthub.infra.persistence.repository.LLMUsageAuditJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class JpaLLMUsageAuditService implements LLMUsageAuditService {
    private final LLMUsageAuditJpaRepository repository;
    private final ObjectMapper objectMapper;

    public JpaLLMUsageAuditService(LLMUsageAuditJpaRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    @Override
    public void record(LLMUsageAuditRecord record) {
        if (record == null) {
            return;
        }
        repository.save(toEntity(record));
    }

    @Override
    public List<LLMUsageAuditRecord> listRecords() {
        List<LLMUsageAuditRecord> result = new ArrayList<>();
        for (LLMUsageAuditEntity entity : repository.findByTenantIdOrderByCreatedAtDesc(TenantContext.tenantId())) {
            result.add(toRecord(entity));
        }
        return result;
    }

    private LLMUsageAuditEntity toEntity(LLMUsageAuditRecord record) {
        LLMUsageAuditEntity entity = new LLMUsageAuditEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setBusinessTag(record.getBusinessTag());
        entity.setUserId(record.getUserId());
        entity.setPolicyId(record.getPolicyId());
        entity.setAgentId(record.getAgentId());
        entity.setAgentSessionId(record.getAgentSessionId());
        entity.setAgentStepId(record.getAgentStepId());
        entity.setAgentStepType(record.getAgentStepType());
        entity.setTraceId(record.getTraceId());
        entity.setToolNames(toJson(record.getToolNames()));
        entity.setKnowledgeBaseId(record.getKnowledgeBaseId());
        entity.setProvider(record.getProvider());
        entity.setModel(record.getModel());
        entity.setPromptTokens(record.getPromptTokens());
        entity.setCompletionTokens(record.getCompletionTokens());
        entity.setTotalTokens(record.getTotalTokens());
        entity.setCost(record.getCost());
        entity.setRouteDecision(record.getRouteDecision());
        entity.setRouteReason(record.getRouteReason());
        entity.setCreatedAt(record.getCreatedAt());
        return entity;
    }

    private LLMUsageAuditRecord toRecord(LLMUsageAuditEntity entity) {
        LLMUsageAuditRecord record = new LLMUsageAuditRecord();
        record.setBusinessTag(entity.getBusinessTag());
        record.setUserId(entity.getUserId());
        record.setPolicyId(entity.getPolicyId());
        record.setAgentId(entity.getAgentId());
        record.setAgentSessionId(entity.getAgentSessionId());
        record.setAgentStepId(entity.getAgentStepId());
        record.setAgentStepType(entity.getAgentStepType());
        record.setTraceId(entity.getTraceId());
        record.setToolNames(fromJson(entity.getToolNames()));
        record.setKnowledgeBaseId(entity.getKnowledgeBaseId());
        record.setProvider(entity.getProvider());
        record.setModel(entity.getModel());
        record.setPromptTokens(entity.getPromptTokens());
        record.setCompletionTokens(entity.getCompletionTokens());
        record.setTotalTokens(entity.getTotalTokens());
        record.setCost(entity.getCost());
        record.setRouteDecision(entity.getRouteDecision());
        record.setRouteReason(entity.getRouteReason());
        record.setCreatedAt(entity.getCreatedAt());
        return record;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : new ArrayList<>());
        } catch (Exception ex) {
            return "[]";
        }
    }

    private List<String> fromJson(String value) {
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }
}
