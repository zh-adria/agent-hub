package com.agenthub.client.impl;

import com.agenthub.client.api.FunctionApi;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class FunctionApiImpl implements FunctionApi {
    private final Map<Long, Map<String, Object>> functions = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Object createFunction(Map<String, Object> functionConfig) {
        Long id = idGenerator.getAndIncrement();
        functionConfig.put("id", id);
        functions.put(id, functionConfig);
        return functionConfig;
    }
    
    @Override
    public Object getFunction(Long functionId) {
        return functions.get(functionId);
    }
    
    @Override
    public List<Map<String, Object>> listFunctions() {
        return new ArrayList<>(functions.values());
    }
    
    @Override
    public Object updateFunction(Long functionId, Map<String, Object> updates) {
        Map<String, Object> function = functions.get(functionId);
        if (function == null) {
            throw new IllegalArgumentException("Function not found: " + functionId);
        }
        function.putAll(updates);
        return function;
    }
    
    @Override
    public void deleteFunction(Long functionId) {
        functions.remove(functionId);
    }
    
    @Override
    public Object invokeFunction(Long functionId, Map<String, Object> args) {
        Map<String, Object> function = functions.get(functionId);
        if (function == null) {
            throw new IllegalArgumentException("Function not found: " + functionId);
        }
        return Map.of("result", "Mock invocation result", "functionId", functionId);
    }
    
    @Override
    public Object testFunction(Long functionId, Map<String, Object> args) {
        return invokeFunction(functionId, args);
    }
}
