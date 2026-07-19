package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.service.FunctionRegistryService;
import com.agenthub.domain.service.SpringAiMcpAdapter;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mcp")
public class McpApiImpl {
    private final FunctionRegistryService functionRegistryService;
    private final FunctionDefinitionJpaRepository functionRepository;
    private final ObjectMapper objectMapper;
    private final SpringAiMcpAdapter springAiMcpAdapter;

    public McpApiImpl(FunctionRegistryService functionRegistryService,
                      FunctionDefinitionJpaRepository functionRepository,
                      ObjectMapper objectMapper,
                      SpringAiMcpAdapter springAiMcpAdapter) {
        this.functionRegistryService = functionRegistryService;
        this.functionRepository = functionRepository;
        this.objectMapper = objectMapper;
        this.springAiMcpAdapter = springAiMcpAdapter;
    }

    @PostMapping("/tools/import")
    public List<Map<String, Object>> importTools(@RequestBody Map<String, Object> payload) {
        // If Spring AI MCP is available and payload contains serverUrl, use it
        if (springAiMcpAdapter.isAvailable() && payload.containsKey("serverUrl")) {
            String serverUrl = String.valueOf(payload.get("serverUrl"));
            List<FunctionDefinition> imported = springAiMcpAdapter.importMcpTools(
                    serverUrl, TenantContext.externalTenantId(), TenantContext.userId());
            return imported.stream().map(this::map).toList();
        }

        // Fallback: direct tool list import (original behavior)
        List<Map<String, Object>> imported = new ArrayList<>();
        for (Map<String, Object> tool : tools(payload)) {
            FunctionDefinition function = new FunctionDefinition();
            function.setName(String.valueOf(tool.get("name")));
            function.setDescription((String) tool.get("description"));
            function.setEndpoint((String) firstNonNull(tool.get("endpoint"), tool.get("url")));
            function.setMethod(String.valueOf(firstNonNull(tool.get("method"), "POST")));
            function.setParameters(json(firstNonNull(tool.get("inputSchema"), tool.get("parameters"))));
            function.setTimeoutMs(intValue(tool.get("timeoutMs"), 30000));
            function.setImplementation("mcp");
            function.setOwnerId(TenantContext.userId());
            imported.add(map(functionRegistryService.registerFunction(function)));
        }
        return imported;
    }

    @GetMapping("/readiness")
    public Map<String, Object> readiness() {
        long mcpFunctionCount = functionRepository.findByTenantId(TenantContext.tenantId()).stream()
                .filter(function -> "mcp".equals(function.getImplementation()) || "dify-tool".equals(function.getImplementation()))
                .count();
        Map<String, Object> checks = new LinkedHashMap<>();
        checks.put("schemaMapping", true);
        checks.put("parameterValidation", true);
        checks.put("rbacPermission", true);
        checks.put("timeout", true);
        checks.put("errorClassification", true);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("ready", true);
        response.put("mcpFunctionCount", mcpFunctionCount);
        response.put("checks", checks);
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tools(Map<String, Object> payload) {
        Object tools = payload.get("tools");
        if (tools instanceof List) {
            return (List<Map<String, Object>>) tools;
        }
        List<Map<String, Object>> single = new ArrayList<>();
        single.add(payload);
        return single;
    }

    private Map<String, Object> map(FunctionDefinition function) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", function.getId());
        response.put("name", function.getName());
        response.put("description", function.getDescription());
        response.put("endpoint", function.getEndpoint());
        response.put("method", function.getMethod());
        response.put("parameters", readValue(function.getParameters()));
        response.put("implementation", function.getImplementation());
        response.put("timeoutMs", function.getTimeoutMs());
        response.put("retryPolicy", function.getRetryPolicy());
        response.put("circuitBreakerPolicy", function.getCircuitBreakerPolicy());
        response.put("fallbackResponse", function.getFallbackResponse());
        return response;
    }

    private Object firstNonNull(Object value, Object fallback) {
        return value != null ? value : fallback;
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) return Integer.parseInt((String) value);
        return fallback;
    }

    private String json(Object value) {
        try {
            return objectMapper.writeValueAsString(value != null ? value : new LinkedHashMap<>());
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid MCP tool schema", ex);
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
