package com.agenthub.client.impl;

import com.agenthub.client.audit.LLMUsageAuditRecord;
import com.agenthub.client.audit.LLMUsageAuditService;
import com.agenthub.client.tokenrouter.AgentInvocationMetadata;
import com.agenthub.client.tokenrouter.TokenRouterChatRequest;
import com.agenthub.client.tokenrouter.TokenRouterChatResponse;
import com.agenthub.client.tokenrouter.TokenRouterClient;
import com.agenthub.client.tokenrouter.TokenRouterRequestMapper;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.port.LLMClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class TokenRouterDomainLLMClient implements LLMClient {

    private final TokenRouterClient tokenRouterClient;
    private final TokenRouterRequestMapper requestMapper;
    private final LLMUsageAuditService auditService;
    private final ObjectMapper objectMapper;

    public TokenRouterDomainLLMClient(TokenRouterClient tokenRouterClient, ObjectMapper objectMapper, LLMUsageAuditService auditService) {
        this.tokenRouterClient = tokenRouterClient;
        this.requestMapper = new TokenRouterRequestMapper(objectMapper);
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    @Override
    public String reason(Agent agent, List<Message> context) {
        TokenRouterChatRequest request = requestMapper.fromMessagesJson(toMessagesJson(context), agent.getModel(), metadata(agent, context, "reason"));
        TokenRouterChatResponse response = tokenRouterClient.complete(request);
        audit(metadata(agent, context, "reason"), response);
        return response.getContent();
    }

    @Override
    public String generateFinalAnswer(Agent agent, List<Message> context) {
        TokenRouterChatRequest request = requestMapper.fromMessagesJson(toMessagesJson(context), agent.getModel(), metadata(agent, context, "final_answer"));
        TokenRouterChatResponse response = tokenRouterClient.complete(request);
        audit(metadata(agent, context, "final_answer"), response);
        return response.getContent();
    }

    @Override
    public String streamFinalAnswer(Agent agent, List<Message> context, Consumer<String> chunkHandler) {
        AgentInvocationMetadata metadata = metadata(agent, context, "final_answer_stream");
        TokenRouterChatRequest request = requestMapper.fromMessagesJson(toMessagesJson(context), agent.getModel(), metadata);
        StringBuilder content = new StringBuilder();
        TokenRouterChatResponse response = tokenRouterClient.streamComplete(request, chunk -> {
            String normalized = normalizeStreamChunk(chunk);
            if (normalized == null || normalized.isEmpty()) {
                return;
            }
            content.append(normalized);
            if (chunkHandler != null) {
                chunkHandler.accept(normalized);
            }
        });
        if (response.getContent() == null || response.getContent().isEmpty()) {
            response.setContent(content.toString());
        }
        audit(metadata, response);
        return response.getContent();
    }

    private AgentInvocationMetadata metadata(Agent agent, List<Message> context, String stepType) {
        AgentInvocationMetadata metadata = new AgentInvocationMetadata();
        metadata.setAgentId(agent.getId());
        metadata.setAgentStepType(stepType);
        Message lastMessage = lastMessage(context);
        if (lastMessage != null) {
            metadata.setAgentSessionId(lastMessage.getSessionId());
            metadata.setAgentStepId(lastMessage.getId());
        }
        return metadata;
    }

    private String toMessagesJson(List<Message> context) {
        List<Map<String, Object>> payload = new ArrayList<>();
        if (context != null) {
            for (Message message : context) {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("role", role(message));
                item.put("content", message.getContent());
                payload.add(item);
            }
        }
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Failed to serialize Agent context messages", ex);
        }
    }

    private String role(Message message) {
        if (message == null || message.getRole() == null) {
            return "user";
        }
        return message.getRole().name().toLowerCase();
    }

    private Message lastMessage(List<Message> context) {
        if (context == null || context.isEmpty()) {
            return null;
        }
        return context.get(context.size() - 1);
    }

    private void audit(AgentInvocationMetadata metadata, TokenRouterChatResponse response) {
        auditService.record(LLMUsageAuditRecord.from(metadata, response));
    }

    @SuppressWarnings("unchecked")
    private String normalizeStreamChunk(String raw) {
        if (raw == null) {
            return "";
        }
        String line = raw.trim();
        if (line.isEmpty() || "[DONE]".equals(line)) {
            return "";
        }
        if (line.startsWith("data:")) {
            line = line.substring("data:".length()).trim();
        }
        if ("[DONE]".equals(line)) {
            return "";
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(line, Map.class);
            Object content = payload.get("content");
            if (content != null) {
                return String.valueOf(content);
            }
            Object choices = payload.get("choices");
            if (choices instanceof List && !((List<?>) choices).isEmpty()) {
                Object first = ((List<?>) choices).get(0);
                if (first instanceof Map) {
                    Object delta = ((Map<?, ?>) first).get("delta");
                    if (delta instanceof Map && ((Map<?, ?>) delta).get("content") != null) {
                        return String.valueOf(((Map<?, ?>) delta).get("content"));
                    }
                    Object message = ((Map<?, ?>) first).get("message");
                    if (message instanceof Map && ((Map<?, ?>) message).get("content") != null) {
                        return String.valueOf(((Map<?, ?>) message).get("content"));
                    }
                }
            }
        } catch (Exception ignored) {
            return line;
        }
        return "";
    }
}
