package com.agenthub.domain.service;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.port.LLMClient;
import com.agenthub.domain.port.FunctionRegistry;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.function.Consumer;

/**
 * ReAct (Reason + Act) 循环引擎
 * 实现 Agent 的推理-行动循环
 */
@Service
public class ReActEngine {

    private final LLMClient llmClient;
    private final FunctionRegistry functionRegistry;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ReActEngine(LLMClient llmClient, FunctionRegistry functionRegistry) {
        this.llmClient = llmClient;
        this.functionRegistry = functionRegistry;
    }

    /**
     * 执行 ReAct 循环
     * @param agent Agent 配置
     * @param session 当前会话
     * @param userMessage 用户输入消息
     * @return 生成的回复消息列表
     */
    public List<Message> executeReActLoop(Agent agent, Session session, Message userMessage) {
        return executeReActLoop(agent, session, userMessage, null);
    }

    public List<Message> executeReActLoop(Agent agent, Session session, Message userMessage, Consumer<String> chunkHandler) {
        List<Message> messages = new ArrayList<>();
        messages.add(userMessage);

        // 构建上下文
        List<Message> context = new ArrayList<>(session.getMessages() == null ? Collections.emptyList() : session.getMessages());
        context.add(userMessage);

        int maxIterations = agent.getMaxIterations() != null && agent.getMaxIterations() > 0 ? agent.getMaxIterations() : 10;
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;

            // 1. Reason: 调用 LLM 进行推理
            String reasoning = llmClient.reason(agent, context);

            // 2. Act: 判断是否需要调用 Function
            ToolCall toolCall = parseToolCall(reasoning);
            if (toolCall != null) {
                // 解析 Function 调用
                // 执行 Function
                Object result = functionRegistry.invoke(toolCall.name, toolCall.arguments);

                // 添加 Function 结果到上下文
                Message functionResult = new Message();
                functionResult.setId(UUID.randomUUID().toString());
                functionResult.setSessionId(session.getId());
                functionResult.setRole(Message.MessageRole.SYSTEM);
                functionResult.setContent("Function " + toolCall.name + " returned: " + result);
                functionResult.setCreatedAt(LocalDateTime.now());
                messages.add(functionResult);
                context.add(functionResult);
            } else {
                // 3. Final Answer: 生成最终回复
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
        }

        throw new IllegalStateException("Agent execution exceeded max iterations: " + maxIterations);
    }

    private ToolCall parseToolCall(String reasoning) {
        if (reasoning == null || reasoning.trim().isEmpty()) {
            return null;
        }
        ToolCall structured = parseStructuredToolCall(reasoning);
        if (structured != null) {
            return structured;
        }
        if (reasoning.contains("FUNCTION_CALL:")) {
            return new ToolCall(extractFunctionName(reasoning), extractArguments(reasoning));
        }
        return null;
    }

    @SuppressWarnings("unchecked")
    private ToolCall parseStructuredToolCall(String reasoning) {
        String text = reasoning.trim();
        if (!text.startsWith("{")) {
            return null;
        }
        try {
            Map<String, Object> payload = objectMapper.readValue(text, new TypeReference<Map<String, Object>>() {});
            Object rawCall = firstNonNull(payload.get("toolCall"), payload.get("tool_call"), payload.get("functionCall"), payload.get("function_call"));
            if (rawCall == null && payload.get("toolCalls") instanceof List && !((List<?>) payload.get("toolCalls")).isEmpty()) {
                rawCall = ((List<?>) payload.get("toolCalls")).get(0);
            }
            if (rawCall == null && payload.get("tool_calls") instanceof List && !((List<?>) payload.get("tool_calls")).isEmpty()) {
                rawCall = ((List<?>) payload.get("tool_calls")).get(0);
            }
            if (!(rawCall instanceof Map)) {
                return null;
            }
            Map<String, Object> call = (Map<String, Object>) rawCall;
            String name = stringValue(firstNonNull(call.get("name"), call.get("functionName"), call.get("function_name")));
            Object rawArguments = firstNonNull(call.get("arguments"), call.get("args"), call.get("input"));
            Map<String, Object> arguments = toArguments(rawArguments);
            if (name == null || name.trim().isEmpty()) {
                return null;
            }
            return new ToolCall(name, arguments);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * 提取 Function 名称
     */
    private String extractFunctionName(String reasoning) {
        // 解析 FUNCTION_CALL: functionName
        String[] parts = reasoning.split("FUNCTION_CALL:");
        if (parts.length > 1) {
            return parts[1].trim().split("\\s+")[0];
        }
        throw new IllegalArgumentException("Invalid function call format: " + reasoning);
    }

    /**
     * 提取 Function 参数
     */
    private Map<String, Object> extractArguments(String reasoning) {
        int marker = reasoning.indexOf("FUNCTION_CALL:");
        if (marker < 0) {
            return Collections.emptyMap();
        }
        String call = reasoning.substring(marker + "FUNCTION_CALL:".length()).trim();
        int jsonStart = call.indexOf('{');
        if (jsonStart < 0) {
            return Collections.emptyMap();
        }
        String json = call.substring(jsonStart).trim();
        try {
            return objectMapper.readValue(json, new TypeReference<Map<String, Object>>() {});
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid function call arguments: " + json, ex);
        }
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

    private static class ToolCall {
        private final String name;
        private final Map<String, Object> arguments;

        private ToolCall(String name, Map<String, Object> arguments) {
            this.name = name;
            this.arguments = arguments;
        }
    }
}
