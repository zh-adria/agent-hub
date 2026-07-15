package com.agenthub.domain.repository;

import com.agenthub.domain.model.Session;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.SessionEntity;
import com.agenthub.infra.persistence.repository.SessionJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@Repository
public class JpaSessionRepository implements SessionRepository {

    private final SessionJpaRepository jpaRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JpaSessionRepository(SessionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.findByIdAndTenantId(id, TenantContext.tenantId()).isPresent();
    }

    @Override
    public Session save(Session session) {
        SessionEntity entity = toEntity(session);
        SessionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Session> findById(String id) {
        return jpaRepository.findByIdAndTenantId(id, TenantContext.tenantId()).map(this::toDomain);
    }

    @Override
    public List<Session> findAll() {
        List<Session> result = new ArrayList<>();
        for (SessionEntity entity : jpaRepository.findByTenantId(TenantContext.tenantId())) {
            result.add(toDomain(entity));
        }
        return result;
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.findByIdAndTenantId(id, TenantContext.tenantId())
                .ifPresent(jpaRepository::delete);
    }

    private SessionEntity toEntity(Session domain) {
        SessionEntity entity = domain.getId() != null
                ? jpaRepository.findByIdAndTenantId(domain.getId(), TenantContext.tenantId()).orElse(new SessionEntity())
                : new SessionEntity();
        entity.setId(domain.getId() != null ? domain.getId() : UUID.randomUUID().toString());
        entity.setTenantId(TenantContext.tenantId());
        entity.setAgentId(domain.getAgentId() != null ? Long.parseLong(domain.getAgentId()) : 0L);
        entity.setUserId(toNumericId(domain.getUserId()));
        entity.setContext(messagesToContext(domain.getMessages()));
        entity.setState(domain.getStatus() != null ? domain.getStatus().name() : "ACTIVE");
        entity.setTurnCount(domain.getMessages() != null ? domain.getMessages().size() : 0);
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(TenantContext.userId());
        }
        entity.setUpdatedBy(TenantContext.userId());
        return entity;
    }

    private Long toNumericId(String value) {
        if (value == null || value.trim().isEmpty()) {
            return 0L;
        }
        String trimmed = value.trim();
        if (trimmed.matches("\\d+")) {
            return Long.parseLong(trimmed);
        }
        int dash = trimmed.lastIndexOf('-');
        if (dash >= 0 && dash + 1 < trimmed.length()) {
            String suffix = trimmed.substring(dash + 1).replaceFirst("^0+", "");
            if (!suffix.isEmpty() && suffix.matches("\\d+")) {
                return Long.parseLong(suffix);
            }
        }
        return Math.abs((long) trimmed.hashCode());
    }

    private Session toDomain(SessionEntity entity) {
        Session session = new Session();
        session.setId(entity.getId());
        session.setAgentId(String.valueOf(entity.getAgentId()));
        session.setUserId(String.valueOf(entity.getUserId()));
        session.setMessages(contextToMessages(entity.getContext(), entity.getId()));
        session.setStatus(parseStatus(entity.getState()));
        session.setCreatedAt(entity.getCreatedAt());
        session.setUpdatedAt(entity.getUpdatedAt());
        return session;
    }

    private String messagesToContext(List<Message> messages) {
        Map<String, Object> context = new LinkedHashMap<>();
        List<Map<String, Object>> payload = new ArrayList<>();
        if (messages != null) {
            for (Message message : messages) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("id", message.getId());
                item.put("sessionId", message.getSessionId());
                item.put("role", message.getRole() != null ? message.getRole().name().toLowerCase() : "user");
                item.put("content", message.getContent());
                item.put("timestamp", message.getCreatedAt() != null ? message.getCreatedAt().toString() : LocalDateTime.now().toString());
                payload.add(item);
            }
        }
        context.put("messages", payload);
        try {
            return objectMapper.writeValueAsString(context);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize session context", ex);
        }
    }

    private List<Message> contextToMessages(String context, String sessionId) {
        if (context == null || context.trim().isEmpty()) {
            return new ArrayList<>();
        }
        try {
            Object parsed = objectMapper.readValue(context, Object.class);
            if (parsed instanceof String) {
                return contextToMessages((String) parsed, sessionId);
            }
            if (!(parsed instanceof Map)) {
                return new ArrayList<>();
            }
            Map<?, ?> payload = (Map<?, ?>) parsed;
            Object rawMessages = payload.get("messages");
            if (!(rawMessages instanceof List)) {
                return new ArrayList<>();
            }
            List<Message> messages = new ArrayList<>();
            for (Object raw : (List<?>) rawMessages) {
                if (!(raw instanceof Map)) continue;
                Map<?, ?> item = (Map<?, ?>) raw;
                Message message = new Message();
                message.setId(asString(item.get("id")));
                message.setSessionId(asString(item.get("sessionId")) != null ? asString(item.get("sessionId")) : sessionId);
                message.setRole(parseRole(asString(item.get("role"))));
                message.setContent(asString(item.get("content")));
                message.setCreatedAt(parseDate(asString(item.get("timestamp"))));
                messages.add(message);
            }
            return messages;
        } catch (Exception ex) {
            return new ArrayList<>();
        }
    }

    private String asString(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private LocalDateTime parseDate(String value) {
        if (value == null || value.isEmpty()) return LocalDateTime.now();
        return LocalDateTime.parse(value);
    }

    private Message.MessageRole parseRole(String value) {
        if (value == null) return Message.MessageRole.USER;
        return Message.MessageRole.valueOf(value.toUpperCase());
    }

    private Session.SessionStatus parseStatus(String value) {
        if (value == null) return Session.SessionStatus.ACTIVE;
        return Session.SessionStatus.valueOf(value.toUpperCase());
    }
}
