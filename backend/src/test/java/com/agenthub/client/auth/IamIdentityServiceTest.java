package com.agenthub.client.auth;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class IamIdentityServiceTest {

    @Test
    void introspectUsesIamOAuthEndpointAndMapsPrincipal() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        IamIdentityService service = new IamIdentityService(restTemplate, properties());

        server.expect(requestTo("http://iam.test/iam/oauth/introspect"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Content-Type", containsString(MediaType.APPLICATION_FORM_URLENCODED_VALUE)))
                .andExpect(content().string(containsString("token=access-token")))
                .andExpect(content().string(containsString("client_id=agent-hub")))
                .andExpect(content().string(containsString("client_secret=secret")))
                .andRespond(withSuccess("{\"active\":true,\"uid\":42,\"sub\":\"alice\",\"tenant\":\"tenant-001\",\"roles\":[\"ROLE_AGENT\"],\"perms\":[\"agent:read\"],\"scope\":\"session:message\"}", MediaType.APPLICATION_JSON));

        AuthenticatedPrincipal principal = service.introspect("access-token");

        assertThat(principal.isActive()).isTrue();
        assertThat(principal.getUserId()).isEqualTo("42");
        assertThat(principal.getUsername()).isEqualTo("alice");
        assertThat(principal.getTenantId()).isEqualTo("tenant-001");
        assertThat(principal.getRoles()).containsExactly("ROLE_AGENT");
        assertThat(principal.getPermissions()).contains("agent:read", "session:message");
        assertThat(principal.getAccessToken()).isEqualTo("access-token");
        server.verify();
    }

    @Test
    void authorizeDelegatesToIamAuthzCheckWhenPermissionMissingLocally() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        IamIdentityService service = new IamIdentityService(restTemplate, properties());
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                true,
                "42",
                "alice",
                "tenant-001",
                Collections.singleton("tenant-001"),
                Collections.singleton("ROLE_AGENT"),
                Collections.emptySet(),
                "access-token");

        server.expect(requestTo("http://iam.test/iam/api/authz/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andExpect(content().string(containsString("\"permission\":\"agent:create\"")))
                .andExpect(content().string(containsString("\"tenantCode\":\"tenant-001\"")))
                .andRespond(withSuccess("{\"success\":true,\"code\":\"OK\",\"data\":{\"allowed\":true,\"permission\":\"agent:create\",\"tenant\":\"tenant-001\"}}", MediaType.APPLICATION_JSON));

        assertThat(service.authorize(principal, "tenant-001", "agent:create")).isTrue();
        server.verify();
    }

    @Test
    void authorizeHonorsIamDenialEvenWhenPermissionExistsLocally() {
        RestTemplate restTemplate = new RestTemplate();
        MockRestServiceServer server = MockRestServiceServer.bindTo(restTemplate).build();
        IamIdentityService service = new IamIdentityService(restTemplate, properties());
        AuthenticatedPrincipal principal = new AuthenticatedPrincipal(
                true,
                "42",
                "alice",
                "tenant-001",
                Collections.singleton("tenant-001"),
                Collections.singleton("ROLE_AGENT"),
                Collections.singleton("agent:delete"),
                "access-token");

        server.expect(requestTo("http://iam.test/iam/api/authz/check"))
                .andExpect(method(HttpMethod.POST))
                .andExpect(header("Authorization", "Bearer access-token"))
                .andRespond(withSuccess("{\"success\":true,\"code\":\"OK\",\"data\":{\"allowed\":false,\"permission\":\"agent:delete\",\"tenant\":\"tenant-001\"}}", MediaType.APPLICATION_JSON));

        assertThat(service.authorize(principal, "tenant-001", "agent:delete")).isFalse();
        server.verify();
    }

    private IamIdentityProperties properties() {
        IamIdentityProperties properties = new IamIdentityProperties();
        properties.setBaseUrl("http://iam.test/iam");
        properties.setClientId("agent-hub");
        properties.setClientSecret("secret");
        return properties;
    }
}
