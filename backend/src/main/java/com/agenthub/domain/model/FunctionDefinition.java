package com.agenthub.domain.model;

public class FunctionDefinition {
    private String id;
    private String name;
    private String description;
    private String endpoint;
    private String method;
    private String parameters;
    private Integer timeoutMs;
    private String implementation;
    private String ownerId;
    private String retryPolicy;
    private String circuitBreakerPolicy;
    private String fallbackResponse;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEndpoint() { return endpoint; }
    public void setEndpoint(String endpoint) { this.endpoint = endpoint; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getImplementation() { return implementation; }
    public void setImplementation(String implementation) { this.implementation = implementation; }
    public String getOwnerId() { return ownerId; }
    public void setOwnerId(String ownerId) { this.ownerId = ownerId; }
    public String getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(String retryPolicy) { this.retryPolicy = retryPolicy; }
    public String getCircuitBreakerPolicy() { return circuitBreakerPolicy; }
    public void setCircuitBreakerPolicy(String circuitBreakerPolicy) { this.circuitBreakerPolicy = circuitBreakerPolicy; }
    public String getFallbackResponse() { return fallbackResponse; }
    public void setFallbackResponse(String fallbackResponse) { this.fallbackResponse = fallbackResponse; }
}
