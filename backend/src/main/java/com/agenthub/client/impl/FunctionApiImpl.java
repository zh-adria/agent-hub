package com.agenthub.client.impl;

import com.agenthub.client.api.FunctionApi;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.service.FunctionRegistryService;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/functions")
public class FunctionApiImpl implements FunctionApi {

    private final FunctionRegistryService functionRegistryService;
    private final RestTemplate restTemplate = new RestTemplate();

    public FunctionApiImpl(FunctionRegistryService functionRegistryService) {
        this.functionRegistryService = functionRegistryService;
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
        if (function.getEndpoint() == null || function.getEndpoint().trim().isEmpty()) {
            throw new IllegalArgumentException("Function endpoint is required: " + functionId);
        }
        Map<String, Object> input = extractInput(args);
        String method = function.getMethod() != null ? function.getMethod().toUpperCase(Locale.ROOT) : "GET";
        ResponseEntity<Object> response;
        if ("GET".equals(method)) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(function.getEndpoint());
            for (Map.Entry<String, Object> entry : input.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, HttpEntity.EMPTY, Object.class);
        } else {
            response = restTemplate.exchange(function.getEndpoint(), HttpMethod.valueOf(method), new HttpEntity<>(input), Object.class);
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("functionId", function.getId());
        result.put("status", response.getStatusCodeValue());
        result.put("result", response.getBody());
        return result;
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
        function.setParameters(config.get("parameters") != null ? config.get("parameters").toString() : null);
        if (config.get("timeoutMs") instanceof Number) {
            function.setTimeoutMs(((Number) config.get("timeoutMs")).intValue());
        }
        function.setImplementation((String) config.get("implementation"));
        function.setOwnerId(config.containsKey("ownerId") ? (String) config.get("ownerId") : "system");
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
}
