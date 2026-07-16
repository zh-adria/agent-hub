package com.agenthub.client.api;

import com.agenthub.client.auth.AuthContext;
import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.IamIdentityProperties;
import com.agenthub.client.auth.IdentityService;
import com.agenthub.client.auth.UnauthorizedException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {
    private final RestTemplate restTemplate;
    private final IamIdentityProperties properties;
    private final IdentityService identityService;
    private final String identityProvider;

    public AuthApi(
            IamIdentityProperties properties,
            IdentityService identityService,
            @Value("${agenthub.identity.provider:iam}") String identityProvider) {
        this.restTemplate = new RestTemplate();
        this.properties = properties;
        this.identityService = identityService;
        this.identityProvider = identityProvider;
    }

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        if ("mock".equalsIgnoreCase(identityProvider)) {
            return mockLogin(body);
        }
        Map<String, Object> request = new LinkedHashMap<>(body);
        request.putIfAbsent("clientId", properties.getClientId());
        request.putIfAbsent("grantType", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.loginUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(request, headers),
                    Map.class);
            return unwrapApiResult(response.getBody());
        } catch (HttpStatusCodeException ex) {
            throw new UnauthorizedException("IAM login failed: " + ex.getStatusCode());
        } catch (RestClientException ex) {
            throw new UnauthorizedException("IAM login unavailable");
        }
    }

    @GetMapping("/me")
    public Map<String, Object> me(@RequestHeader(value = "Authorization", required = false) String authorization) {
        AuthenticatedPrincipal principal = AuthContext.principal();
        if (principal == null || !principal.isActive()) {
            throw new UnauthorizedException("Missing authenticated principal");
        }
        boolean authorizationPresent = authorization != null && !authorization.trim().isEmpty();
        return identityService.userInfo(principal, authorizationPresent);
    }

    @PostMapping("/logout")
    public Map<String, Object> logout(@RequestHeader(value = "Authorization", required = false) String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            throw new UnauthorizedException("Missing bearer token");
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(authorization.replaceFirst("(?i)^bearer\\s+", "").trim());
        try {
            restTemplate.exchange(properties.logoutUrl(), HttpMethod.POST, new HttpEntity<>(null, headers), Map.class);
        } catch (RestClientException ignored) {
            // Local logout still clears the browser token even if IAM is temporarily unavailable.
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        return response;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> unwrapApiResult(Map<?, ?> body) {
        if (body == null) {
            throw new UnauthorizedException("Empty IAM login response");
        }
        Object success = body.get("success");
        if (Boolean.FALSE.equals(success)) {
            Object message = body.get("message");
            throw new UnauthorizedException(message == null ? "IAM login failed" : String.valueOf(message));
        }
        Object data = body.get("data");
        if (data instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) data);
        }
        return new LinkedHashMap<>((Map<String, Object>) body);
    }

    private Map<String, Object> mockLogin(Map<String, Object> body) {
        String tenantCode = text(body.get("tenantCode"), "tenant-001");
        String username = text(body.get("username"), "admin");
        String token = "tenant-002".equals(tenantCode) ? "tenant-002-token" : "mock-token";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", token);
        response.put("tokenType", "Bearer");
        response.put("tenantId", tenantCode);
        response.put("username", username);
        return response;
    }

    private String text(Object value, String fallback) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
