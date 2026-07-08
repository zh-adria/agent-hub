package com.agenthub.client.impl;

import com.agenthub.client.api.SessionApi;
import org.springframework.stereotype.Service;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class SessionApiImpl implements SessionApi {
    private final Map<Long, Map<String, Object>> sessions = new ConcurrentHashMap<>();
    private final Map<Long, List<Map<String, Object>>> sessionMessages = new ConcurrentHashMap<>();
    private final AtomicLong idGenerator = new AtomicLong(1);
    
    @Override
    public Object createSession(Map<String, Object> sessionConfig) {
        Long id = idGenerator.getAndIncrement();
        sessionConfig.put("id", id);
        sessionConfig.put("createdAt", new Date().toString());
        sessions.put(id, sessionConfig);
        sessionMessages.put(id, new ArrayList<>());
        return sessionConfig;
    }
    
    @Override
    public Object getSession(Long sessionId) {
        return sessions.get(sessionId);
    }
    
    @Override
    public List<Map<String, Object>> listSessions(Long agentId) {
        return sessions.values().stream()
            .filter(s -> agentId.equals(s.get("agentId")))
            .toList();
    }
    
    @Override
    public Object updateSession(Long sessionId, Map<String, Object> updates) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        session.putAll(updates);
        return session;
    }
    
    @Override
    public void deleteSession(Long sessionId) {
        sessions.remove(sessionId);
        sessionMessages.remove(sessionId);
    }
    
    @Override
    public Object sendMessage(Long sessionId, Map<String, Object> message) {
        List<Map<String, Object>> messages = sessionMessages.get(sessionId);
        if (messages == null) {
            throw new IllegalArgumentException("Session not found: " + sessionId);
        }
        message.put("timestamp", new Date().toString());
        messages.add(message);
        return message;
    }
    
    @Override
    public List<Map<String, Object>> getMessages(Long sessionId) {
        return sessionMessages.getOrDefault(sessionId, List.of());
    }
    
    @Override
    public Object getSessionStatus(Long sessionId) {
        Map<String, Object> session = sessions.get(sessionId);
        if (session == null) return null;
        return Map.of(
            "sessionId", sessionId,
            "status", "active",
            "messageCount", sessionMessages.get(sessionId).size()
        );
    }
}
