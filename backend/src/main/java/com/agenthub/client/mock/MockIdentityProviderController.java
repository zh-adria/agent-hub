package com.agenthub.client.mock;

import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.MockIdentityService;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/mock")
public class MockIdentityProviderController {
    private final MockIdentityService identityService;

    public MockIdentityProviderController(MockIdentityService identityService) {
        this.identityService = identityService;
    }

    @GetMapping("/.well-known/jwks.json")
    public Map<String, Object> jwks() {
        Map<String, Object> key = new LinkedHashMap<>();
        key.put("kty", "RSA");
        key.put("kid", "mock-key-001");
        key.put("use", "sig");
        key.put("alg", "RS256");
        key.put("n", "mock-modulus");
        key.put("e", "AQAB");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("keys", Collections.singletonList(key));
        return response;
    }

    @PostMapping("/oauth2/introspect")
    public Map<String, Object> introspect(@RequestBody Map<String, Object> request) {
        String token = stringValue(request.get("token"), "mock-token");
        return identityService.introspectionResponse(token);
    }

    @GetMapping("/userinfo")
    public Map<String, Object> userInfo(@RequestHeader(value = "Authorization", required = false) String authorization) {
        String token = bearerToken(authorization);
        AuthenticatedPrincipal principal = identityService.introspect(token != null ? token : "mock-token");
        return identityService.userInfo(principal, authorization != null && !authorization.trim().isEmpty());
    }

    @PostMapping("/rbac/authorize")
    public Map<String, Object> authorize(@RequestBody Map<String, Object> request) {
        String action = stringValue(request.get("action"), "");
        String token = stringValue(request.get("token"), "mock-token");
        String tenantId = stringValue(request.get("tenantId"), "tenant-001");
        AuthenticatedPrincipal principal = identityService.introspect(token);
        boolean allowed = identityService.authorize(principal, tenantId, action);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("allowed", allowed);
        response.put("reason", allowed ? "mock role grants " + action : "mock role denies " + action);
        return response;
    }

    @GetMapping("/rbac/users/{userId}/roles")
    public Map<String, Object> userRoles(@PathVariable String userId, @RequestParam(required = false) String tenantId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("tenantId", tenantId != null ? tenantId : "tenant-001");
        response.put("roles", Arrays.asList("agent_admin", "knowledge_editor"));
        return response;
    }

    @GetMapping("/rbac/roles/{roleCode}/permissions")
    public Map<String, Object> rolePermissions(@PathVariable String roleCode, @RequestParam(required = false) String tenantId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("role", roleCode);
        response.put("tenantId", tenantId != null ? tenantId : "tenant-001");
        response.put("permissions", identityService.permissionsFor(roleCode));
        return response;
    }

    @GetMapping("/tenants/{tenantId}")
    public Map<String, Object> tenant(@PathVariable String tenantId) {
        return identityService.tenant(tenantId);
    }

    @GetMapping("/users/{userId}/tenants")
    public Map<String, Object> userTenants(@PathVariable String userId) {
        AuthenticatedPrincipal principal = "user-002".equals(userId)
                ? identityService.introspect("tenant-002-token")
                : identityService.introspect("mock-token");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", userId);
        response.put("tenants", identityService.tenantSummaries(principal.getTenantIds()));
        return response;
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private String bearerToken(String authorization) {
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String value = authorization.trim();
        if (value.toLowerCase().startsWith("bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }
}
