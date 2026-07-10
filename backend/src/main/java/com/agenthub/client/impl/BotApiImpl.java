package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.domain.service.SessionMessageService;
import com.agenthub.infra.persistence.entity.BotBindingEntity;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bots")
public class BotApiImpl {
    private final BotBindingJpaRepository botBindingRepository;
    private final AgentRepository agentRepository;
    private final SessionRepository sessionRepository;
    private final SessionMessageService sessionMessageService;

    public BotApiImpl(
            BotBindingJpaRepository botBindingRepository,
            AgentRepository agentRepository,
            SessionRepository sessionRepository,
            SessionMessageService sessionMessageService) {
        this.botBindingRepository = botBindingRepository;
        this.agentRepository = agentRepository;
        this.sessionRepository = sessionRepository;
        this.sessionMessageService = sessionMessageService;
    }

    @PostMapping("/bindings")
    public Map<String, Object> createBinding(@RequestBody Map<String, Object> payload) {
        String agentId = String.valueOf(payload.get("agentId"));
        agentRepository.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
        BotBindingEntity entity = new BotBindingEntity();
        entity.setTenantId(TenantContext.tenantId());
        entity.setChannel(stringValue(payload.get("channel"), "generic"));
        entity.setChannelBotId(stringValue(payload.get("channelBotId"), "default"));
        entity.setAgentId(agentId);
        entity.setSecret((String) payload.get("secret"));
        entity.setStatus(1);
        entity.setCreatedBy(TenantContext.userId());
        entity.setUpdatedBy(TenantContext.userId());
        return map(botBindingRepository.save(entity));
    }

    @GetMapping("/bindings")
    public List<Map<String, Object>> listBindings() {
        return botBindingRepository.findByTenantId(TenantContext.tenantId()).stream()
                .map(this::map)
                .collect(Collectors.toList());
    }

    @DeleteMapping("/bindings/{bindingId}")
    public void deleteBinding(@PathVariable Long bindingId) {
        botBindingRepository.findByIdAndTenantId(bindingId, TenantContext.tenantId())
                .ifPresent(botBindingRepository::delete);
    }

    @PostMapping("/webhooks/{channel}")
    public Map<String, Object> webhook(
            @PathVariable String channel,
            @RequestBody Map<String, Object> payload,
            HttpServletRequest request) {
        String channelBotId = stringValue(firstNonNull(payload.get("botId"), payload.get("channelBotId")), "default");
        BotBindingEntity binding = botBindingRepository
                .findByTenantIdAndChannelAndChannelBotIdAndStatus(TenantContext.tenantId(), channel, channelBotId, 1)
                .orElseThrow(() -> new ResourceNotFoundException("Bot binding not found: " + channel + "/" + channelBotId));
        validateSecret(binding, payload, request);
        String conversationId = stringValue(firstNonNull(payload.get("conversationId"), payload.get("chatId")), "default");
        String content = extractContent(payload);
        String sessionId = sessionId(binding, conversationId);
        ensureSession(sessionId, binding.getAgentId());
        Message response = sessionMessageService.send(sessionId, sessionMessageService.newUserMessage(sessionId, content));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", channel);
        result.put("bindingId", binding.getId());
        result.put("sessionId", sessionId);
        result.put("reply", response.getContent());
        return result;
    }

    private void ensureSession(String sessionId, String agentId) {
        if (sessionRepository.findById(sessionId).isPresent()) {
            return;
        }
        Session session = new Session();
        session.setId(sessionId);
        session.setAgentId(agentId);
        session.setUserId(TenantContext.userId());
        session.setMessages(new ArrayList<>());
        session.setStatus(Session.SessionStatus.ACTIVE);
        sessionRepository.save(session);
    }

    private void validateSecret(BotBindingEntity binding, Map<String, Object> payload, HttpServletRequest request) {
        if (binding.getSecret() == null || binding.getSecret().trim().isEmpty()) {
            return;
        }
        String supplied = stringValue(firstNonNull(payload.get("secret"), request.getHeader("X-Bot-Secret")), "");
        if (!binding.getSecret().equals(supplied)) {
            throw new IllegalArgumentException("Invalid bot webhook secret");
        }
    }

    @SuppressWarnings("unchecked")
    private String extractContent(Map<String, Object> payload) {
        Object content = firstNonNull(payload.get("content"), payload.get("text"));
        if (content instanceof Map) {
            Object text = ((Map<String, Object>) content).get("text");
            return text != null ? String.valueOf(text) : "";
        }
        return content != null ? String.valueOf(content) : "";
    }

    private String sessionId(BotBindingEntity binding, String conversationId) {
        return ("bot-" + binding.getId() + "-" + conversationId).replaceAll("[^a-zA-Z0-9_-]", "_");
    }

    private Map<String, Object> map(BotBindingEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("channel", entity.getChannel());
        response.put("channelBotId", entity.getChannelBotId());
        response.put("agentId", entity.getAgentId());
        response.put("status", entity.getStatus());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());
        return response;
    }

    private Object firstNonNull(Object value, Object fallback) {
        return value != null ? value : fallback;
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }
}
