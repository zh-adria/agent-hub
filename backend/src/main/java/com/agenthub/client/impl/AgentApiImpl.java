package com.agenthub.client.impl;

import com.agenthub.client.api.AgentApi;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.repository.FunctionRepository;
import com.agenthub.domain.repository.AgentRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/agents")
public class AgentApiImpl implements AgentApi {

    private final AgentRepository agentRepository;
    private final FunctionRepository functionRepository;
    private final ObjectMapper objectMapper;

    public AgentApiImpl(AgentRepository agentRepository, FunctionRepository functionRepository, ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.functionRepository = functionRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @PostMapping
    public Object createAgent(@RequestBody Map<String, Object> agentConfig) {
        Agent agent = mapToDomain(agentConfig);
        Agent saved = agentRepository.save(agent);
        return mapToResponse(saved);
    }

    @Override
    @GetMapping("/{agentId}")
    public Object getAgent(@PathVariable Long agentId) {
        return agentRepository.findById(String.valueOf(agentId))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
    }

    @Override
    @GetMapping
    public List<Map<String, Object>> listAgents() {
        return agentRepository.findAll().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PutMapping("/{agentId}")
    public Object updateAgent(@PathVariable Long agentId, @RequestBody Map<String, Object> updates) {
        Agent existing = agentRepository.findById(String.valueOf(agentId))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));

        if (updates.containsKey("name")) existing.setName((String) updates.get("name"));
        if (updates.containsKey("description")) existing.setDescription((String) updates.get("description"));
        if (updates.containsKey("prompt")) existing.setPrompt((String) updates.get("prompt"));
        if (updates.containsKey("provider")) existing.setProvider((String) updates.get("provider"));
        if (updates.containsKey("model")) existing.setModel((String) updates.get("model"));
        if (updates.containsKey("temperature")) existing.setTemperature(numberAsDouble(updates.get("temperature")));
        if (updates.containsKey("maxTokens")) existing.setMaxTokens(numberAsInteger(updates.get("maxTokens")));
        if (updates.containsKey("maxIterations")) existing.setMaxIterations(numberAsInteger(updates.get("maxIterations")));
        if (updates.containsKey("functionIds")) existing.setFunctionIds(toJsonArray(updates.get("functionIds")));
        if (updates.containsKey("functions")) existing.setFunctionIds(toJsonArray(updates.get("functions")));

        Agent updated = agentRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    @DeleteMapping("/{agentId}")
    public void deleteAgent(@PathVariable Long agentId) {
        agentRepository.deleteById(String.valueOf(agentId));
    }

    @Override
    @PostMapping("/{agentId}/functions/{functionName}")
    public void bindFunction(@PathVariable Long agentId, @PathVariable String functionName) {
        Agent agent = agentRepository.findById(String.valueOf(agentId))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        Set<String> functionIds = new LinkedHashSet<>(parseJsonArray(agent.getFunctionIds()));
        String functionId = functionRepository.findByNameContainingOrDescriptionContaining(functionName, functionName)
                .stream()
                .filter(function -> functionName.equals(function.getName()) || functionName.equals(function.getId()))
                .findFirst()
                .map(function -> function.getId())
                .orElse(functionName);
        functionIds.add(functionId);
        agent.setFunctionIds(toJsonArray(functionIds));
        agentRepository.save(agent);
    }

    @Override
    @DeleteMapping("/{agentId}/functions/{functionName}")
    public void unbindFunction(@PathVariable Long agentId, @PathVariable String functionName) {
        Agent agent = agentRepository.findById(String.valueOf(agentId))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        Set<String> functionIds = new LinkedHashSet<>(parseJsonArray(agent.getFunctionIds()));
        functionIds.remove(functionName);
        agent.setFunctionIds(toJsonArray(functionIds));
        agentRepository.save(agent);
    }

    @Override
    @GetMapping("/{agentId}/functions")
    public List<Map<String, Object>> getAgentFunctions(@PathVariable Long agentId) {
        Agent agent = agentRepository.findById(String.valueOf(agentId))
                .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + agentId));
        return parseJsonArray(agent.getFunctionIds()).stream()
                .map(functionRepository::findById)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .map(function -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", function.getId());
                    item.put("name", function.getName());
                    item.put("description", function.getDescription());
                    return item;
                })
                .collect(Collectors.toList());
    }

    private Agent mapToDomain(Map<String, Object> config) {
        Agent agent = new Agent();
        if (config.containsKey("id")) {
            agent.setId(String.valueOf(config.get("id")));
        }
        agent.setName((String) config.get("name"));
        agent.setDescription((String) config.get("description"));
        agent.setPrompt((String) config.get("prompt"));
        agent.setProvider((String) config.get("provider"));
        agent.setModel(config.get("model") != null ? (String) config.get("model") : "gpt-4o-mini");
        agent.setTemperature(numberAsDouble(config.get("temperature")));
        agent.setMaxTokens(numberAsInteger(config.get("maxTokens")));
        if (config.containsKey("functionIds")) {
            agent.setFunctionIds(toJsonArray(config.get("functionIds")));
        } else if (config.containsKey("functions")) {
            agent.setFunctionIds(toJsonArray(config.get("functions")));
        }
        if (config.containsKey("maxIterations")) {
            agent.setMaxIterations(numberAsInteger(config.get("maxIterations")));
        }
        return agent;
    }

    private Map<String, Object> mapToResponse(Agent agent) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", agent.getId());
        response.put("name", agent.getName());
        response.put("description", agent.getDescription());
        response.put("prompt", agent.getPrompt());
        response.put("provider", agent.getProvider());
        response.put("model", agent.getModel());
        response.put("temperature", agent.getTemperature());
        response.put("maxTokens", agent.getMaxTokens());
        response.put("maxIterations", agent.getMaxIterations());
        response.put("functionIds", parseJsonArray(agent.getFunctionIds()));
        return response;
    }

    private Integer numberAsInteger(Object value) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) return Integer.parseInt((String) value);
        return null;
    }

    private Double numberAsDouble(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String && !((String) value).isEmpty()) return Double.parseDouble((String) value);
        return null;
    }

    private List<String> parseJsonArray(String value) {
        if (value == null || value.trim().isEmpty()) return Collections.emptyList();
        try {
            return objectMapper.readValue(value, new TypeReference<List<String>>() {});
        } catch (Exception ex) {
            return Collections.emptyList();
        }
    }

    private String toJsonArray(Object value) {
        try {
            if (value == null) return "[]";
            if (value instanceof String) {
                String text = (String) value;
                if (text.trim().startsWith("[")) return text;
                return objectMapper.writeValueAsString(Collections.singletonList(text));
            }
            if (value instanceof Collection) return objectMapper.writeValueAsString(value);
            return objectMapper.writeValueAsString(Collections.singletonList(String.valueOf(value)));
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid functionIds payload", ex);
        }
    }
}
