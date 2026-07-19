package com.agenthub.infra.persistence.entity;

import javax.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ah_llm_usage_audit")
public class LLMUsageAuditEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "business_tag", length = 128)
    private String businessTag;

    @Column(name = "user_id", length = 128)
    private String userId;

    @Column(name = "policy_id", length = 128)
    private String policyId;

    @Column(name = "agent_id", length = 64)
    private String agentId;

    @Column(name = "agent_session_id", length = 64)
    private String agentSessionId;

    @Column(name = "agent_step_id", length = 64)
    private String agentStepId;

    @Column(name = "agent_step_type", length = 64)
    private String agentStepType;

    @Column(name = "trace_id", length = 64)
    private String traceId;

    @Column(name = "tool_names", columnDefinition = "TEXT")
    private String toolNames;

    @Column(name = "knowledge_base_id", length = 64)
    private String knowledgeBaseId;

    @Column(name = "provider", length = 64)
    private String provider;

    @Column(name = "model", length = 128)
    private String model;

    @Column(name = "prompt_tokens")
    private Integer promptTokens;

    @Column(name = "completion_tokens")
    private Integer completionTokens;

    @Column(name = "total_tokens")
    private Integer totalTokens;

    @Column(name = "cost", precision = 12, scale = 6)
    private BigDecimal cost;

    @Column(name = "route_decision", length = 64)
    private String routeDecision;

    @Column(name = "route_reason", columnDefinition = "TEXT")
    private String routeReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
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
    public String getToolNames() { return toolNames; }
    public void setToolNames(String toolNames) { this.toolNames = toolNames; }
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
