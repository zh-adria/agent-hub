package com.agenthub.client.api;

import java.util.List;
import java.util.Map;

public interface FunctionRegistry {
    void register(String name, String schemaJson);
    Object invoke(String name, Map<String, Object> args);
    List<Map<String, Object>> listAll();
    boolean exists(String name);
    void unregister(String name);
}
