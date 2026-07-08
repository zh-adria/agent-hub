package com.agenthub.client.model;

public class Session {
    private Long id;
    private Long agentId;
    private String name;
    private String status;  // active, paused, closed
    private String createdAt;
    private String updatedAt;
    private Map<String, Object> context;
    
    public Session() {}
    
    public Session(Long agentId, String name) {
        this.agentId = agentId;
        this.name = name;
        this.status = "active";
        this.createdAt = new java.util.Date().toString();
        this.updatedAt = this.createdAt;
    }
    
    // Getters and setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getAgentId() { return agentId; }
    public void setAgentId(Long agentId) { this.agentId = agentId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }
    public String getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(String updatedAt) { this.updatedAt = updatedAt; }
    public Map<String, Object> getContext() { return context; }
    public void setContext(Map<String, Object> context) { this.context = context; }
}
