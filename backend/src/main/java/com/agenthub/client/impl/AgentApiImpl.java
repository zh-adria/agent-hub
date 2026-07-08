package com.agenthub.client.impl;

import com.agenthub.client.api.AgentApi;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class AgentApiImpl implements AgentApi {
    private final Map<Long, Map<String, Object>> agents = new ConcurrentHashMap<>();
    private final Map<Long, Set<String>> agentFunctions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Object createAgent(Map<String, Object> agentConfig) {
        Long id = idGenerator.getAndIncrement();
        agentConfig.put("id", id);
        agents.put(id, agentConfig);
        return agentConfig;
    }
    
    @Override
    public Object getAgent(Long agentId) {
        return agents.get(agentId);
    }
    
    @Override
    public List<Map<String, Object>> listAgents() {
        return new ArrayList<>(agents.values());
    }
    
    @Override
    public Object updateAgent(Long agentId, Map<String, Object> updates) {
        Map<String, Object> agent = agents.get(agentId);
        if (agent == null) {
            throw new IllegalArgumentException("Agent not found: " + agentId);
        }
        agent.putAll(updates);
        return agent;
    }
    
    @Override
    public void deleteAgent(Long agentId) {
        agents.remove(agentId);
        agentFunctions.remove(agentId);
    }
    
    @Override
    public void bindFunction(Long agentId, String functionName) {
        agentFunctions.computeIfAbsent(agentId, k -> new HashSet<>()).add(functionName);
    }
    
    @Override
    public void unbindFunction(Long agentId, String functionName) {
        Set<String> functions = agentFunctions.get(agentId);
        if (functions != null) {
            functions.remove(functionName);
        }
    }
    
    @Override
    public List<Map<String, Object>> getAgentFunctions(Long agentId) {
        Set<String> functions = agentFunctions.get(agentId);
        if (functions == null) return List.of();
        return functions.stream().map(f -> Map.of("name", f)).toList();
    }
}
