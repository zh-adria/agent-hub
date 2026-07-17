package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.service.WorkflowExecutionService;
import com.agenthub.infra.persistence.entity.WorkflowDefinitionEntity;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/workflows")
public class WorkflowApiImpl {
    private final WorkflowDefinitionJpaRepository workflowRepository;
    private final WorkflowExecutionService workflowExecutionService;
    private final ObjectMapper objectMapper;

    public WorkflowApiImpl(
            WorkflowDefinitionJpaRepository workflowRepository,
            WorkflowExecutionService workflowExecutionService,
            ObjectMapper objectMapper) {
        this.workflowRepository = workflowRepository;
        this.workflowExecutionService = workflowExecutionService;
        this.objectMapper = objectMapper;
    }

    @PostMapping
    public Map<String, Object> create(@RequestBody Map<String, Object> payload) {
        WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setName(String.valueOf(payload.get("name")));
        entity.setDescription((String) payload.get("description"));
        entity.setDefinition(json(payload.get("definition")));
        entity.setStatus(1);
        entity.setCreatedBy(TenantContext.userId());
        entity.setUpdatedBy(TenantContext.userId());
        return map(workflowRepository.save(entity));
    }

    @GetMapping
    public List<Map<String, Object>> list() {
        return workflowRepository.findByTenantId(TenantContext.tenantId()).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @GetMapping("/{workflowId}")
    public Map<String, Object> get(@PathVariable Long workflowId) {
        return workflowRepository.findByIdAndTenantId(workflowId, TenantContext.tenantId())
                .map(this::map)
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
    }

    @PutMapping("/{workflowId}")
    public Map<String, Object> update(@PathVariable Long workflowId, @RequestBody Map<String, Object> payload) {
        WorkflowDefinitionEntity entity = workflowRepository.findByIdAndTenantId(workflowId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
        if (payload.containsKey("name")) entity.setName(String.valueOf(payload.get("name")));
        if (payload.containsKey("description")) entity.setDescription((String) payload.get("description"));
        if (payload.containsKey("definition")) entity.setDefinition(json(payload.get("definition")));
        entity.setUpdatedBy(TenantContext.userId());
        return map(workflowRepository.save(entity));
    }

    @DeleteMapping("/{workflowId}")
    public void delete(@PathVariable Long workflowId) {
        workflowRepository.findByIdAndTenantId(workflowId, TenantContext.tenantId())
                .ifPresent(workflowRepository::delete);
    }

    @PostMapping("/{workflowId}/execute")
    public Map<String, Object> execute(@PathVariable Long workflowId, @RequestBody Map<String, Object> payload) {
        WorkflowDefinitionEntity workflow = workflowRepository.findByIdAndTenantId(workflowId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
        return workflowExecutionService.execute(workflow, payload);
    }

    @PostMapping("/{workflowId}/resume")
    @SuppressWarnings("unchecked")
    public Map<String, Object> resume(@PathVariable Long workflowId, @RequestBody Map<String, Object> payload) {
        WorkflowDefinitionEntity workflow = workflowRepository.findByIdAndTenantId(workflowId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Workflow not found: " + workflowId));
        Map<String, Object> resumePayload = new LinkedHashMap<>(payload);
        Object checkpoint = payload.get("checkpoint");
        if (checkpoint instanceof Map && !resumePayload.containsKey("approvedNodeId")) {
            Object waitingNodeId = ((Map<String, Object>) checkpoint).get("waitingNodeId");
            if (waitingNodeId != null) {
                resumePayload.put("approvedNodeId", String.valueOf(waitingNodeId));
            }
        }
        return workflowExecutionService.execute(workflow, resumePayload);
    }

    private Map<String, Object> map(WorkflowDefinitionEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("name", entity.getName());
        response.put("description", entity.getDescription());
        response.put("definition", readValue(entity.getDefinition()));
        response.put("status", entity.getStatus());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());
        return response;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : new LinkedHashMap<>());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid workflow definition", ex);
        }
    }

    private Object readValue(String value) {
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception ex) {
            return value;
        }
    }
}
