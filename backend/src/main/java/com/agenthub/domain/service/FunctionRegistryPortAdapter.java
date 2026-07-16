package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.port.FunctionRegistry;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

@Service
public class FunctionRegistryPortAdapter implements FunctionRegistry {

    private final FunctionRegistryService functionRegistryService;
    private final ObjectMapper objectMapper;

    public FunctionRegistryPortAdapter(FunctionRegistryService functionRegistryService, ObjectMapper objectMapper) {
        this.functionRegistryService = functionRegistryService;
        this.objectMapper = objectMapper;
    }

    @Override
    public Object invoke(String name, Map<String, Object> arguments) {
        FunctionDefinition function = resolveFunction(name)
                .orElseThrow(() -> new IllegalArgumentException("Function not found: " + name));

        validateArguments(function, arguments);

        return executeHttp(function, arguments != null ? arguments : Collections.emptyMap());
    }

    private Optional<FunctionDefinition> resolveFunction(String nameOrId) {
        if (nameOrId != null && nameOrId.matches("\\d+")) {
            Optional<FunctionDefinition> byId = functionRegistryService.getFunction(nameOrId);
            if (byId.isPresent()) {
                return byId;
            }
        }
        return functionRegistryService.discoverFunctions(nameOrId).stream()
                .filter(function -> nameOrId.equals(function.getName()))
                .findFirst();
    }

    private Object executeHttp(FunctionDefinition function, Map<String, Object> arguments) {
        if (function.getEndpoint() == null || function.getEndpoint().trim().isEmpty()) {
            throw new IllegalArgumentException("Function endpoint is required: " + function.getId());
        }
        RestTemplate restTemplate = restTemplate(function.getTimeoutMs());
        String method = function.getMethod() != null ? function.getMethod().toUpperCase(Locale.ROOT) : "GET";
        ResponseEntity<Object> response;
        if ("GET".equals(method)) {
            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(function.getEndpoint());
            for (Map.Entry<String, Object> entry : arguments.entrySet()) {
                builder.queryParam(entry.getKey(), entry.getValue());
            }
            response = restTemplate.exchange(builder.toUriString(), HttpMethod.GET, HttpEntity.EMPTY, Object.class);
        } else {
            response = restTemplate.exchange(function.getEndpoint(), HttpMethod.valueOf(method), new HttpEntity<>(arguments), Object.class);
        }
        return response.getBody();
    }

    private RestTemplate restTemplate(Integer timeoutMs) {
        int timeout = timeoutMs != null && timeoutMs > 0 ? timeoutMs : 30000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(FunctionDefinition function, Map<String, Object> arguments) {
        if (function.getParameters() == null || function.getParameters().trim().isEmpty()) {
            return;
        }
        try {
            Map<String, Object> schema = readSchema(function.getParameters());
            Object required = schema.get("required");
            if (required instanceof List) {
                for (Object field : (List<Object>) required) {
                    String name = String.valueOf(field);
                    if (arguments == null || !arguments.containsKey(name)) {
                        throw new IllegalArgumentException("Missing required function argument: " + name);
                    }
                }
            }
            Object parameters = schema.get("parameters");
            if (parameters instanceof List) {
                for (Object parameter : (List<Object>) parameters) {
                    if (!(parameter instanceof Map)) continue;
                    Map<String, Object> item = (Map<String, Object>) parameter;
                    if (Boolean.TRUE.equals(item.get("required"))) {
                        String name = String.valueOf(item.get("name"));
                        if (arguments == null || !arguments.containsKey(name)) {
                            throw new IllegalArgumentException("Missing required function argument: " + name);
                        }
                    }
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid function parameter schema: " + function.getId(), ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readSchema(String value) throws Exception {
        Object parsed = objectMapper.readValue(value, Object.class);
        if (parsed instanceof String) {
            parsed = objectMapper.readValue((String) parsed, Object.class);
        }
        if (!(parsed instanceof Map)) {
            throw new IllegalArgumentException("Function parameter schema must be an object");
        }
        return (Map<String, Object>) parsed;
    }
}
