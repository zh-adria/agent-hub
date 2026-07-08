package com.agenthub.domain.service;

import com.agenthub.domain.model.agent.Agent;
import com.agenthub.domain.model.session.Session;
import com.agenthub.domain.model.message.Message;
import com.agenthub.domain.port.LLMClient;
import com.agenthub.domain.port.FunctionRegistry;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * ReAct (Reason + Act) 循环引擎
 * 实现 Agent 的推理-行动循环
 */
@Service
public class ReActEngine {

    private final LLMClient llmClient;
    private final FunctionRegistry functionRegistry;

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
        List<Message> messages = new ArrayList<>();
        messages.add(userMessage);

        // 构建上下文
        List<Message> context = new ArrayList<>(session.getHistory());
        context.add(userMessage);

        int maxIterations = agent.getMaxIterations() != null ? agent.getMaxIterations() : 10;
        int iteration = 0;

        while (iteration < maxIterations) {
            iteration++;

            // 1. Reason: 调用 LLM 进行推理
            String reasoning = llmClient.reason(agent, context);

            // 2. Act: 判断是否需要调用 Function
            if (shouldCallFunction(reasoning)) {
                // 解析 Function 调用
                String functionName = extractFunctionName(reasoning);
                Map<String, Object> arguments = extractArguments(reasoning);

                // 执行 Function
                Object result = functionRegistry.invoke(functionName, arguments);

                // 添加 Function 结果到上下文
                Message functionResult = Message.builder()
                        .role(Message.Role.SYSTEM)
                        .content("Function " + functionName + " returned: " + result)
                        .build();
                messages.add(functionResult);
                context.add(functionResult);
            } else {
                // 3. Final Answer: 生成最终回复
                String finalAnswer = llmClient.generateFinalAnswer(agent, context);

                Message assistantMessage = Message.builder()
                        .role(Message.Role.ASSISTANT)
                        .content(finalAnswer)
                        .build();
                messages.add(assistantMessage);
                break;
            }
        }

        return messages;
    }

    /**
     * 判断是否需要调用 Function
     */
    private boolean shouldCallFunction(String reasoning) {
        // 简单实现：检查是否包含 Function 调用标记
        return reasoning.contains("FUNCTION_CALL:");
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
        // TODO: 实现参数解析逻辑
        return Map.of();
    }
}
