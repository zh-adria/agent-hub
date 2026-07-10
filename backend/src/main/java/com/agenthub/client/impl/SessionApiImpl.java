package com.agenthub.client.impl;

import com.agenthub.client.api.SessionApi;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.domain.service.SessionMessageService;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
public class SessionApiImpl implements SessionApi {

    private final SessionRepository sessionRepository;
    private final SessionMessageService sessionMessageService;

    public SessionApiImpl(SessionRepository sessionRepository, SessionMessageService sessionMessageService) {
        this.sessionRepository = sessionRepository;
        this.sessionMessageService = sessionMessageService;
    }

    @Override
    @PostMapping
    public Object createSession(@RequestBody Map<String, Object> sessionConfig) {
        Session session = mapToDomain(sessionConfig);
        Session saved = sessionRepository.save(session);
        return mapToResponse(saved);
    }

    @Override
    @GetMapping("/{sessionId}")
    public Object getSession(@PathVariable Long sessionId) {
        return sessionRepository.findById(String.valueOf(sessionId))
                .map(this::mapToResponse)
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));
    }

    @Override
    @GetMapping
    public List<Map<String, Object>> listSessions(@RequestParam(required = false) Long agentId) {
        List<Session> all = sessionRepository.findAll();
        if (agentId != null) {
            return all.stream()
                    .filter(s -> agentId.toString().equals(s.getAgentId()))
                    .map(this::mapToResponse)
                    .collect(Collectors.toList());
        }
        return all.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Override
    @PutMapping("/{sessionId}")
    public Object updateSession(@PathVariable Long sessionId, @RequestBody Map<String, Object> updates) {
        Session existing = sessionRepository.findById(String.valueOf(sessionId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));

        if (updates.containsKey("state")) {
            existing.setStatus(Session.SessionStatus.valueOf((String) updates.get("state")));
        }

        Session updated = sessionRepository.save(existing);
        return mapToResponse(updated);
    }

    @Override
    @DeleteMapping("/{sessionId}")
    public void deleteSession(@PathVariable Long sessionId) {
        sessionRepository.deleteById(String.valueOf(sessionId));
    }

    @Override
    @PostMapping("/{sessionId}/messages")
    public Object sendMessage(@PathVariable Long sessionId, @RequestBody Map<String, Object> message) {
        Session session = sessionRepository.findById(String.valueOf(sessionId))
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        Message userMessage = mapToMessage(String.valueOf(sessionId), message);
        Message response = sessionMessageService.send(session.getId(), userMessage);
        return mapMessageToResponse(response);
    }

    @Override
    @GetMapping("/{sessionId}/messages")
    public List<Map<String, Object>> getMessages(@PathVariable Long sessionId) {
        return sessionRepository.findById(String.valueOf(sessionId))
                .map(Session::getMessages)
                .orElse(Collections.emptyList())
                .stream()
                .map(this::mapMessageToResponse)
                .collect(Collectors.toList());
    }

    @Override
    @GetMapping("/{sessionId}/status")
    public Object getSessionStatus(@PathVariable Long sessionId) {
        Session session = sessionRepository.findById(String.valueOf(sessionId))
                .orElseThrow(() -> new ResourceNotFoundException("Session not found: " + sessionId));

        Map<String, Object> status = new LinkedHashMap<>();
        status.put("sessionId", session.getId());
        status.put("status", session.getStatus() != null ? session.getStatus().name() : "UNKNOWN");
        return status;
    }

    private Session mapToDomain(Map<String, Object> config) {
        Session session = new Session();
        if (config.containsKey("id")) {
            session.setId(String.valueOf(config.get("id")));
        } else {
            session.setId(String.valueOf(System.currentTimeMillis()));
        }
        session.setAgentId((String) config.get("agentId"));
        session.setUserId(TenantContext.userId());
        session.setMessages(new ArrayList<>());
        session.setStatus(Session.SessionStatus.ACTIVE);
        return session;
    }

    private Map<String, Object> mapToResponse(Session session) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", session.getId());
        response.put("agentId", session.getAgentId());
        response.put("userId", session.getUserId());
        response.put("status", session.getStatus() != null ? session.getStatus().name() : "ACTIVE");
        response.put("createdAt", session.getCreatedAt());
        response.put("updatedAt", session.getUpdatedAt());
        return response;
    }

    private Message mapToMessage(String sessionId, Map<String, Object> payload) {
        Message message = new Message();
        message.setId(payload.get("id") != null ? String.valueOf(payload.get("id")) : UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(parseRole(payload.get("role")));
        message.setContent(payload.get("content") != null ? String.valueOf(payload.get("content")) : "");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private Map<String, Object> mapMessageToResponse(Message message) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", message.getId());
        response.put("sessionId", message.getSessionId());
        response.put("role", message.getRole() != null ? message.getRole().name().toLowerCase() : "user");
        response.put("content", message.getContent());
        response.put("timestamp", message.getCreatedAt());
        return response;
    }

    private Message.MessageRole parseRole(Object raw) {
        if (raw == null) return Message.MessageRole.USER;
        return Message.MessageRole.valueOf(String.valueOf(raw).toUpperCase());
    }
}
