package com.agenthub.client.impl;

import com.agenthub.client.audit.InMemoryLLMUsageAuditService;
import com.agenthub.client.tokenrouter.TokenRouterChatRequest;
import com.agenthub.client.tokenrouter.TokenRouterChatResponse;
import com.agenthub.client.tokenrouter.TokenRouterClient;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRouterDomainLLMClientTest {

    @Test
    void propagatesAgentSessionAndStepMetadataFromDomainRuntime() {
        CapturingTokenRouterClient tokenRouterClient = new CapturingTokenRouterClient();
        TokenRouterDomainLLMClient domainClient = new TokenRouterDomainLLMClient(
                tokenRouterClient, new ObjectMapper(), new InMemoryLLMUsageAuditService());
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setModel("gpt-test");
        Message message = new Message();
        message.setId("step-1");
        message.setSessionId("session-1");
        message.setRole(Message.MessageRole.USER);
        message.setContent("hello");

        String result = domainClient.reason(agent, Collections.singletonList(message));

        assertThat(result).isEqualTo("ok");
        assertThat(tokenRouterClient.extensions.get("agentId")).isEqualTo("agent-1");
        assertThat(tokenRouterClient.extensions.get("agentSessionId")).isEqualTo("session-1");
        assertThat(tokenRouterClient.extensions.get("agentStepId")).isEqualTo("step-1");
        assertThat(tokenRouterClient.extensions.get("agentStepType")).isEqualTo("reason");
    }

    @Test
    void propagatesWorkflowFinalAnswerStepType() {
        CapturingTokenRouterClient tokenRouterClient = new CapturingTokenRouterClient();
        TokenRouterDomainLLMClient domainClient = new TokenRouterDomainLLMClient(
                tokenRouterClient, new ObjectMapper(), new InMemoryLLMUsageAuditService());
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setModel("gpt-test");
        Message message = new Message();
        message.setId("step-final");
        message.setSessionId("session-1");
        message.setRole(Message.MessageRole.ASSISTANT);
        message.setContent("reasoned answer");

        domainClient.generateFinalAnswer(agent, Collections.singletonList(message));

        assertThat(tokenRouterClient.extensions.get("agentId")).isEqualTo("agent-1");
        assertThat(tokenRouterClient.extensions.get("agentSessionId")).isEqualTo("session-1");
        assertThat(tokenRouterClient.extensions.get("agentStepId")).isEqualTo("step-final");
        assertThat(tokenRouterClient.extensions.get("agentStepType")).isEqualTo("final_answer");
    }

    @Test
    void streamsFinalAnswerChunksFromTokenRouter() {
        CapturingTokenRouterClient tokenRouterClient = new CapturingTokenRouterClient();
        tokenRouterClient.streamChunks.add("data: {\"choices\":[{\"delta\":{\"content\":\"hel\"}}]}");
        tokenRouterClient.streamChunks.add("data: {\"choices\":[{\"delta\":{\"content\":\"lo\"}}]}");
        TokenRouterDomainLLMClient domainClient = new TokenRouterDomainLLMClient(
                tokenRouterClient, new ObjectMapper(), new InMemoryLLMUsageAuditService());
        Agent agent = new Agent();
        agent.setId("agent-1");
        agent.setModel("gpt-test");
        Message message = new Message();
        message.setId("step-stream");
        message.setSessionId("session-1");
        message.setRole(Message.MessageRole.USER);
        message.setContent("hello");
        List<String> chunks = new ArrayList<>();

        String result = domainClient.streamFinalAnswer(agent, Collections.singletonList(message), chunks::add);

        assertThat(result).isEqualTo("hello");
        assertThat(chunks).containsExactly("hel", "lo");
        assertThat(tokenRouterClient.extensions.get("agentStepType")).isEqualTo("final_answer_stream");
    }

    private static class CapturingTokenRouterClient implements TokenRouterClient {
        java.util.Map<String, Object> extensions;
        String model;
        List<String> streamChunks = new ArrayList<>();

        @Override
        public TokenRouterChatResponse complete(TokenRouterChatRequest request) {
            this.extensions = request.getExtensions();
            this.model = request.getModelHint();
            TokenRouterChatResponse response = new TokenRouterChatResponse();
            response.setContent("ok");
            return response;
        }

        @Override
        public TokenRouterChatResponse streamComplete(TokenRouterChatRequest request, Consumer<String> chunkHandler) {
            this.extensions = request.getExtensions();
            this.model = request.getModelHint();
            for (String chunk : streamChunks) {
                chunkHandler.accept(chunk);
            }
            return new TokenRouterChatResponse();
        }
    }
}
