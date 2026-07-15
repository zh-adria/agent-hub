package com.agenthub.client.audit;

public class LLMUsageAuditFilter {
    private String agentId;
    private String agentSessionId;
    private String traceId;
    private String userId;

    public boolean matches(LLMUsageAuditRecord record) {
        if (record == null) {
            return false;
        }
        return matches(agentId, record.getAgentId())
                && matches(agentSessionId, record.getAgentSessionId())
                && matches(traceId, record.getTraceId())
                && matches(userId, record.getUserId());
    }

    private boolean matches(String expected, String actual) {
        return expected == null || expected.trim().isEmpty() || expected.equals(actual);
    }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getAgentSessionId() { return agentSessionId; }
    public void setAgentSessionId(String agentSessionId) { this.agentSessionId = agentSessionId; }
    public String getTraceId() { return traceId; }
    public void setTraceId(String traceId) { this.traceId = traceId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
}
