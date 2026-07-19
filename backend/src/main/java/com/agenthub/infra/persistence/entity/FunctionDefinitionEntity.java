package com.agenthub.infra.persistence.entity;

import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "function_definition")
public class FunctionDefinitionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "name", nullable = false, length = 128)
    private String name;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "endpoint_url", length = 512)
    private String endpointUrl;

    @Column(name = "protocol", length = 16)
    private String protocol;

    @Column(name = "method", length = 16)
    private String method;

    @Column(name = "implementation", length = 32)
    private String implementation;

    @Column(name = "parameters", columnDefinition = "JSON")
    private String parameters;

    @Column(name = "headers", columnDefinition = "JSON")
    private String headers;

    @Column(name = "auth_config", columnDefinition = "JSON")
    private String authConfig;

    @Column(name = "timeout_ms", columnDefinition = "INT DEFAULT 30000")
    private Integer timeoutMs;

    @Column(name = "retry_policy", columnDefinition = "JSON")
    private String retryPolicy;

    @Column(name = "circuit_breaker_policy", columnDefinition = "JSON")
    private String circuitBreakerPolicy;

    @Column(name = "fallback_response", columnDefinition = "TEXT")
    private String fallbackResponse;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getEndpointUrl() { return endpointUrl; }
    public void setEndpointUrl(String endpointUrl) { this.endpointUrl = endpointUrl; }
    public String getProtocol() { return protocol; }
    public void setProtocol(String protocol) { this.protocol = protocol; }
    public String getMethod() { return method; }
    public void setMethod(String method) { this.method = method; }
    public String getImplementation() { return implementation; }
    public void setImplementation(String implementation) { this.implementation = implementation; }
    public String getParameters() { return parameters; }
    public void setParameters(String parameters) { this.parameters = parameters; }
    public String getHeaders() { return headers; }
    public void setHeaders(String headers) { this.headers = headers; }
    public String getAuthConfig() { return authConfig; }
    public void setAuthConfig(String authConfig) { this.authConfig = authConfig; }
    public Integer getTimeoutMs() { return timeoutMs; }
    public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
    public String getRetryPolicy() { return retryPolicy; }
    public void setRetryPolicy(String retryPolicy) { this.retryPolicy = retryPolicy; }
    public String getCircuitBreakerPolicy() { return circuitBreakerPolicy; }
    public void setCircuitBreakerPolicy(String circuitBreakerPolicy) { this.circuitBreakerPolicy = circuitBreakerPolicy; }
    public String getFallbackResponse() { return fallbackResponse; }
    public void setFallbackResponse(String fallbackResponse) { this.fallbackResponse = fallbackResponse; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
