package com.agenthub.domain.model;

import java.time.LocalDateTime;

public class Message {
    private String id;
    private String sessionId;
    private MessageRole role;
    private String content;
    private String functionCallId;
    private String functionCallName;
    private String functionCallArguments;
    private LocalDateTime createdAt;
    
    public enum MessageRole {
        USER, ASSISTANT, SYSTEM, FUNCTION
    }
    
    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }
    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }
    public MessageRole getRole() { return role; }
    public void setRole(MessageRole role) { this.role = role; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public String getFunctionCallId() { return functionCallId; }
    public void setFunctionCallId(String functionCallId) { this.functionCallId = functionCallId; }
    public String getFunctionCallName() { return functionCallName; }
    public void setFunctionCallName(String functionCallName) { this.functionCallName = functionCallName; }
    public String getFunctionCallArguments() { return functionCallArguments; }
    public void setFunctionCallArguments(String functionCallArguments) { this.functionCallArguments = functionCallArguments; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
