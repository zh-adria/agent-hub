package com.agenthub.client.impl;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.port.LLMClient;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.List;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasItem;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class MainFlowE2ETest {
    private static final String AUTH = "Bearer mock-token";
    private static final String TENANT = "tenant-001";

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LLMClient llmClient;

    @Test
    void agentFunctionSessionMainFlowPersistsMessagesAndTrace() throws Exception {
        Mockito.when(llmClient.reason(any(Agent.class), any(List.class))).thenReturn("READY");
        Mockito.when(llmClient.generateFinalAnswer(any(Agent.class), any(List.class))).thenReturn("hello from agent");

        MvcResult functionResult = mockMvc.perform(post("/api/functions")
                        .headers(httpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"e2e_lookup\",\"description\":\"lookup\",\"endpoint\":\"http://127.0.0.1:1/mock\",\"method\":\"POST\",\"parameters\":[{\"name\":\"q\",\"type\":\"string\",\"required\":true}]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.name").value("e2e_lookup"))
                .andReturn();
        String functionId = JsonPath.read(functionResult.getResponse().getContentAsString(), "$.id");

        MvcResult agentResult = mockMvc.perform(post("/api/agents")
                        .headers(httpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"e2e-agent\",\"prompt\":\"answer briefly\",\"model\":\"deepseek-v4-flash\",\"temperature\":0.2,\"functionIds\":[\"" + functionId + "\"]}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.functionIds", hasItem(functionId)))
                .andReturn();
        String agentId = JsonPath.read(agentResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(get("/api/agents/" + agentId + "/functions")
                        .headers(httpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value(functionId));

        MvcResult sessionResult = mockMvc.perform(post("/api/sessions")
                        .headers(httpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"agentId\":\"" + agentId + "\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.agentId").value(agentId))
                .andReturn();
        String sessionId = JsonPath.read(sessionResult.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/sessions/" + sessionId + "/messages")
                        .headers(httpHeaders())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"role\":\"user\",\"content\":\"hello\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.role").value("assistant"))
                .andExpect(jsonPath("$.content").value("hello from agent"));

        mockMvc.perform(get("/api/sessions/" + sessionId + "/messages")
                        .headers(httpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(1)))
                .andExpect(jsonPath("$[0].role").value("user"))
                .andExpect(jsonPath("$[1].role").value("assistant"));

        mockMvc.perform(get("/api/traces")
                        .headers(httpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()", greaterThan(0)));

        mockMvc.perform(get("/api/observability/summary")
                        .headers(httpHeaders()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tenantId").value(TENANT))
                .andExpect(jsonPath("$.traceCount", greaterThan(0)))
                .andExpect(jsonPath("$.stepRecordCount", greaterThan(0)));
    }

    private org.springframework.http.HttpHeaders httpHeaders() {
        org.springframework.http.HttpHeaders headers = new org.springframework.http.HttpHeaders();
        headers.add("Authorization", AUTH);
        headers.add("X-Tenant-Id", TENANT);
        return headers;
    }
}
