package com.agenthub.client.api;

import java.util.List;
import java.util.Map;

public interface FunctionApi {
    // Function CRUD
    Object createFunction(Map<String, Object> functionConfig);
    Object getFunction(Long functionId);
    List<Map<String, Object>> listFunctions();
    Object updateFunction(Long functionId, Map<String, Object> updates);
    void deleteFunction(Long functionId);
    
    // Function invocation
    Object invokeFunction(Long functionId, Map<String, Object> args);
    Object testFunction(Long functionId, Map<String, Object> args);
}
