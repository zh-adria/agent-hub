package com.agenthub.client.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Logto Cloud 集成配置属性。
 *
 * <p>激活方式：设置 {@code agenthub.identity.provider=logto}。
 *
 * <h3>Logto Cloud 中的前置配置</h3>
 * <ol>
 *   <li>在 Logto Cloud 创建一个 <b>Machine-to-Machine (M2M)</b> Application，
 *       授予 {@code Organization Roles Management} API scope。</li>
 *   <li>记录该 M2M Application 的 {@code clientId} 和 {@code clientSecret}
 *       （即 {@code clientId}/{@code clientSecret} 字段）。</li>
 *   <li>在 Logto Cloud 中创建 Organization 对应每个 AgentHub tenant，
 *       为 Organization 配置 Roles 并分配 Scopes（Scope 即 AgentHub permission）。</li>
 *   <li>将用户加入对应 Organization 并赋予角色。</li>
 * </ol>
 */
@Component
@ConfigurationProperties(prefix = "agenthub.identity.logto")
public class LogtoIdentityProperties {

    public LogtoIdentityProperties() {
    }

    /**
     * Logto Cloud OIDC issuer URL，例如：
     * {@code https://your-tenant.logto.app}
     * <p>
     * 该 URL 用于构造 OIDC discovery 路径：{issuer}/.well-known/openid-configuration
     */
    private String issuer = "https://localhost:3001";

    /**
     * Logto Cloud Machine-to-Machine Application 的 Client ID。
     * 用于调用 Organization Roles Management API 拉取用户 RBAC 数据。
     */
    private String clientId = "";

    /**
     * Logto Cloud M2M Application 的 Client Secret。
     */
    private String clientSecret = "";

    /**
     * Organization Roles API 路径，默认使用 Logto v1 API。
     * 最终请求 URL = baseUrl + organizationRolesPath
     */
    private String organizationRolesPath = "/api/organization-roles";

    /**
     * Logto Management API base URL。
     * 默认与 issuer 同域；若使用 Logto 私有部署或网关代理可改写。
     */
    private String managementApiBaseUrl = "";

    /**
     * OIDC JWKS endpoint 缓存刷新间隔（毫秒）。
     * 默认 1 小时，期间 key rollover 不影响已有签名校验。
     */
    private long jwksCacheTtlMs = 3_600_000L;

    /**
     * JWT 验证时允许的时钟偏差（秒）。
     * 用于补偿服务端时钟漂移。
     */
    private int clockSkewSeconds = 30;

    // ------------------------------------------------------------------
    // derived URLs
    // ------------------------------------------------------------------

    public String getJwksUri() {
        return issuer + "/oidc/jwks";
    }

    public String getOidcConfigurationUri() {
        return issuer + "/.well-known/openid-configuration";
    }

    public String getUserinfoUri() {
        return issuer + "/oidc/userinfo";
    }

    public String getIntrospectUri() {
        return issuer + "/oidc/introspect";
    }

    public String getManagementApiBaseUrl() {
        if (managementApiBaseUrl == null || managementApiBaseUrl.isBlank()) {
            return issuer;
        }
        String trimmed = managementApiBaseUrl.endsWith("/")
                ? managementApiBaseUrl.substring(0, managementApiBaseUrl.length() - 1)
                : managementApiBaseUrl;
        return trimmed;
    }

    public String getOrganizationRolesUrl(String organizationId, String userId) {
        return getManagementApiBaseUrl()
                + organizationRolesPath
                + "?organizationId=" + encode(organizationId)
                + "&userId=" + encode(userId);
    }

    public String getM2MTokenUrl() {
        return getManagementApiBaseUrl() + "/oidc/token";
    }

    // ------------------------------------------------------------------
    // getters / setters
    // ------------------------------------------------------------------

    public String getIssuer() {
        return issuer;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public String getClientId() {
        return clientId;
    }

    public void setClientId(String clientId) {
        this.clientId = clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public void setClientSecret(String clientSecret) {
        this.clientSecret = clientSecret;
    }

    public String getOrganizationRolesPath() {
        return organizationRolesPath;
    }

    public void setOrganizationRolesPath(String organizationRolesPath) {
        this.organizationRolesPath = organizationRolesPath;
    }

    public String getManagementApiBaseUrlRaw() {
        return managementApiBaseUrl;
    }

    public void setManagementApiBaseUrl(String managementApiBaseUrl) {
        this.managementApiBaseUrl = managementApiBaseUrl;
    }

    public long getJwksCacheTtlMs() {
        return jwksCacheTtlMs;
    }

    public void setJwksCacheTtlMs(long jwksCacheTtlMs) {
        this.jwksCacheTtlMs = jwksCacheTtlMs;
    }

    public int getClockSkewSeconds() {
        return clockSkewSeconds;
    }

    public void setClockSkewSeconds(int clockSkewSeconds) {
        this.clockSkewSeconds = clockSkewSeconds;
    }

    private String encode(String value) {
        return java.net.URLEncoder.encode(value, java.nio.charset.StandardCharsets.UTF_8);
    }
}
