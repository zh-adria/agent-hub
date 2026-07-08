package com.agenthub.app.AgentManager.Domain.Service.Runtime;

import com.agenthub.app.AgentManager.Domain.Model.Agent;
import com.agenthub.app.AgentManager.Domain.Model.FunctionDefinition;
import com.agenthub.app.AgentManager.Domain.Model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/**
 * ReAct (Reasoning + Acting) 循环引擎默认实现
 * 状态机：IDLE → THINKING → ACTING → OBSERVING → (循环) → FINISHED/ERROR
 */
@Component
public class DefaultReActEngine implements ReActEngine {
    
    private static final Logger logger = LoggerFactory.getLogger(DefaultReActEngine.class);
    
    private volatile ExecutionState state = ExecutionState.IDLE;
    private volatile boolean stopped = false;
    
    // 执行上下文存储
    private final Map<String, ExecutionContext> activeContexts = new ConcurrentHashMap<>();
    
    @Override
    public ReActResult execute(Agent agent, Session session, String userMessage) {
        if (stopped) {
            return new ReActResult(false, null, null, "Engine stopped");
        }
        
        String contextId = String.valueOf(session.getId());
        ExecutionContext context = activeContexts.computeIfAbsent(contextId, 
            k -> new ExecutionContext(agent, session));
        
        try {
            // 1. THINKING: 推理阶段
            state = ExecutionState.THINKING;
            logger.info("[{}] THINKING: Processing user message", contextId);
            
            // 构建 prompt
            String prompt = buildPrompt(context, userMessage);
            
            // 调用 LLM 获取响应（需要 LLMClient）
            // 暂时返回模拟结果
            String llmResponse = mockLLMCall(prompt);
            
            // 2. ACTING: 执行工具调用
            state = ExecutionState.ACTING;
            logger.info("[{}] ACTING: Executing function calls", contextId);
            
            List<FunctionCall> functionCalls = new ArrayList<>();
            
            // 检查是否需要调用工具
            if (llmResponse.contains("function_call")) {
                functionCalls = extractAndExecuteFunctions(context, llmResponse);
            }
            
            // 3. OBSERVING: 观察结果
            state = ExecutionState.OBSERVING;
            logger.info("[{}] OBSERVING: Processing results", contextId);
            
            // 构建最终响应
            String finalResponse = buildFinalResponse(context, llmResponse, functionCalls);
            
            // 4. 完成
            state = ExecutionState.FINISHED;
            logger.info("[{}] FINISHED: ReAct cycle completed", contextId);
            
            return new ReActResult(true, finalResponse, functionCalls);
            
        } catch (Exception e) {
            logger.error("[{}] ERROR: {}", contextId, e.getMessage(), e);
            state = ExecutionState.ERROR;
            return new ReActResult(false, null, null, e.getMessage());
        } finally {
            // 清理上下文
            activeContexts.remove(contextId);
            state = ExecutionState.IDLE;
        }
    }
    
    @Override
    public ExecutionState getState() {
        return state;
    }
    
    @Override
    public void stop() {
        this.stopped = true;
        this.state = ExecutionState.IDLE;
        activeContexts.clear();
        logger.info("ReAct Engine stopped");
    }
    
    /**
     * 构建 prompt
     */
    private String buildPrompt(ExecutionContext context, String userMessage) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("System: ").append(context.getAgent().getSystemPrompt()).append("\n\n");
        prompt.append("User: ").append(userMessage).append("\n\n");
        prompt.append("Assistant: ");
        return prompt.toString();
    }
    
    /**
     * 模拟 LLM 调用
     */
    private String mockLLMCall(String prompt) {
        // TODO: 替换为真实的 LLM 客户端调用
        if (prompt.contains("hello") || prompt.contains("你好")) {
            return "Hello! How can I help you today?";
        }
        return "I understand your request. Let me help you with that.";
    }
    
    /**
     * 提取并执行函数调用
     */
    private List<FunctionCall> extractAndExecuteFunctions(ExecutionContext context, String llmResponse) {
        List<FunctionCall> calls = new ArrayList<>();
        
        // TODO: 实现函数调用提取逻辑
        // 1. 解析 LLM 响应中的 function_call
        // 2. 查找对应的 FunctionDefinition
        // 3. 执行函数
        // 4. 记录结果
        
        return calls;
    }
    
    /**
     * 构建最终响应
     */
    private String buildFinalResponse(ExecutionContext context, String llmResponse, 
                                       List<FunctionCall> functionCalls) {
        if (functionCalls.isEmpty()) {
            return llmResponse;
        }
        
        StringBuilder response = new StringBuilder(llmResponse);
        response.append("\n\nExecuted functions:\n");
        for (FunctionCall call : functionCalls) {
            response.append(String.format("- %s: %s\n", 
                call.getFunctionName(), 
                call.isSuccess() ? "Success" : "Failed"));
        }
        return response.toString();
    }
    
    /**
     * 执行上下文
     */
    private static class ExecutionContext {
        private final Agent agent;
        private final Session session;
        private final List<String> conversationHistory = new ArrayList<>();
        
        ExecutionContext(Agent agent, Session session) {
            this.agent = agent;
            this.session = session;
        }
        
        Agent getAgent() { return agent; }
        Session getSession() { return session; }
        List<String> getConversationHistory() { return conversationHistory; }
    }
}
