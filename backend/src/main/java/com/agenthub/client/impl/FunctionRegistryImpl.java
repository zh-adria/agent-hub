package com.agenthub.client.impl;

import com.agenthub.client.api.FunctionRegistry;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class FunctionRegistryImpl implements FunctionRegistry {
    private final Map<String, String> registry = new ConcurrentHashMap<>();
    
    @Override
    public void register(String name, String schemaJson) {
        registry.put(name, schemaJson);
    }
    
    @Override
    public Object invoke(String name, Map<String, Object> args) {
        if (!registry.containsKey(name)) {
            throw new IllegalArgumentException("Function not found: " + name);
        }
        return Map.of("result", "Mock result for " + name);
    }
    
    @Override
    public List<Map<String, Object>> listAll() {
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map.Entry<String, String> entry : registry.entrySet()) {
            list.add(Map.of("name", entry.getKey(), "schema", entry.getValue()));
        }
        return list;
    }
    
    @Override
    public boolean exists(String name) {
        return registry.containsKey(name);
    }
    
    @Override
    public void unregister(String name) {
        registry.remove(name);
    }
}
