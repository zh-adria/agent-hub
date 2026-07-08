package com.agenthub.client.api;

import java.util.List;
import java.util.Map;

public interface SessionApi {
    // Session CRUD
    Object createSession(Map<String, Object> sessionConfig);
    Object getSession(Long sessionId);
    List<Map<String, Object>> listSessions(Long agentId);
    Object updateSession(Long sessionId, Map<String, Object> updates);
    void deleteSession(Long sessionId);
    
    // Session operations
    Object sendMessage(Long sessionId, Map<String, Object> message);
    List<Map<String, Object>> getMessages(Long sessionId);
    Object getSessionStatus(Long sessionId);
}
