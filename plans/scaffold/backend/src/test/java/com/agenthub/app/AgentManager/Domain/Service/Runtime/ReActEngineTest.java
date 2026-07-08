package com.agenthub.app.AgentManager.Domain.Service.Runtime;

import com.agenthub.app.AgentManager.Domain.Model.Agent;
import com.agenthub.app.AgentManager.Domain.Model.Session;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ReAct 循环引擎单元测试
 */
@DisplayName("ReAct Engine 状态机测试")
class ReActEngineTest {
    
    private DefaultReActEngine engine;
    private Agent testAgent;
    private Session testSession;
    
    @BeforeEach
    void setUp() {
        engine = new DefaultReActEngine();
        
        testAgent = new Agent();
        testAgent.setId(1L);
        testAgent.setName("Test Agent");
        testAgent.setSystemPrompt("You are a helpful assistant.");
        
        testSession = new Session();
        testSession.setId(1L);
        testSession.setAgentId(testAgent.getId());
    }
    
    @Test
    @DisplayName("测试 ReAct 循环完成")
    void testExecute_Success() {
        // 执行 ReAct 循环
        ReActEngine.ReActResult result = engine.execute(testAgent, testSession, "Hello");
        
        // 验证结果
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertNotNull(result.getResponse());
        assertTrue(result.getFunctionCalls().isEmpty()); // 简单消息不触发工具调用
        
        // 验证状态流转
        assertEquals(ReActEngine.ExecutionState.IDLE, engine.getState());
    }
    
    @Test
    @DisplayName("测试 ReAct 循环中文消息")
    void testExecute_ChineseMessage() {
        ReActEngine.ReActResult result = engine.execute(testAgent, testSession, "你好");
        
        assertNotNull(result);
        assertTrue(result.isSuccess());
        assertTrue(result.getResponse().toLowerCase().contains("hello"));
    }
    
    @Test
    @DisplayName("测试引擎停止")
    void testStop() {
        engine.stop();
        assertEquals(ReActEngine.ExecutionState.IDLE, engine.getState());
        
        ReActEngine.ReActResult result = engine.execute(testAgent, testSession, "Test");
        assertFalse(result.isSuccess());
        assertEquals("Engine stopped", result.getErrorMessage());
    }
    
    @Test
    @DisplayName("测试状态流转")
    void testStateTransitions() {
        // 初始状态
        assertEquals(ReActEngine.ExecutionState.IDLE, engine.getState());
    }
}
