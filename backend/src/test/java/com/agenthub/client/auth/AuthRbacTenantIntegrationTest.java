package com.agenthub.client.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AuthRbacTenantIntegrationTest {
    @Autowired
    private MockMvc mockMvc;

    @Test
    void mockLoginReturnsUsableToken() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenantCode\":\"tenant-001\",\"username\":\"admin\",\"password\":\"demo\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("mock-token"))
                .andExpect(jsonPath("$.tenantId").value("tenant-001"));

        mockMvc.perform(get("/api/auth/me")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-001"));
    }

    @Test
    void mockIdentityEndpointsExposeContracts() throws Exception {
        mockMvc.perform(post("/mock/oauth2/introspect")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"mock-token\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.active").value(true))
                .andExpect(jsonPath("$.permissions", hasItem("agent:create")));

        mockMvc.perform(post("/mock/rbac/authorize")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"token\":\"reader-token\",\"tenantId\":\"tenant-001\",\"action\":\"agent:create\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.allowed").value(false))
                .andExpect(jsonPath("$.reason", containsString("denies")));
    }

    @Test
    void protectedApiRejectsMissingOrInactiveToken() throws Exception {
        mockMvc.perform(get("/api/agents")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));

        mockMvc.perform(get("/api/agents")
                        .header("Authorization", "Bearer expired")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.error").value("UNAUTHORIZED"));
    }

    @Test
    void rbacDeniesWriteWithoutPermission() throws Exception {
        mockMvc.perform(post("/api/agents")
                        .header("Authorization", "Bearer reader-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"blocked\",\"prompt\":\"blocked\",\"model\":\"gpt-4o-mini\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.error").value("FORBIDDEN"));
    }

    @Test
    void resourcesAreScopedByTenant() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/agents")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tenant-one-agent\",\"prompt\":\"hello\",\"model\":\"gpt-4o-mini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();

        String agentId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/agents/" + agentId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-002"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void mcpToolImportCreatesFunctionDefinition() throws Exception {
        mockMvc.perform(post("/api/mcp/tools/import")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tools\":[{\"name\":\"mcp_search\",\"description\":\"MCP search\",\"endpoint\":\"http://127.0.0.1:9000/search\",\"method\":\"POST\",\"inputSchema\":{\"type\":\"object\"}}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("mcp_search"))
                .andExpect(jsonPath("$[0].implementation").value("mcp"));
    }

    @Test
    void workflowCrudIsTenantScopedAndAuthorized() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/workflows")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"triage\",\"description\":\"test workflow\",\"definition\":{\"nodes\":[{\"id\":\"start\",\"agentId\":\"1\"}]}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("triage"))
                .andReturn();

        Integer workflowId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(put("/api/workflows/" + workflowId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"description\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.description").value("updated"));

        mockMvc.perform(get("/api/workflows/" + workflowId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-002"))
                .andExpect(status().isNotFound());

        mockMvc.perform(delete("/api/workflows/" + workflowId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk());
    }

    @Test
    void healthTraceAndObservabilityEndpointsRespond() throws Exception {
        mockMvc.perform(get("/api/health"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.integrations.milvus").value(false));

        mockMvc.perform(get("/api/traces")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/observability/summary")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-001"));
    }

    @Test
    void evaluationRunPersistsGoldenDatasetResult() throws Exception {
        MvcResult agentResult = createAgent("eval-agent");
        String agentId = com.jayway.jsonpath.JsonPath.read(agentResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/evaluations/runs")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"empty-eval\",\"agentId\":\"" + agentId + "\",\"cases\":[]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.score").value(1.0));
    }

    @Test
    void botBindingAndWebhookCreateChannelScopedSession() throws Exception {
        MvcResult agentResult = createAgent("bot-agent");
        String agentId = com.jayway.jsonpath.JsonPath.read(agentResult.getResponse().getContentAsString(), "$.id");

        MvcResult bindingResult = mockMvc.perform(post("/api/bots/bindings")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"feishu\",\"channelBotId\":\"bot-a\",\"agentId\":\"" + agentId + "\",\"secret\":\"s1\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.channel").value("feishu"))
                .andReturn();

        Integer bindingId = com.jayway.jsonpath.JsonPath.read(bindingResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/bots/webhooks/feishu")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .header("X-Bot-Secret", "s1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"botId\":\"bot-a\",\"conversationId\":\"chat-1\",\"content\":{\"text\":\"hello\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bindingId").value(bindingId))
                .andExpect(jsonPath("$.sessionId").value("bot-" + bindingId + "-chat-1"));
    }

    private MvcResult createAgent(String name) throws Exception {
        return mockMvc.perform(post("/api/agents")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"" + name + "\",\"prompt\":\"hello\",\"model\":\"gpt-4o-mini\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
    }
}
