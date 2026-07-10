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
}
