package com.agenthub.client.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.Base64;

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

        MvcResult functionResult = mockMvc.perform(post("/api/functions")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tenant-one-function\",\"endpoint\":\"http://127.0.0.1:1/test\",\"method\":\"GET\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String functionId = com.jayway.jsonpath.JsonPath.read(functionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/functions/" + functionId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-002"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        MvcResult sessionResult = mockMvc.perform(post("/api/sessions")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"" + agentId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        String sessionId = com.jayway.jsonpath.JsonPath.read(sessionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/sessions/" + sessionId)
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-002"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("RESOURCE_NOT_FOUND"));

        MvcResult knowledgeBaseResult = mockMvc.perform(post("/api/knowledge-bases")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"tenant-one-kb\",\"description\":\"private\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andReturn();
        Integer knowledgeBaseId = com.jayway.jsonpath.JsonPath.read(knowledgeBaseResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/knowledge-bases/" + knowledgeBaseId)
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

        mockMvc.perform(get("/api/mcp/readiness")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.ready").value(true))
                .andExpect(jsonPath("$.checks.parameterValidation").value(true));
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
                .andExpect(jsonPath("$.integrations.milvus").value(false))
                .andExpect(jsonPath("$.integrations.milvusCollectionStrategy").value("agenthub_t{tenantId}_kb{knowledgeBaseId}"))
                .andExpect(jsonPath("$.integrations.milvusPartitionStrategy").value("collection-per-tenant-knowledge-base"));

        mockMvc.perform(get("/api/traces")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/observability/summary")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value("tenant-001"));

        mockMvc.perform(get("/api/observability/production-readiness")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.mvpReady").value(true))
                .andExpect(jsonPath("$.productionReady").value(true));
    }

    @Test
    void ragDemoPackageCreatesIndustryKnowledgeBase() throws Exception {
        mockMvc.perform(post("/api/knowledge-bases/demo-packages/finance")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.industry").value("finance"))
                .andExpect(jsonPath("$.knowledgeBase.id").exists())
                .andExpect(jsonPath("$.document.accessTags", hasItem("finance")))
                .andExpect(jsonPath("$.chunks[0].accessTags", hasItem("finance")));
    }

    @Test
    void workflowFallbackProducesOutputWhenNodeFails() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/workflows")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"fallback-flow\",\"definition\":{\"nodes\":[{\"id\":\"rescue\",\"agentId\":\"999999\",\"fallback\":\"manual fallback\"}]}}"))
                .andExpect(status().isOk())
                .andReturn();

        Integer workflowId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/workflows/" + workflowId + "/execute")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"need fallback\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.outputs.rescue").value("manual fallback"))
                .andExpect(jsonPath("$.steps[0].stepKey").value("rescue#fallback"));
    }

    @Test
    void workflowApprovalCheckpointCanResume() throws Exception {
        MvcResult createResult = mockMvc.perform(post("/api/workflows")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"approval-flow\",\"definition\":{\"nodes\":[{\"id\":\"approve\",\"type\":\"human\"}]}}"))
                .andExpect(status().isOk())
                .andReturn();

        Integer workflowId = com.jayway.jsonpath.JsonPath.read(createResult.getResponse().getContentAsString(), "$.id");

        MvcResult executeResult = mockMvc.perform(post("/api/workflows/" + workflowId + "/execute")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"input\":\"needs approval\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("WAITING_APPROVAL"))
                .andExpect(jsonPath("$.checkpoint.waitingNodeId").value("approve"))
                .andReturn();

        com.jayway.jsonpath.JsonPath.read(executeResult.getResponse().getContentAsString(), "$.checkpoint");
        String checkpointJson = "{\"completedNodeIds\":[],\"waitingNodeId\":\"approve\",\"pendingNodeIds\":[\"approve\"]}";
        mockMvc.perform(post("/api/workflows/" + workflowId + "/resume")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"checkpoint\":" + checkpointJson + ",\"approvalInput\":\"approved by reviewer\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.outputs.approve").value("approved by reviewer"));
    }

    @Test
    void evaluationRunPersistsGoldenDatasetResult() throws Exception {
        MvcResult agentResult = createAgent("eval-agent");
        String agentId = com.jayway.jsonpath.JsonPath.read(agentResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/evaluations/metrics")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("contains"));

        mockMvc.perform(post("/api/evaluations/runs")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"metric-eval\",\"agentId\":\"" + agentId + "\",\"cases\":[{\"id\":\"case-1\",\"input\":\"hello\",\"expected\":\"Agent execution failed\",\"metrics\":[\"contains\"]}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("SUCCEEDED"))
                .andExpect(jsonPath("$.score").value(1.0))
                .andExpect(jsonPath("$.cases[0].passed").value(true));
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

        mockMvc.perform(post("/api/bots/webhooks/feishu")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .header("X-Lark-Request-Timestamp", "123456")
                        .header("X-Lark-Signature", feishuSignature("s1", "123456"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"botId\":\"bot-a\",\"conversationId\":\"chat-2\",\"messageId\":\"signed-1\",\"content\":{\"text\":\"signed hello\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bindingId").value(bindingId))
                .andExpect(jsonPath("$.sessionId").value("bot-" + bindingId + "-chat-2"));

        MvcResult wecomBindingResult = mockMvc.perform(post("/api/bots/bindings")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"channel\":\"wecom\",\"channelBotId\":\"wx-bot\",\"agentId\":\"" + agentId + "\",\"secret\":\"token-a\"}"))
                .andExpect(status().isOk())
                .andReturn();
        Integer wecomBindingId = com.jayway.jsonpath.JsonPath.read(wecomBindingResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/bots/webhooks/wecom")
                        .header("Authorization", "Bearer mock-token")
                        .header("X-Tenant-Id", "tenant-001")
                        .header("timestamp", "123456")
                        .header("nonce", "nonce-a")
                        .header("msg_signature", sha1Sorted("token-a", "123456", "nonce-a", "cipher-a"))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"botId\":\"wx-bot\",\"conversationId\":\"wx-chat\",\"messageId\":\"wx-1\",\"encrypt\":\"cipher-a\",\"content\":{\"text\":\"wecom hello\"}}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.bindingId").value(wecomBindingId))
                .andExpect(jsonPath("$.sessionId").value("bot-" + wecomBindingId + "-wx-chat"));
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

    private String feishuSignature(String secret, String timestamp) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec((timestamp + "\n" + secret).getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        return Base64.getEncoder().encodeToString(mac.doFinal(new byte[0]));
    }

    private String sha1Sorted(String... values) throws Exception {
        Arrays.sort(values);
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] bytes = digest.digest(String.join("", values).getBytes(StandardCharsets.UTF_8));
        StringBuilder hex = new StringBuilder();
        for (byte b : bytes) {
            hex.append(String.format("%02x", b));
        }
        return hex.toString();
    }
}
