package com.agenthub.client.auth;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public class AuthenticatedPrincipal {
    private final boolean active;
    private final String userId;
    private final String username;
    private final String tenantId;
    private final Set<String> tenantIds;
    private final Set<String> roles;
    private final Set<String> permissions;

    public AuthenticatedPrincipal(
            boolean active,
            String userId,
            String username,
            String tenantId,
            Set<String> tenantIds,
            Set<String> roles,
            Set<String> permissions) {
        this.active = active;
        this.userId = userId;
        this.username = username;
        this.tenantId = tenantId;
        this.tenantIds = immutableSet(tenantIds);
        this.roles = immutableSet(roles);
        this.permissions = immutableSet(permissions);
    }

    public boolean isActive() {
        return active;
    }

    public String getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getTenantId() {
        return tenantId;
    }

    public Set<String> getTenantIds() {
        return tenantIds;
    }

    public Set<String> getRoles() {
        return roles;
    }

    public Set<String> getPermissions() {
        return permissions;
    }

    public boolean hasTenant(String tenantId) {
        return tenantIds.contains(tenantId);
    }

    public boolean hasPermission(String permission) {
        return permissions.contains(permission);
    }

    private Set<String> immutableSet(Set<String> values) {
        return Collections.unmodifiableSet(new LinkedHashSet<>(values));
    }
}
