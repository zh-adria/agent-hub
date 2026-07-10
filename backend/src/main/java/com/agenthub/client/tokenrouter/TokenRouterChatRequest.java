package com.agenthub.client.tokenrouter;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TokenRouterChatRequest {
    private String businessTag;
    private String userId;
    private String policyId;
    private String modelHint;
    private List<TokenRouterMessage> messages = new ArrayList<>();
    private Double temperature;
    private Integer maxTokens;
    private boolean stream;
    private Map<String, Object> extensions = new LinkedHashMap<>();

    public String getBusinessTag() { return businessTag; }
    public void setBusinessTag(String businessTag) { this.businessTag = businessTag; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getPolicyId() { return policyId; }
    public void setPolicyId(String policyId) { this.policyId = policyId; }
    public String getModelHint() { return modelHint; }
    public void setModelHint(String modelHint) { this.modelHint = modelHint; }
    public List<TokenRouterMessage> getMessages() { return messages; }
    public void setMessages(List<TokenRouterMessage> messages) { this.messages = messages; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public boolean isStream() { return stream; }
    public void setStream(boolean stream) { this.stream = stream; }
    public Map<String, Object> getExtensions() { return extensions; }
    public void setExtensions(Map<String, Object> extensions) { this.extensions = extensions; }
}
