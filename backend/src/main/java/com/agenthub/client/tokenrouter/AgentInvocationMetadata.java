package com.agenthub.client.tokenrouter;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class AgentInvocationMetadata {
    private String businessTag;
    private String userId;
    private String policyId;
    private String agentId;
    private String agentSessionId;
    private String agentStepId;
    private String agentStepType;
    private String traceId;
    private List<String> toolNames;
    private String knowledgeBaseId;

    public static AgentInvocationMetadata empty() {
        return new AgentInvocationMetadata();
    }

    public Map<String, Object> toExtensions() {
        Map<String, Object> extensions = new LinkedHashMap<>();
        putIfPresent(extensions, "agentId", agentId);
        putIfPresent(extensions, "agentSessionId", agentSessionId);
        putIfPresent(extensions, "agentStepId", agentStepId);
        putIfPresent(extensions, "agentStepType", agentStepType);
        putIfPresent(extensions, "traceId", traceId);
        putIfPresent(extensions, "toolNames", toolNames);
        putIfPresent(extensions, "knowledgeBaseId", knowledgeBaseId);
        return extensions;
    }

    private void putIfPresent(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
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
}
