package com.agenthub.domain.service;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.model.ToolCall;
import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.port.LLMClient;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Consumer;

/**
 * ReAct (Reason + Act) 循环引擎
 *
 * <p>Enhanced with structured tool call protocol, timeout protection,
 * error classification, and context compression support.</p>
 */
@Service
public class ReActEngine {

    private static final Logger log = LoggerFactory.getLogger(ReActEngine.class);
    private static final int DEFAULT_MAX_ITERATIONS = 10;
    private static final Duration DEFAULT_TOOL_TIMEOUT = Duration.ofSeconds(30);

    private final LLMClient llmClient;
    private final FunctionRegistry functionRegistry;
    private final SessionContextCompressor contextCompressor;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReActEngine(LLMClient llmClient, FunctionRegistry functionRegistry, SessionContextCompressor contextCompressor) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
        this.contextCompressor = contextCompressor;
    }

    /**
     * Execute ReAct loop with structured tool call protocol.
     *
     * @param agent        Agent configuration
     * @param session      Current session
     * @param userMessage  User input message
     * @param chunkHandler Optional streaming handler for final answer
     * @return Generated messages including tool calls and final answer
     */
    public List<Message> executeReActLoop(Agent agent, Session session, Message userMessage, Consumer<String> chunkHandler) {
        return executeReActLoop(agent, session, userMessage, chunkHandler,
                resolveMaxIterations(agent), resolveToolTimeout(agent));
    }

    public List<Message> executeReActLoop(Agent agent, Session session, Message userMessage) {
        return executeReActLoop(agent, session, userMessage, null,
                resolveMaxIterations(agent), resolveToolTimeout(agent));
    }

    /**
     * Execute ReAct loop with configurable limits.
     *
     * @param maxIterations Maximum reasoning loops before forced termination
     * @param toolTimeout   Timeout per individual tool invocation
     */
    public List<Message> executeReActLoop(Agent agent, Session session, Message userMessage,
                                          Consumer<String> chunkHandler, int maxIterations, Duration toolTimeout) {
        List<Message> messages = new ArrayList<>();
        messages.add(userMessage);

        List<Message> context = new ArrayList<>(session.getMessages() == null ? Collections.emptyList() : session.getMessages());
        context.add(userMessage);

        // Compress context if it exceeds token budget
        if (context.size() > 10) {
            int estimatedTokens = contextCompressor.estimateTotalTokens(context);
            int tokenBudget = agent.getMaxTokens() != null ? agent.getMaxTokens() * 2 : 8000;
            if (estimatedTokens > tokenBudget) {
                log.debug("Compressing session context: {} messages, ~{} tokens", context.size(), estimatedTokens);
                context = contextCompressor.compress(session, SessionContextCompressor.Strategy.TOKEN_BUDGET, 20, tokenBudget);
                context.add(userMessage); // Re-add current user message after compression
                log.debug("Compressed context: {} messages", context.size());
            }
        }

        int iteration = 0;
        while (iteration < maxIterations) {
            iteration++;

            // 1. Reason: call LLM for reasoning
            String reasoning = llmClient.reason(agent, context);

            // 2. Act: parse tool calls from structured JSON protocol
            List<ToolCall> toolCalls = parseToolCalls(reasoning);
            if (toolCalls.isEmpty()) {
                // 3. Final Answer: no tool calls detected, generate final response
                String finalAnswer = chunkHandler != null
                        ? llmClient.streamFinalAnswer(agent, context, chunkHandler)
                        : llmClient.generateFinalAnswer(agent, context);

                Message assistantMessage = new Message();
                assistantMessage.setId(UUID.randomUUID().toString());
                assistantMessage.setSessionId(session.getId());
                assistantMessage.setRole(Message.MessageRole.ASSISTANT);
                assistantMessage.setContent(finalAnswer);
                assistantMessage.setCreatedAt(LocalDateTime.now());
                messages.add(assistantMessage);
                return messages;
            }

            // 3. Act: execute all tool calls (parallel-capable, currently sequential)
            for (ToolCall toolCall : toolCalls) {
                try {
                    Object result = invokeWithTimeout(agent, toolCall, toolTimeout);
                    String resultContent = formatToolResult(toolCall, result);

                    Message functionResult = new Message();
                    functionResult.setId(UUID.randomUUID().toString());
                    functionResult.setSessionId(session.getId());
                    functionResult.setRole(Message.MessageRole.SYSTEM);
                    functionResult.setContent(resultContent);
                    functionResult.setCreatedAt(LocalDateTime.now());
                    messages.add(functionResult);
                    context.add(functionResult);
                } catch (ToolInvocationException ex) {
                    String errorContent = "[ToolError] " + ex.getToolName() + " -> " + ex.getCategory() + ": " + ex.getMessage();

                    Message errorResult = new Message();
                    errorResult.setId(UUID.randomUUID().toString());
                    errorResult.setSessionId(session.getId());
                    errorResult.setRole(Message.MessageRole.SYSTEM);
                    errorResult.setContent(errorContent);
                    errorResult.setCreatedAt(LocalDateTime.now());
                    messages.add(errorResult);
                    context.add(errorResult);
                }
            }
        }

        throw new IllegalStateException("Agent execution exceeded max iterations: " + maxIterations);
    }

    /**
     * Parse tool calls from LLM response using structured JSON protocol.
     *
     * <p>Supports multiple formats:
     * <ul>
     *   <li>{@code {"toolCall": {"name": "...", "arguments": {...}}}}</li>
     *   <li>{@code {"tool_calls": [{"function": {"name": "...", "arguments": "..."}}]}}</li>
     *   <li>{@code {"tool_use": {"name": "...", "input": {...}}}}</li>
     *   <li>Legacy: {@code FUNCTION_CALL: name {args}}</li>
     * </ul>
     * </p>
     */
    List<ToolCall> parseToolCalls(String reasoning) {
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String text = reasoning.trim();

        // Try structured JSON protocol first (preferred)
        List<ToolCall> structured = parseStructuredToolCalls(text);
        if (!structured.isEmpty()) {
            return structured;
        }

        // Fallback to legacy text-based protocol
        if (text.contains("FUNCTION_CALL:")) {
            return parseLegacyToolCall(text);
        }

        return Collections.emptyList();
    }

    /**
     * Parse structured JSON tool calls from LLM response.
     * Returns empty list if the response is not a structured tool call.
     */
    @SuppressWarnings("unchecked")
    List<ToolCall> parseStructuredToolCalls(String text) {
        if (!text.startsWith("{")) {
            return Collections.emptyList();
        }

        try {
            Map<String, Object> payload = objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});

            // Check for multiple tool calls first
            List<ToolCall> toolCalls = new ArrayList<>();

            // OpenAI-style: "tool_calls" array
            List<Map<String, Object>> openAiCalls = extractToolCallsArray(payload, "tool_calls", "toolCalls");
            if (openAiCalls != null && !openAiCalls.isEmpty()) {
                for (int i = 0; i < openAiCalls.size(); i++) {
                    Map<String, Object> call = openAiCalls.get(i);
                    Map<String, Object> function = (Map<String, Object>) call.get("function");
                    if (function != null) {
                        String name = stringValue(function.get("name"));
                        Map<String, Object> args = toArguments(function.get("arguments"));
                        if (name != null) {
                            toolCalls.add(new ToolCall(name, args, "openai-tool_calls", i));
                        }
                    }
                }
                return toolCalls;
            }

            // Anthropic-style: "tool_use" (single)
            Map<String, Object> toolUse = (Map<String, Object>) payload.get("tool_use");
            if (toolUse != null) {
                String name = stringValue(toolUse.get("name"));
                Map<String, Object> input = toArguments(toolUse.get("input"));
                if (name != null) {
                    toolCalls.add(new ToolCall(name, input, "anthropic-tool_use", 0));
                }
                return toolCalls;
            }

            // AgentHub native: "toolCall" (single)
            Map<String, Object> rawCall = (Map<String, Object>) firstNonNull(
                    payload.get("toolCall"), payload.get("tool_call"), payload.get("functionCall"), payload.get("function_call")
            );
            if (rawCall != null) {
                String name = stringValue(firstNonNull(rawCall.get("name"), rawCall.get("functionName"), rawCall.get("function_name")));
                Map<String, Object> args = toArguments(firstNonNull(rawCall.get("arguments"), rawCall.get("args"), rawCall.get("input")));
                if (name != null) {
                    toolCalls.add(new ToolCall(name, args, "native-toolCall", 0));
                }
                return toolCalls;
            }

            return Collections.emptyList();
        } catch (Exception ex) {
            log.debug("Failed to parse structured tool call from: {}", text, ex);
            return Collections.emptyList();
        }
    }

    /**
     * Extract tool calls array from payload, trying multiple key names.
     */
    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> extractToolCallsArray(Map<String, Object> payload, String... keys) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof List) {
                List<Map<String, Object>> result = new ArrayList<>();
                for (Object item : (List<?>) value) {
                    if (item instanceof Map) {
                        result.add((Map<String, Object>) item);
                    }
                }
                return result;
            }
        }
        return null;
    }

    /**
     * Parse legacy text-based tool call format: {@code FUNCTION_CALL: name {args}}
     */
    private List<ToolCall> parseLegacyToolCall(String reasoning) {
        List<ToolCall> calls = new ArrayList<>();
        int marker = reasoning.indexOf("FUNCTION_CALL:");
        if (marker < 0) {
            return calls;
        }
        String call = reasoning.substring(marker + "FUNCTION_CALL:".length()).trim();
        String name = call.split("\\s+")[0];
        int jsonStart = call.indexOf('{');
        if (jsonStart >= 0) {
            try {
                Map<String, Object> args = objectMapper.readValue(call.substring(jsonStart), new TypeReference<Map<String, Object>>() {});
                calls.add(new ToolCall(name, args, "legacy-FUNCTION_CALL", 0));
            } catch (JsonProcessingException ex) {
                calls.add(new ToolCall(name, Collections.emptyMap(), "legacy-FUNCTION_CALL", 0));
            }
        } else {
            calls.add(new ToolCall(name, Collections.emptyMap(), "legacy-FUNCTION_CALL", 0));
        }
        return calls;
    }

    /**
     * Invoke a tool with timeout protection and error classification.
     */
    private Object invokeWithTimeout(Agent agent, ToolCall toolCall, Duration timeout) {
        String functionId = toolCall.getName();
        try {
            // Use a separate thread with timeout for tool invocation
            java.util.concurrent.Future<Object> future = java.util.concurrent.CompletableFuture.supplyAsync(() -> {
                try {
                    return functionRegistry.invoke(functionId, toolCall.getArguments());
                } catch (Exception ex) {
                    throw new ToolInvocationException(toolCall, ToolInvocationException.ErrorCategory.EXECUTION_ERROR, ex);
                }
            });

            Object result = future.get(timeout.toMillis(), java.util.concurrent.TimeUnit.MILLISECONDS);
            return result;
        } catch (java.util.concurrent.TimeoutException ex) {
            log.error("Tool invocation timed out: {} (timeout: {})", functionId, timeout);
            throw new ToolInvocationException(toolCall, ToolInvocationException.ErrorCategory.TIMEOUT,
                    "Tool invocation timed out after " + timeout.toMillis() + "ms");
        } catch (java.util.concurrent.ExecutionException ex) {
            Throwable cause = ex.getCause();
            if (cause instanceof ToolInvocationException) {
                throw (ToolInvocationException) cause;
            }
            throw new ToolInvocationException(toolCall, ToolInvocationException.ErrorCategory.EXECUTION_ERROR, cause);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new ToolInvocationException(toolCall, ToolInvocationException.ErrorCategory.INTERRUPTED, ex);
        }
    }

    private String formatToolResult(ToolCall toolCall, Object result) {
        try {
            String json = objectMapper.writeValueAsString(result);
            return "[ToolResult] " + toolCall.getName() + " -> " + json;
        } catch (JsonProcessingException ex) {
            return "[ToolResult] " + toolCall.getName() + " -> " + String.valueOf(result);
        }
    }

    private int resolveMaxIterations(Agent agent) {
        if (agent.getMaxIterations() != null && agent.getMaxIterations() > 0) {
            return agent.getMaxIterations();
        }
        return DEFAULT_MAX_ITERATIONS;
    }

    private Duration resolveToolTimeout(Agent agent) {
        if (agent.getToolTimeoutMs() != null && agent.getToolTimeoutMs() > 0) {
            return Duration.ofMillis(agent.getToolTimeoutMs());
        }
        return DEFAULT_TOOL_TIMEOUT;
    }

    private Object firstNonNull(Object... values) {
        for (Object value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String stringValue(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> toArguments(Object rawArguments) {
        if (rawArguments instanceof Map) {
            return (Map<String, Object>) rawArguments;
        }
        if (rawArguments instanceof String && !((String) rawArguments).trim().isEmpty()) {
            try {
                return objectMapper.readValue((String) rawArguments, new TypeReference<Map<String, Object>>() {});
            } catch (Exception ex) {
                return Collections.emptyMap();
            }
        }
        return Collections.emptyMap();
    }
}
