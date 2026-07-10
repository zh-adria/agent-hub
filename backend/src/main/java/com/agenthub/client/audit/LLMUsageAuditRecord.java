package com.agenthub.client.audit;

import com.agenthub.client.tokenrouter.AgentInvocationMetadata;
import com.agenthub.client.tokenrouter.TokenRouterChatResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class LLMUsageAuditRecord {
    private String businessTag;
    private String userId;
    private String policyId;
    private String agentId;
    private String agentSessionId;
    private String agentStepId;
    private String agentStepType;
    private String traceId;
    private List<String> toolNames = new ArrayList<>();
    private String knowledgeBaseId;
    private String provider;
    private String model;
    private Integer promptTokens;
    private Integer completionTokens;
    private Integer totalTokens;
    private BigDecimal cost;
    private String routeDecision;
    private String routeReason;
    private LocalDateTime createdAt;

    public static LLMUsageAuditRecord from(AgentInvocationMetadata metadata, TokenRouterChatResponse response) {
        AgentInvocationMetadata safeMetadata = metadata != null ? metadata : AgentInvocationMetadata.empty();
        LLMUsageAuditRecord record = new LLMUsageAuditRecord();
        record.setBusinessTag(safeMetadata.getBusinessTag());
        record.setUserId(safeMetadata.getUserId());
        record.setPolicyId(safeMetadata.getPolicyId());
        record.setAgentId(safeMetadata.getAgentId());
        record.setAgentSessionId(safeMetadata.getAgentSessionId());
        record.setAgentStepId(safeMetadata.getAgentStepId());
        record.setAgentStepType(safeMetadata.getAgentStepType());
        record.setTraceId(safeMetadata.getTraceId());
        record.setKnowledgeBaseId(safeMetadata.getKnowledgeBaseId());
        if (safeMetadata.getToolNames() != null) {
            record.setToolNames(new ArrayList<>(safeMetadata.getToolNames()));
        }
        if (response != null) {
            record.setProvider(response.getProvider());
            record.setModel(response.getModel());
            record.setPromptTokens(response.getPromptTokens());
            record.setCompletionTokens(response.getCompletionTokens());
            record.setTotalTokens(response.getTotalTokens());
            record.setCost(response.getCost());
            record.setRouteDecision(response.getRouteDecision());
            record.setRouteReason(response.getRouteReason());
        }
        record.setCreatedAt(LocalDateTime.now());
        return record;
    }

    public String getBusinessTag() { return businessTag; }
    public void setBusinessTag(String businessTag) { this.businessTag = businessTag; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAgentSessionId() { return agentSessionId; }
    public void setAgentSessionId(String agentSessionId) { this.agentSessionId = agentSessionId; }
    public String getAgentStepId() { return agentStepId; }
    public void setAgentStepId(String agentStepId) { this.agentStepId = agentStepId; }
    public String getAgentStepType() { return agentStepType; }
    public void setAgentStepType(String agentStepType) { this.agentStepType = agentStepType; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public List<String> getToolNames() { return toolNames; }
    public void setToolNames(List<String> toolNames) { this.toolNames = toolNames; }
    public String getKnowledgeBaseId() { return knowledgeBaseId; }
    public void setKnowledgeBaseId(String knowledgeBaseId) { this.knowledgeBaseId = knowledgeBaseId; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Integer getPromptTokens() { return promptTokens; }
    public void setPromptTokens(Integer promptTokens) { this.promptTokens = promptTokens; }
    public Integer getCompletionTokens() { return completionTokens; }
    public void setCompletionTokens(Integer completionTokens) { this.completionTokens = completionTokens; }
    public Integer getTotalTokens() { return totalTokens; }
    public void setTotalTokens(Integer totalTokens) { this.totalTokens = totalTokens; }
    public BigDecimal getCost() { return cost; }
    public void setCost(BigDecimal cost) { this.cost = cost; }
    public String getRouteDecision() { return routeDecision; }
    public void setRouteDecision(String routeDecision) { this.routeDecision = routeDecision; }
    public String getRouteReason() { return routeReason; }
    public void setRouteReason(String routeReason) { this.routeReason = routeReason; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
