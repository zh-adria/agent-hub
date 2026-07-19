package com.agenthub.client.api;

import com.agenthub.client.auth.AuthContext;
import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.IamIdentityProperties;
import com.agenthub.client.auth.IdentityService;
import com.agenthub.client.auth.LogtoIdentityProperties;
import com.agenthub.client.auth.LogtoOrganizationRoleMapper;
import com.agenthub.client.auth.UnauthorizedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {
    private static final Logger log = LoggerFactory.getLogger(AuthApi.class);

    private final RestTemplate restTemplate;
    private final IamIdentityProperties iamProperties;
    private final IdentityService identityService;
    private final String identityProvider;

    // Logto-specific: optional, only present when provider=logto
    @Autowired(required = false)
    private LogtoIdentityProperties logtoProperties;

    @Autowired(required = false)
    private LogtoOrganizationRoleMapper roleMapper;

    public AuthApi(
            IamIdentityProperties properties,
            IdentityService identityService,
            @Value("${agenthub.identity.provider:iam}") String identityProvider) {
        this.restTemplate = new RestTemplate();
        this.iamProperties = properties;
        this.identityService = identityService;
        this.identityProvider = identityProvider;
    }

    // ------------------------------------------------------------------
    // Login (provider-aware)
    // ------------------------------------------------------------------

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        if ("mock".equalsIgnoreCase(identityProvider)) {
            return mockLogin(body);
        }
        if ("logto".equalsIgnoreCase(identityProvider)) {
            return logtoLogin(body);
        }
        Map<String, Object> request = new LinkedHashMap<>(body);
        request.putIfAbsent("clientId", iamProperties.getClientId());
        request.putIfAbsent("grantType", "password");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    iamProperties.loginUrl(),
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

    // ------------------------------------------------------------------
    // Logto OAuth2 endpoints (no-op if Logto not configured)
    // ------------------------------------------------------------------

    @GetMapping("/logto/authorize")
    public Map<String, Object> logtoAuthorize(
            @RequestParam(required = false) String redirectUri,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String organizationId) {
        ensureLogtoProvider();

        String baseAuthUrl = logtoProperties.getIssuer() + "/oidc/auth";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", logtoProperties.getClientId());
        params.put("scope", "openid profile email organization");
        if (redirectUri != null && !redirectUri.isBlank()) params.put("redirect_uri", redirectUri);
        if (state != null && !state.isBlank()) params.put("state", state);
        if (organizationId != null && !organizationId.isBlank()) {
            params.put("organization_id", organizationId);
        }

        String authorizeUrl = buildUrl(baseAuthUrl, params);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authorizeUrl", authorizeUrl);
        response.put("redirectUri", redirectUri);
        response.put("state", state);
        return response;
    }

    @GetMapping("/logto/callback")
    public ResponseEntity<Map<String, Object>> logtoCallback(
            @RequestParam String code,
            @RequestParam(required = false) String state,
            @RequestParam(required = false) String error,
            @RequestParam(required = false) String errorDescription) {
        if (!"logto".equalsIgnoreCase(identityProvider)) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "Logto provider is not configured"));
        }
        if (error != null) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", error, "error_description", errorDescription));
        }
        if (code == null || code.isBlank()) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(Map.of("error", "missing_authorization_code"));
        }

        try {
            Map<String, Object> tokens = exchangeCodeForToken(code);
            return ResponseEntity.ok(tokens);
        } catch (HttpStatusCodeException ex) {
            log.error("Logto token exchange failed [{}]: {}", ex.getStatusCode(), ex.getResponseBodyAsString());
            return ResponseEntity.status(ex.getStatusCode())
                    .body(Map.of("error", "token_exchange_failed",
                            "details", ex.getResponseBodyAsString()));
        } catch (RestClientException ex) {
            log.error("Logto token exchange error", ex);
            return ResponseEntity.status(HttpStatus.BAD_GATEWAY)
                    .body(Map.of("error", "logto_unavailable", "message", ex.getMessage()));
        }
    }

    @PostMapping("/logto/permissions")
    public Map<String, Object> logtoPermissions(@RequestBody Map<String, Object> body) {
        ensureLogtoProvider();
        String userId = text(body.get("userId"));
        String organizationId = text(body.get("organizationId"));
        if (userId == null || organizationId == null) {
            throw new UnauthorizedException("userId and organizationId are required");
        }
        Set<String> permissions = roleMapper.getPermissions(organizationId, userId);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("userId", userId);
        result.put("tenantId", organizationId);
        result.put("permissions", new ArrayList<>(permissions));
        return result;
    }

    // ------------------------------------------------------------------
    // Generic endpoints
    // ------------------------------------------------------------------

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
        String cleanToken = authorization.replaceFirst("(?i)^bearer\\s+", "").trim();
        if ("logto".equalsIgnoreCase(identityProvider) && logtoProperties != null) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
                MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
                form.add("token", cleanToken);
                form.add("token_type_hint", "access_token");
                form.add("client_id", logtoProperties.getClientId());
                form.add("client_secret", logtoProperties.getClientSecret());
                restTemplate.exchange(
                        logtoProperties.getIssuer() + "/oidc/revoke",
                        HttpMethod.POST,
                        new HttpEntity<>(form, headers),
                        Map.class);
            } catch (RestClientException ignored) {
                // best-effort logout
            }
        } else {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(cleanToken);
            try {
                restTemplate.exchange(
                        iamProperties.logoutUrl(), HttpMethod.POST,
                        new HttpEntity<>(null, headers), Map.class);
            } catch (RestClientException ignored) {
                // best-effort
            }
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        return response;
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private Map<String, Object> logtoLogin(Map<String, Object> body) {
        String redirectUri = text(body.get("redirectUri"));
        String state = text(body.get("state"));
        String organizationId = text(body.get("organizationId"));
        String prompt = text(body.get("prompt"), "login");

        String baseAuthUrl = logtoProperties.getIssuer() + "/oidc/auth";
        Map<String, String> params = new LinkedHashMap<>();
        params.put("response_type", "code");
        params.put("client_id", logtoProperties.getClientId());
        params.put("scope", "openid profile email organization");
        params.put("prompt", prompt);
        if (redirectUri != null) params.put("redirect_uri", redirectUri);
        if (state != null) params.put("state", state);
        if (organizationId != null) params.put("organization_id", organizationId);

        String authorizeUrl = buildUrl(baseAuthUrl, params);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("authorizeUrl", authorizeUrl);
        response.put("provider", "logto");
        return response;
    }

    private Map<String, Object> exchangeCodeForToken(String code) {
        String tokenUrl = logtoProperties.getIssuer() + "/oidc/token";

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("grant_type", "authorization_code");
        form.add("code", code);
        form.add("client_id", logtoProperties.getClientId());
        form.add("client_secret", logtoProperties.getClientSecret());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        ResponseEntity<Map> response = restTemplate.exchange(
                tokenUrl,
                HttpMethod.POST,
                new HttpEntity<>(form, headers),
                Map.class);

        Map<?, ?> body = response.getBody();
        if (body == null || !Boolean.TRUE.equals(body.get("success"))) {
            throw new UnauthorizedException("Logto token exchange returned unsuccessful response");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> data = (Map<String, Object>) body.get("data");
        if (data == null) {
            throw new UnauthorizedException("Logto token response missing data");
        }
        return data;
    }

    private void ensureLogtoProvider() {
        if (!"logto".equalsIgnoreCase(identityProvider) || logtoProperties == null) {
            throw new UnauthorizedException("Logto provider is not configured");
        }
    }

    private static String buildUrl(String baseUrl, Map<String, String> params) {
        StringBuilder sb = new StringBuilder(baseUrl);
        boolean first = baseUrl.contains("?");
        for (Map.Entry<String, String> entry : params.entrySet()) {
            if (entry.getValue() == null || entry.getValue().isBlank()) continue;
            sb.append(first ? '?' : '&');
            sb.append(URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8));
            sb.append('=');
            sb.append(URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8));
            first = false;
        }
        return sb.toString();
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

    private String text(Object value) {
        return text(value, null);
    }
}
