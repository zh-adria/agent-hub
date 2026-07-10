package com.agenthub.client.tokenrouter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.Map;

public class TokenRouterResponseMapper {
    private final ObjectMapper objectMapper;

    public TokenRouterResponseMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public TokenRouterChatResponse fromMap(Map<String, Object> body) {
        JsonNode root = objectMapper.valueToTree(body);
        TokenRouterChatResponse response = new TokenRouterChatResponse();
        response.setRaw(body);
        response.setProvider(textAt(root, "provider"));
        response.setModel(firstText(root, "model", "selectedModel"));
        response.setContent(firstText(root, "content", "/choices/0/message/content"));
        response.setPromptTokens(firstInt(root, "promptTokens", "/usage/prompt_tokens", "/usage/promptTokens"));
        response.setCompletionTokens(firstInt(root, "completionTokens", "/usage/completion_tokens", "/usage/completionTokens"));
        response.setTotalTokens(firstInt(root, "totalTokens", "/usage/total_tokens", "/usage/totalTokens"));
        response.setCost(firstDecimal(root, "cost", "/usage/cost"));
        response.setRouteDecision(firstText(root, "routeDecision", "/route/decision"));
        response.setRouteReason(firstText(root, "routeReason", "/route/reason"));
        return response;
    }

    private String firstText(JsonNode root, String... paths) {
        for (String path : paths) {
            String value = textAt(root, path);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode root, String... paths) {
        for (String path : paths) {
            JsonNode node = nodeAt(root, path);
            if (node != null && node.isNumber()) {
                return node.intValue();
            }
        }
        return null;
    }

    private BigDecimal firstDecimal(JsonNode root, String... paths) {
        for (String path : paths) {
            JsonNode node = nodeAt(root, path);
            if (node != null && node.isNumber()) {
                return node.decimalValue();
            }
        }
        return null;
    }

    private String textAt(JsonNode root, String path) {
        JsonNode node = nodeAt(root, path);
        return node == null || node.isNull() ? null : node.asText();
    }

    private JsonNode nodeAt(JsonNode root, String path) {
        JsonNode node = path.startsWith("/") ? root.at(path) : root.get(path);
        return node == null || node.isMissingNode() ? null : node;
    }
}
