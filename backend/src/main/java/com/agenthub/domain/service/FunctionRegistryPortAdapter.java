package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.port.FunctionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

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
        FunctionDefinition function = functionRegistryService.getFunction(name)
                .orElseThrow(() -> new IllegalArgumentException("Function not found: " + name));

        validateArguments(function, arguments);

        // Execute the function
        // For now, return a mock result
        return java.util.Collections.singletonMap("result", "Mock result for " + name);
    }

    @SuppressWarnings("unchecked")
    private void validateArguments(FunctionDefinition function, Map<String, Object> arguments) {
        if (function.getParameters() == null || function.getParameters().trim().isEmpty()) {
            return;
        }
        try {
            Map<String, Object> schema = objectMapper.readValue(function.getParameters(), new TypeReference<Map<String, Object>>() {});
            Object required = schema.get("required");
            if (!(required instanceof List)) {
                return;
            }
            for (Object field : (List<Object>) required) {
                String name = String.valueOf(field);
                if (arguments == null || !arguments.containsKey(name)) {
                    throw new IllegalArgumentException("Missing required function argument: " + name);
                }
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid function parameter schema: " + function.getId(), ex);
        }
    }
}
