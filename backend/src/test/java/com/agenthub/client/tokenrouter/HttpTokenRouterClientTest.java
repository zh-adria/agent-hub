package com.agenthub.client.tokenrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.content;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class HttpTokenRouterClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void postsCompletionRequestAndConsumesAuditCostRouteFields() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TokenRouterProperties properties = new TokenRouterProperties();
        properties.setBaseUrl("http://token-router.test");
        HttpTokenRouterClient client = new HttpTokenRouterClient(
                restTemplate,
                properties,
                new TokenRouterResponseMapper(objectMapper),
                objectMapper
        );

        TokenRouterChatRequest request = new TokenRouterChatRequest();
        request.setBusinessTag("tenant-a");
        request.setModelHint("gpt-test");
        request.getMessages().add(new TokenRouterMessage("user", "hello"));
        request.getExtensions().put("agentId", "agent-1");

        server.expect(requestTo("http://token-router.test/api/chat/completions"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"businessTag\":\"tenant-a\",\"modelHint\":\"gpt-test\",\"messages\":[{\"role\":\"user\",\"content\":\"hello\"}],\"extensions\":{\"agentId\":\"agent-1\"}}"))
                .andRespond(withSuccess("{\"provider\":\"openai\",\"model\":\"gpt-4.1-mini\",\"content\":\"hi\",\"promptTokens\":3,\"completionTokens\":4,\"totalTokens\":7,\"cost\":0.0012,\"routeDecision\":\"primary\",\"routeReason\":\"healthy\"}", MediaType.APPLICATION_JSON));

        TokenRouterChatResponse response = client.complete(request);

        assertThat(response.getProvider()).isEqualTo("openai");
        assertThat(response.getModel()).isEqualTo("gpt-4.1-mini");
        assertThat(response.getContent()).isEqualTo("hi");
        assertThat(response.getTotalTokens()).isEqualTo(7);
        assertThat(response.getCost()).isEqualByComparingTo(new BigDecimal("0.0012"));
        assertThat(response.getRouteDecision()).isEqualTo("primary");
        assertThat(response.getRouteReason()).isEqualTo("healthy");
        server.verify();
    }

    @Test
    void mapsPolicyDenialToRuntimeFailureException() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TokenRouterProperties properties = new TokenRouterProperties();
        properties.setBaseUrl("http://token-router.test");
        HttpTokenRouterClient client = new HttpTokenRouterClient(
                restTemplate,
                properties,
                new TokenRouterResponseMapper(objectMapper),
                objectMapper
        );

        server.expect(requestTo("http://token-router.test/api/chat/completions"))
                .andRespond(withStatus(HttpStatus.FORBIDDEN).body("{\"error\":\"policy denied\"}"));

        assertThatThrownBy(() -> client.complete(new TokenRouterChatRequest()))
                .isInstanceOf(TokenRouterPolicyDeniedException.class)
                .hasMessageContaining("policy denied");
        server.verify();
    }

    @Test
    void postsStreamingCompletionRequestAndForwardsChunks() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        TokenRouterProperties properties = new TokenRouterProperties();
        properties.setBaseUrl("http://token-router.test");
        HttpTokenRouterClient client = new HttpTokenRouterClient(
                restTemplate,
                properties,
                new TokenRouterResponseMapper(objectMapper),
                objectMapper
        );

        TokenRouterChatRequest request = new TokenRouterChatRequest();
        request.setBusinessTag("tenant-a");
        request.getExtensions().put("agentSessionId", "session-1");
        List<String> chunks = new ArrayList<>();

        server.expect(requestTo("http://token-router.test/api/chat/completions/stream"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(content().json("{\"businessTag\":\"tenant-a\",\"stream\":true,\"extensions\":{\"agentSessionId\":\"session-1\"}}"))
                .andRespond(withSuccess("data: first\n\ndata: second\n", MediaType.TEXT_EVENT_STREAM));

        client.streamComplete(request, chunks::add);

        assertThat(chunks).containsExactly("data: first", "data: second");
        server.verify();
    }
}
