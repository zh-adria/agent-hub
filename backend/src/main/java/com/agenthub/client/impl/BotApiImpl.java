package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.domain.service.SessionMessageService;
import com.agenthub.infra.persistence.entity.BotBindingEntity;
import com.agenthub.infra.persistence.entity.BotWebhookEventEntity;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import com.agenthub.infra.persistence.repository.BotWebhookEventJpaRepository;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/bots")
public class BotApiImpl {
    private final BotBindingJpaRepository botBindingRepository;
    private final AgentRepository agentRepository;
    private final SessionRepository sessionRepository;
    private final SessionMessageService sessionMessageService;
    private final BotWebhookEventJpaRepository webhookEventRepository;

    public BotApiImpl(
            BotBindingJpaRepository botBindingRepository,
            AgentRepository agentRepository,
            SessionRepository sessionRepository,
            SessionMessageService sessionMessageService,
            BotWebhookEventJpaRepository webhookEventRepository) {
        this.botBindingRepository = botBindingRepository;
        this.agentRepository = agentRepository;
        this.sessionRepository = sessionRepository;
        this.sessionMessageService = sessionMessageService;
        this.webhookEventRepository = webhookEventRepository;
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

    @PostMapping("/bindings/{bindingId}/rotate-secret")
    public Map<String, Object> rotateSecret(@PathVariable Long bindingId, @RequestBody Map<String, Object> payload) {
        BotBindingEntity binding = botBindingRepository.findByIdAndTenantId(bindingId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Bot binding not found: " + bindingId));
        binding.setSecret((String) payload.get("newSecret"));
        binding.setUpdatedBy(TenantContext.userId());
        return map(botBindingRepository.save(binding));
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
        String messageId = messageId(channel, payload, request);
        String content = extractContent(payload);
        String sessionId = sessionId(binding, conversationId);
        Optional<BotWebhookEventEntity> existing = webhookEventRepository.findByTenantIdAndChannelAndMessageId(TenantContext.tenantId(), channel, messageId);
        if (existing.isPresent()) {
            Map<String, Object> duplicate = new LinkedHashMap<>();
            duplicate.put("channel", channel);
            duplicate.put("bindingId", binding.getId());
            duplicate.put("sessionId", existing.get().getSessionId());
            duplicate.put("duplicate", true);
            return duplicate;
        }
        ensureSession(sessionId, binding.getAgentId());
        saveWebhookEvent(channel, messageId, binding.getId(), sessionId);
        Message response = sessionMessageService.send(sessionId, sessionMessageService.newUserMessage(sessionId, content));

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("channel", channel);
        result.put("bindingId", binding.getId());
        result.put("sessionId", sessionId);
        result.put("messageId", messageId);
        result.put("duplicate", false);
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
        String signature = request.getHeader("X-Bot-Signature");
        if (signature != null && !signature.trim().isEmpty()) {
            String timestamp = stringValue(request.getHeader("X-Bot-Timestamp"), "");
            String messageId = messageId(binding.getChannel(), payload, request);
            String expected = hmac(binding.getSecret(), timestamp + "." + messageId);
            if (!constantTimeEquals(expected, signature)) {
                throw new IllegalArgumentException("Invalid bot webhook signature");
            }
            return;
        }
        if (!constantTimeEquals(binding.getSecret(), supplied)) {
            throw new IllegalArgumentException("Invalid bot webhook secret");
        }
    }

    private void saveWebhookEvent(String channel, String messageId, Long bindingId, String sessionId) {
        BotWebhookEventEntity event = new BotWebhookEventEntity();
        event.setTenantId(TenantContext.tenantId());
        event.setChannel(channel);
        event.setMessageId(messageId);
        event.setBindingId(bindingId);
        event.setSessionId(sessionId);
        webhookEventRepository.save(event);
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

    private String messageId(String channel, Map<String, Object> payload, HttpServletRequest request) {
        Object value = firstNonNull(firstNonNull(payload.get("messageId"), payload.get("eventId")), request.getHeader("X-Message-Id"));
        if (value != null) {
            return String.valueOf(value);
        }
        return channel + "-" + Integer.toHexString(String.valueOf(payload).hashCode());
    }

    private String hmac(String secret, String payload) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] digest = mac.doFinal(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();
            for (byte b : digest) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to compute webhook signature", ex);
        }
    }

    private boolean constantTimeEquals(String expected, String supplied) {
        if (expected == null || supplied == null) {
            return false;
        }
        return MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), supplied.getBytes(StandardCharsets.UTF_8));
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
