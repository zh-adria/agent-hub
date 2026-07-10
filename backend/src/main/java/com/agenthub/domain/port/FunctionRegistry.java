package com.agenthub.domain.port;

import java.util.Map;

public interface FunctionRegistry {
    Object invoke(String name, Map<String, Object> arguments);
}
