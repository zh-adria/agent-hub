package com.agenthub.domain.service;

import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.repository.FunctionRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Wires the Resilience4j-backed FunctionRegistry as the primary bean.
 *
 * <p>FunctionRegistryPortAdapter remains the HTTP execution delegate;
 * ResilientFunctionRegistry wraps it with retry / circuit breaker / fallback.</p>
 */
@Configuration
public class ResilienceConfig {

    @Bean
    @Primary
    public FunctionRegistry primaryFunctionRegistry(
            FunctionRegistryPortAdapter delegate,
            FunctionRepository functionRepository) {
        return new ResilientFunctionRegistry(delegate, functionRepository);
    }
}
