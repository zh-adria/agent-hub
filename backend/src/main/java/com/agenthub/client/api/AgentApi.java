package com.agenthub.client.api;

import java.util.List;
import java.util.Map;

public interface AgentApi {
    // Agent CRUD
    Object createAgent(Map<String, Object> agentConfig);
    Object getAgent(Long agentId);
    List<Map<String, Object>> listAgents();
    Object updateAgent(Long agentId, Map<String, Object> updates);
    void deleteAgent(Long agentId);
    
    // Agent functions
    void bindFunction(Long agentId, String functionName);
    void unbindFunction(Long agentId, String functionName);
    List<Map<String, Object>> getAgentFunctions(Long agentId);
}
