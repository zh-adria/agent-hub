package com.agenthub.client.auth;

import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class MockIdentityService {

    public AuthenticatedPrincipal introspect(String token) {
        String normalized = token == null ? "" : token.trim();
        if (normalized.isEmpty() || "expired".equalsIgnoreCase(normalized)) {
            return principal(false, "anonymous", "anonymous", "tenant-001",
                    set("tenant-001"), set(), set());
        }
        if ("reader-token".equalsIgnoreCase(normalized)) {
            return principal(true, "user-reader", "reader", "tenant-001",
                    set("tenant-001"), set("agent_reader"), readPermissions());
        }
        if ("knowledge-token".equalsIgnoreCase(normalized)) {
            return principal(true, "user-knowledge", "knowledge-editor", "tenant-001",
                    set("tenant-001"), set("knowledge_editor"), permissionsFor("knowledge_editor"));
        }
        if ("tenant-002-token".equalsIgnoreCase(normalized)) {
            return principal(true, "user-002", "tenant-two-admin", "tenant-002",
                    set("tenant-002"), set("agent_admin", "knowledge_editor"), adminPermissions());
        }
        return principal(true, "user-001", "admin", "tenant-001",
                set("tenant-001", "tenant-002"), set("agent_admin", "knowledge_editor"), adminPermissions());
    }

    public boolean authorize(AuthenticatedPrincipal principal, String tenantId, String action) {
        return principal != null
                && principal.isActive()
                && principal.hasTenant(tenantId)
                && principal.hasPermission(action);
    }

    public Map<String, Object> introspectionResponse(String token) {
        AuthenticatedPrincipal principal = introspect(token);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", principal.isActive());
        response.put("sub", principal.getUserId());
        response.put("userId", principal.getUserId());
        response.put("tenantId", principal.getTenantId());
        response.put("username", principal.getUsername());
        response.put("roles", new ArrayList<>(principal.getRoles()));
        response.put("permissions", new ArrayList<>(principal.getPermissions()));
        response.put("tenants", tenantSummaries(principal.getTenantIds()));
        return response;
    }

    public Map<String, Object> userInfo(AuthenticatedPrincipal principal, boolean authorizationPresent) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", principal.getUserId());
        response.put("tenantId", principal.getTenantId());
        response.put("username", principal.getUsername());
        response.put("displayName", displayName(principal.getUserId()));
        response.put("email", principal.getUsername() + "@example.com");
        response.put("authorizationPresent", authorizationPresent);
        return response;
    }

    public Map<String, Object> tenant(String tenantId) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", tenantId);
        response.put("name", "tenant-002".equals(tenantId) ? "Test Organization" : "Example Company");
        response.put("status", "ACTIVE");
        response.put("features", Arrays.asList("agent", "rag", "session"));
        return response;
    }

    public List<Map<String, Object>> tenantSummaries(Set<String> tenantIds) {
        List<Map<String, Object>> tenants = new ArrayList<>();
        for (String tenantId : tenantIds) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tenantId", tenantId);
            item.put("name", "tenant-002".equals(tenantId) ? "Test Organization" : "Example Company");
            tenants.add(item);
        }
        return tenants;
    }

    public Set<String> permissionsFor(String roleCode) {
        if ("knowledge_editor".equals(roleCode)) {
            return set("knowledge:create", "knowledge:read", "knowledge:update", "knowledge:delete", "knowledge:search");
        }
        if ("agent_reader".equals(roleCode)) {
            return readPermissions();
        }
        return adminPermissions();
    }

    private AuthenticatedPrincipal principal(
            boolean active,
            String userId,
            String username,
            String tenantId,
            Set<String> tenantIds,
            Set<String> roles,
            Set<String> permissions) {
        return new AuthenticatedPrincipal(active, userId, username, tenantId, tenantIds, roles, permissions);
    }

    private Set<String> adminPermissions() {
        return set(
                "agent:create", "agent:read", "agent:update", "agent:delete",
                "function:create", "function:read", "function:update", "function:delete", "function:invoke",
                "session:create", "session:read", "session:message", "session:delete",
                "knowledge:create", "knowledge:read", "knowledge:update", "knowledge:delete", "knowledge:search",
                "workflow:create", "workflow:read", "workflow:update", "workflow:delete", "workflow:execute",
                "evaluation:read", "evaluation:run",
                "bot:create", "bot:read", "bot:update", "bot:delete",
                "trace:read",
                "audit:read"
        );
    }

    private Set<String> readPermissions() {
        return set("agent:read", "function:read", "session:read", "knowledge:read", "workflow:read", "evaluation:read", "bot:read", "trace:read", "audit:read");
    }

    private Set<String> set(String... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }

    private String displayName(String userId) {
        if ("user-reader".equals(userId)) return "Read Only User";
        if ("user-knowledge".equals(userId)) return "Knowledge Editor";
        if ("user-002".equals(userId)) return "Tenant Two Admin";
        return "System Admin";
    }
}
