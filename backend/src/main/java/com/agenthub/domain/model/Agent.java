package com.agenthub.domain.model;

public class Agent {
    private String id;
    private String name;
    private String description;
    private String prompt;
    private String provider;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Integer maxIterations;
    private String functionIds;
    private Integer toolTimeoutMs;
    private String errorHandlingMode;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getPrompt() { return prompt; }
    public void setPrompt(String prompt) { this.prompt = prompt; }
    public String getProvider() { return provider; }
    public void setProvider(String provider) { this.provider = provider; }
    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }
    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }
    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }
    public Integer getMaxIterations() { return maxIterations; }
    public void setMaxIterations(Integer maxIterations) { this.maxIterations = maxIterations; }
    public String getFunctionIds() { return functionIds; }
    public void setFunctionIds(String functionIds) { this.functionIds = functionIds; }
    public Integer getToolTimeoutMs() { return toolTimeoutMs; }
    public void setToolTimeoutMs(Integer toolTimeoutMs) { this.toolTimeoutMs = toolTimeoutMs; }
    public String getErrorHandlingMode() { return errorHandlingMode; }
    public void setErrorHandlingMode(String errorHandlingMode) { this.errorHandlingMode = errorHandlingMode; }
}
