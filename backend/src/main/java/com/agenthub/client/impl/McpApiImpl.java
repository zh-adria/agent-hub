package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.service.FunctionRegistryService;
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
    private final ObjectMapper objectMapper;

    public McpApiImpl(FunctionRegistryService functionRegistryService, ObjectMapper objectMapper) {
        this.functionRegistryService = functionRegistryService;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/tools/import")
    public List<Map<String, Object>> importTools(@RequestBody Map<String, Object> payload) {
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
