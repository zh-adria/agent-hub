package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.repository.FunctionRepository;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.retry.Retry;
import io.github.resilience4j.retry.RetryConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Resilience4j wrapper for FunctionRegistry.invoke().
 *
 * <p>Applies retry, circuit breaker, and fallback policies defined per FunctionDefinition.
 * Uses a simple loop-based retry with Resilience4j circuit breaker state tracking.</p>
 *
 * <p>Instantiated via {@link ResilienceConfig#primaryFunctionRegistry} as the primary
 * {@link FunctionRegistry} bean, wrapping {@link FunctionRegistryPortAdapter}.</p>
 */
public class ResilientFunctionRegistry implements FunctionRegistry {
    private static final Logger log = LoggerFactory.getLogger(ResilientFunctionRegistry.class);

    private final FunctionRegistry delegate;
    private final FunctionRepository functionRepository;

    public ResilientFunctionRegistry(FunctionRegistry delegate, FunctionRepository functionRepository) {
        this.delegate = delegate;
        this.functionRepository = functionRepository;
    }

    @Override
    public Object invoke(String functionId, Map<String, Object> arguments) {
        FunctionDefinition function = functionRepository.findById(functionId)
                .orElseThrow(() -> new IllegalArgumentException("Function not found: " + functionId));

        Retry retry = Retry.of("fn-" + functionId, resolveRetryConfig(function));
        CircuitBreaker circuitBreaker = CircuitBreaker.of("fn-" + functionId, resolveCircuitBreakerConfig(function));
        int maxAttempts = retry.getRetryConfig().getMaxAttempts();
        RuntimeException lastError = null;

        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            try {
                if (circuitBreaker.getState() == CircuitBreaker.State.OPEN) {
                    throw new IllegalStateException("Circuit breaker OPEN for function " + functionId);
                }
                Object result = delegate.invoke(functionId, arguments);
                circuitBreaker.onResult(0, TimeUnit.MILLISECONDS, result);
                return result;
            } catch (IllegalStateException ex) {
                log.warn("Circuit breaker open for function {}: {}", functionId, ex.getMessage());
                return fallback(function, arguments, ex);
            } catch (RuntimeException ex) {
                lastError = ex;
                circuitBreaker.onError(0, TimeUnit.MILLISECONDS, ex);
                log.warn("Function {} invocation failed (attempt {}/{}): {}", functionId, attempt, maxAttempts, ex.getMessage());
            } catch (Exception ex) {
                lastError = new RuntimeException(ex);
                circuitBreaker.onError(0, TimeUnit.MILLISECONDS, ex);
                log.warn("Function {} invocation failed (attempt {}/{}): {}", functionId, attempt, maxAttempts, ex.getMessage());
            }
        }
        return fallback(function, arguments, lastError);
    }

    private Object fallback(FunctionDefinition function, Map<String, Object> arguments, Throwable cause) {
        String fallback = function.getFallbackResponse();
        if (fallback != null && !fallback.isBlank()) {
            log.info("Using fallback for function {}: {}", function.getId(), fallback);
            return java.util.Map.of(
                    "functionId", function.getId(),
                    "status", 502,
                    "error", cause.getClass().getSimpleName() + ": " + cause.getMessage(),
                    "fallback", fallback
            );
        }
        throw new RuntimeException("Function invocation failed: " + function.getName(), cause);
    }

    private RetryConfig resolveRetryConfig(FunctionDefinition function) {
        RetryConfig.Builder builder = RetryConfig.custom();
        String policy = function.getRetryPolicy();
        int maxAttempts = 3;
        long waitDurationMs = 500;
        if (policy != null && !policy.isBlank()) {
            try {
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(policy, Map.class);
                if (parsed.containsKey("maxAttempts")) {
                    maxAttempts = ((Number) parsed.get("maxAttempts")).intValue();
                }
                if (parsed.containsKey("waitDurationMs")) {
                    waitDurationMs = ((Number) parsed.get("waitDurationMs")).longValue();
                }
            } catch (Exception ex) {
                log.warn("Failed to parse retry policy for function {}, using defaults", function.getId(), ex);
            }
        }
        return builder
                .maxAttempts(maxAttempts)
                .waitDuration(Duration.ofMillis(waitDurationMs))
                .retryExceptions(java.io.IOException.class, RuntimeException.class)
                .build();
    }

    private CircuitBreakerConfig resolveCircuitBreakerConfig(FunctionDefinition function) {
        CircuitBreakerConfig.Builder builder = CircuitBreakerConfig.custom();
        float failureRateThreshold = 50f;
        long waitDurationMs = 30000;
        int slidingWindowSize = 10;
        String policy = function.getCircuitBreakerPolicy();
        if (policy != null && !policy.isBlank()) {
            try {
                Map<String, Object> parsed = new com.fasterxml.jackson.databind.ObjectMapper().readValue(policy, Map.class);
                if (parsed.containsKey("failureRateThreshold")) {
                    failureRateThreshold = ((Number) parsed.get("failureRateThreshold")).floatValue();
                }
                if (parsed.containsKey("waitDurationInOpenStateMs")) {
                    waitDurationMs = ((Number) parsed.get("waitDurationInOpenStateMs")).longValue();
                }
                if (parsed.containsKey("slidingWindowSize")) {
                    slidingWindowSize = ((Number) parsed.get("slidingWindowSize")).intValue();
                }
            } catch (Exception ex) {
                log.warn("Failed to parse circuit breaker policy for function {}, using defaults", function.getId(), ex);
            }
        }
        return builder
                .failureRateThreshold(failureRateThreshold)
                .waitDurationInOpenState(Duration.ofMillis(waitDurationMs))
                .slidingWindowSize(slidingWindowSize)
                .minimumNumberOfCalls(5)
                .build();
    }
}
