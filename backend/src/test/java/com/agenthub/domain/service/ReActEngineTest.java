package com.agenthub.domain.service;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.port.LLMClient;
import com.agenthub.domain.service.SessionContextCompressor;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ReActEngineTest {
    @Test
    void executesStructuredToolCallThenFinalAnswer() {
        LLMClient llmClient = mock(LLMClient.class);
        FunctionRegistry functionRegistry = mock(FunctionRegistry.class);
        SessionContextCompressor compressor = mock(SessionContextCompressor.class);
        ReActEngine engine = new ReActEngine(llmClient, functionRegistry, compressor);
        when(llmClient.reason(any(Agent.class), any(List.class)))
                .thenReturn("{\"toolCall\":{\"name\":\"lookup\",\"arguments\":{\"q\":\"agent\"}}}")
                .thenReturn("READY");
        when(functionRegistry.invoke(any(String.class), any(Map.class))).thenReturn(Collections.singletonMap("answer", "42"));
        when(llmClient.generateFinalAnswer(any(Agent.class), any(List.class))).thenReturn("done");
        when(compressor.estimateTotalTokens(any(List.class))).thenReturn(100); // Below budget, no compression

        Agent agent = new Agent();
        agent.setMaxIterations(3);
        Session session = new Session();
        session.setId("s1");
        session.setMessages(new ArrayList<>());
        Message user = new Message();
        user.setSessionId("s1");
        user.setRole(Message.MessageRole.USER);
        user.setContent("question");

        List<Message> messages = engine.executeReActLoop(agent, session, user);

        assertEquals("done", messages.get(messages.size() - 1).getContent());
        // Structured tool call protocol produces [ToolResult] messages
        assertTrue(messages.stream().anyMatch(message -> message.getContent() != null && message.getContent().contains("[ToolResult]")));
        assertTrue(messages.stream().anyMatch(message -> message.getContent() != null && message.getContent().contains("lookup")));
        Map<String, Object> expected = new LinkedHashMap<>();
        expected.put("q", "agent");
        verify(functionRegistry).invoke("lookup", expected);
    }
}
