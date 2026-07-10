package com.agenthub.client.tokenrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class TokenRouterRequestMapperTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final TokenRouterRequestMapper mapper = new TokenRouterRequestMapper(objectMapper);

    @Test
    void mapsAgentRuntimeMetadataIntoTokenRouterExtensions() {
        AgentInvocationMetadata metadata = new AgentInvocationMetadata();
        metadata.setBusinessTag("tenant-a");
        metadata.setUserId("user-1");
        metadata.setPolicyId("policy-1");
        metadata.setAgentId("agent-1");
        metadata.setAgentSessionId("session-1");
        metadata.setAgentStepId("step-1");
        metadata.setAgentStepType("reason");
        metadata.setTraceId("trace-1");
        metadata.setToolNames(Arrays.asList("search", "calculator"));
        metadata.setKnowledgeBaseId("kb-1");

        TokenRouterChatRequest request = mapper.fromPrompt("hello", "gpt-test", metadata);

        assertThat(request.getBusinessTag()).isEqualTo("tenant-a");
        assertThat(request.getUserId()).isEqualTo("user-1");
        assertThat(request.getPolicyId()).isEqualTo("policy-1");
        assertThat(request.getModelHint()).isEqualTo("gpt-test");
        assertThat(request.getMessages()).hasSize(1);
        assertThat(request.getMessages().get(0).getRole()).isEqualTo("user");
        assertThat(request.getMessages().get(0).getContent()).isEqualTo("hello");
        assertThat(request.getExtensions())
                .containsEntry("agentId", "agent-1")
                .containsEntry("agentSessionId", "session-1")
                .containsEntry("agentStepId", "step-1")
                .containsEntry("agentStepType", "reason")
                .containsEntry("traceId", "trace-1")
                .containsEntry("knowledgeBaseId", "kb-1");
        assertThat(request.getExtensions().get("toolNames")).isEqualTo(Arrays.asList("search", "calculator"));
    }

    @Test
    void keepsRagOwnershipInAgentHubBySendingKnowledgeBaseIdOnlyAsExtension() throws Exception {
        AgentInvocationMetadata metadata = new AgentInvocationMetadata();
        metadata.setKnowledgeBaseId("kb-agenthub-owned");

        TokenRouterChatRequest request = mapper.fromPrompt("retrieve context", "gpt-test", metadata);
        String json = objectMapper.writeValueAsString(request);
        JsonNode root = objectMapper.readTree(json);

        assertThat(request.getExtensions()).containsEntry("knowledgeBaseId", "kb-agenthub-owned");
        assertThat(root.has("knowledgeBaseId")).isFalse();
        assertThat(root.at("/extensions/knowledgeBaseId").asText()).isEqualTo("kb-agenthub-owned");
    }
}
