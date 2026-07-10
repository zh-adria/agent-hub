package com.agenthub.client.tokenrouter;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

public class TokenRouterRequestMapper {
    private static final String DEFAULT_BUSINESS_TAG = "agenthub-runtime";

    private final ObjectMapper objectMapper;

    public TokenRouterRequestMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TokenRouterChatRequest fromPrompt(String prompt, String modelHint, AgentInvocationMetadata metadata) {
        TokenRouterChatRequest request = baseRequest(modelHint, metadata);
        request.setMessages(Collections.singletonList(new TokenRouterMessage("user", prompt)));
        return request;
    }

    public TokenRouterChatRequest fromMessagesJson(String messagesJson, String modelHint, AgentInvocationMetadata metadata) {
        TokenRouterChatRequest request = baseRequest(modelHint, metadata);
        request.setMessages(parseMessages(messagesJson));
        return request;
    }

    private TokenRouterChatRequest baseRequest(String modelHint, AgentInvocationMetadata metadata) {
        AgentInvocationMetadata safeMetadata = metadata != null ? metadata : AgentInvocationMetadata.empty();
        TokenRouterChatRequest request = new TokenRouterChatRequest();
        request.setBusinessTag(valueOrDefault(safeMetadata.getBusinessTag(), DEFAULT_BUSINESS_TAG));
        request.setUserId(safeMetadata.getUserId());
        request.setPolicyId(safeMetadata.getPolicyId());
        request.setModelHint(modelHint);
        request.setStream(false);
        request.setExtensions(safeMetadata.toExtensions());
        return request;
    }

    private List<TokenRouterMessage> parseMessages(String messagesJson) {
        if (isBlank(messagesJson)) {
            return Collections.emptyList();
        }
        try {
            List<Map<String, Object>> rawMessages = objectMapper.readValue(
                    messagesJson,
                    new TypeReference<List<Map<String, Object>>>() {}
            );
            List<TokenRouterMessage> messages = new ArrayList<>();
            for (Map<String, Object> rawMessage : rawMessages) {
                messages.add(new TokenRouterMessage(
                        String.valueOf(rawMessage.getOrDefault("role", "user")),
                        String.valueOf(rawMessage.getOrDefault("content", ""))
                ));
            }
            return messages;
        } catch (IOException ex) {
            return Collections.singletonList(new TokenRouterMessage("user", messagesJson));
        }
    }

    private String valueOrDefault(String value, String defaultValue) {
        return isBlank(value) ? defaultValue : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
