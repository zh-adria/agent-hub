package com.agenthub.domain.context;

public final class TenantContext {
    private static final ThreadLocal<Long> TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> EXTERNAL_TENANT_ID = new ThreadLocal<>();
    private static final ThreadLocal<String> USER_ID = new ThreadLocal<>();

    private TenantContext() {
    }

    public static void set(Long tenantId, String externalTenantId, String userId) {
        TENANT_ID.set(tenantId);
        EXTERNAL_TENANT_ID.set(externalTenantId);
        USER_ID.set(userId);
    }

    public static Long tenantId() {
        Long tenantId = TENANT_ID.get();
        return tenantId != null ? tenantId : 1L;
    }

    public static String externalTenantId() {
        String tenantId = EXTERNAL_TENANT_ID.get();
        return tenantId != null ? tenantId : "tenant-001";
    }

    public static String userId() {
        String userId = USER_ID.get();
        return userId != null ? userId : "user-001";
    }

    public static void clear() {
        TENANT_ID.remove();
        EXTERNAL_TENANT_ID.remove();
        USER_ID.remove();
    }
}
