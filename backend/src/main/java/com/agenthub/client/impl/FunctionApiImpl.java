package com.agenthub.client.impl;

import com.agenthub.client.api.FunctionApi;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.service.FunctionRegistryService;
import com.agenthub.domain.service.TraceService;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.*;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/functions")
public class FunctionApiImpl implements FunctionApi {

    private final FunctionRegistryService functionRegistryService;
    private final FunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper;
    private final TraceService traceService;

    public FunctionApiImpl(
            FunctionRegistryService functionRegistryService,
            FunctionRegistry functionRegistry,
            ObjectMapper objectMapper,
            TraceService traceService) {
        this.functionRegistryService = functionRegistryService;
        this.functionRegistry = functionRegistry;
        this.objectMapper = objectMapper;
        this.traceService = traceService;
    }

    @Override
    @PostMapping
    public Object createFunction(@RequestBody Map<String, Object> functionConfig) {
        FunctionDefinition function = mapToDomain(functionConfig);
        FunctionDefinition saved = functionRegistryService.registerFunction(function);
        return mapToResponse(saved);
    }

    @Override
    @GetMapping("/{functionId}")
    public Object getFunction(@PathVariable Long functionId) {
        return functionRegistryService.getFunction(String.valueOf(functionId))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Function not found: " + functionId));
    }

    @Override
    @GetMapping
    public List<Map<String, Object>> listFunctions() {
        return functionRegistryService.getAllFunctions().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @PutMapping("/{functionId}")
    public Object updateFunction(@PathVariable Long functionId, @RequestBody Map<String, Object> updates) {
        FunctionDefinition updated = functionRegistryService.updateFunction(
                String.valueOf(functionId), mapToDomain(updates));
        return mapToResponse(updated);
    }

    @Override
    @DeleteMapping("/{functionId}")
    public void deleteFunction(@PathVariable Long functionId) {
        functionRegistryService.deleteFunction(String.valueOf(functionId));
    }

    @Override
    @PostMapping("/{functionId}/invoke")
    public Object invokeFunction(@PathVariable Long functionId, @RequestBody Map<String, Object> args) {
        FunctionDefinition function = functionRegistryService.getFunction(String.valueOf(functionId))
                .orElseThrow(() -> new IllegalArgumentException("Function not found: " + functionId));
        Map<String, Object> input = extractInput(args);
        TraceEntity trace = traceService.start("function.invoke", null, null, functionTraceMetadata(function));
        StepRecordEntity step = traceService.startStep(trace.getId(), null, null, "function:" + function.getId(), null, safeJson(input));
        try {
            Object response = functionRegistry.invoke(function.getId(), input);
            Map<String, Object> result = new LinkedHashMap<>();
            result.put("functionId", function.getId());
            result.put("traceId", trace.getId());
            result.put("stepRecordId", step.getId());
            result.put("status", 200);
            result.put("result", response);
            traceService.completeStep(step, safeJson(result));
            traceService.finish(trace.getId(), "SUCCEEDED");
            return result;
        } catch (RuntimeException ex) {
            traceService.failStep(step, ex.getClass().getSimpleName() + ": " + ex.getMessage());
            traceService.finish(trace.getId(), "FAILED");
            throw ex;
        }
    }

    @Override
    @PostMapping("/{functionId}/test")
    public Object testFunction(@PathVariable Long functionId, @RequestBody Map<String, Object> args) {
        return invokeFunction(functionId, args);
    }

    private FunctionDefinition mapToDomain(Map<String, Object> config) {
        FunctionDefinition function = new FunctionDefinition();
        if (config.containsKey("id")) {
            function.setId(String.valueOf(config.get("id")));
        }
        function.setName((String) config.get("name"));
        function.setDescription((String) config.get("description"));
        function.setEndpoint((String) config.get("endpoint"));
        function.setMethod(config.get("method") != null ? (String) config.get("method") : "GET");
        function.setParameters(toJson(config.get("parameters")));
        if (config.get("timeoutMs") instanceof Number) {
            function.setTimeoutMs(((Number) config.get("timeoutMs")).intValue());
        }
        function.setImplementation((String) config.get("implementation"));
        function.setOwnerId(config.containsKey("ownerId") ? (String) config.get("ownerId") : "system");
        function.setRetryPolicy((String) config.get("retryPolicy"));
        function.setCircuitBreakerPolicy((String) config.get("circuitBreakerPolicy"));
        function.setFallbackResponse((String) config.get("fallbackResponse"));
        return function;
    }

    private Map<String, Object> mapToResponse(FunctionDefinition function) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", function.getId());
        response.put("name", function.getName());
        response.put("description", function.getDescription());
        response.put("endpoint", function.getEndpoint());
        response.put("method", function.getMethod());
        response.put("parameters", function.getParameters());
        response.put("timeoutMs", function.getTimeoutMs());
        response.put("implementation", function.getImplementation());
        response.put("ownerId", function.getOwnerId());
        response.put("retryPolicy", function.getRetryPolicy());
        response.put("circuitBreakerPolicy", function.getCircuitBreakerPolicy());
        response.put("fallbackResponse", function.getFallbackResponse());
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> extractInput(Map<String, Object> args) {
        Object input = args != null ? args.get("input") : null;
        if (input instanceof Map) {
            return (Map<String, Object>) input;
        }
        return args != null ? args : Collections.emptyMap();
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof String) {
            String text = ((String) value).trim();
            if (text.startsWith("{") || text.startsWith("[")) {
                return text;
            }
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid function parameters schema", ex);
        }
    }

    private String functionTraceMetadata(FunctionDefinition function) {
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("functionId", function.getId());
        metadata.put("functionName", function.getName());
        metadata.put("endpoint", function.getEndpoint());
        metadata.put("method", function.getMethod());
        return safeJson(metadata);
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return String.valueOf(value);
        }
    }
}
