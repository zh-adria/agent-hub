package com.agenthub.app.AgentManager.Domain.Service.Runtime;

import com.agenthub.app.AgentManager.Domain.Model.Agent;
import com.agenthub.app.AgentManager.Domain.Model.FunctionDefinition;
import com.agenthub.app.AgentManager.Domain.Model.Session;

import java.util.List;
import java.util.Map;

/**
 * ReAct (Reasoning + Acting) 循环引擎
 * 状态机：IDLE → THINKING → ACTING → OBSERVING → (循环) → FINISHED/ERROR
 */
public interface ReActEngine {
    
    /**
     * 执行 ReAct 循环
     * @param agent 配置的 Agent
     * @param session 当前会话
     * @param userMessage 用户输入消息
     * @return 执行结果
     */
    ReActResult execute(Agent agent, Session session, String userMessage);
    
    /**
     * 获取当前执行状态
     */
    ExecutionState getState();
    
    /**
     * 停止执行
     */
    void stop();
    
    /**
     * 执行状态枚举
     */
    enum ExecutionState {
        IDLE,        // 空闲
        THINKING,    // 推理中
        ACTING,      // 执行工具中
        OBSERVING,   // 观察结果中
        FINISHED,    // 完成
        ERROR        // 错误
    }
    
    /**
     * ReAct 执行结果
     */
    class ReActResult {
        private final boolean success;
        private final String response;
        private final List<FunctionCall> functionCalls;
        private final String errorMessage;
        
        public ReActResult(boolean success, String response, List<FunctionCall> functionCalls) {
            this(success, response, functionCalls, null);
        }
        
        public ReActResult(boolean success, String response, List<FunctionCall> functionCalls, String errorMessage) {
            this.success = success;
            this.response = response;
            this.functionCalls = functionCalls;
            this.errorMessage = errorMessage;
        }
        
        public boolean isSuccess() { return success; }
        public String getResponse() { return response; }
        public List<FunctionCall> getFunctionCalls() { return functionCalls; }
        public String getErrorMessage() { return errorMessage; }
    }
    
    /**
     * 函数调用记录
     */
    class FunctionCall {
        private final String functionName;
        private final Map<String, Object> arguments;
        private final String result;
        private final boolean success;
        
        public FunctionCall(String functionName, Map<String, Object> arguments, String result, boolean success) {
            this.functionName = functionName;
            this.arguments = arguments;
            this.result = result;
            this.success = success;
        }
        
        public String getFunctionName() { return functionName; }
        public Map<String, Object> getArguments() { return arguments; }
        public String getResult() { return result; }
        public boolean isSuccess() { return success; }
    }
}
