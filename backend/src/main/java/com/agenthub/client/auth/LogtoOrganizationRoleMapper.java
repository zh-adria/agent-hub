package com.agenthub.client.auth;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * Logto Cloud Organization Roles API 调用器。
 *
 * <p>职责：
 * <ol>
 *   <li>使用 Logto M2M Application 的 {@code client_credentials} 流程获取 management access token</li>
 *   <li>调用 Logto Management API 的
 *       {@code GET /api/organization-roles?organizationId=xxx&userId=yyy}
 *       拉取用户在指定 Organization 下的所有 Organization Roles</li>
 *   <li>将每个 Organization Role 的 {@code scopes} 聚合为 AgentHub permission 集合</li>
 * </ol>
 *
 * <p>缓存策略：对 (organizationId, userId) → permissions 的查询结果做短时缓存，
 * 减少对 Logto API 的调用频率。缓存过期后自动刷新。
 *
 * <p><b>Logto Cloud 前置配置</b>：
 * <pre>
 *   1. 创建 M2M Application，API Resource 选 "Organization Roles Management"
 *   2. 记录 clientId 和 clientSecret → agenthub.identity.logto.client-id / client-secret
 *   3. 在 Organization 中创建 Roles（如 agent_admin）并配置 Scopes
 *   4. 将用户加入 Organization 并赋予对应 Role
 * </pre>
 */
@Component
@ConditionalOnProperty(prefix = "agenthub.identity", name = "provider", havingValue = "logto")
public class LogtoOrganizationRoleMapper {

    private static final Logger log = LoggerFactory.getLogger(LogtoOrganizationRoleMapper.class);

    private final RestTemplate restTemplate;
    private final LogtoIdentityProperties properties;

    /** (organizationId:userId) → permissions 缓存 */
    private final Map<String, CachedPermissions> permissionCache = new ConcurrentHashMap<>();

    /** M2M access token 及其过期时间 */
    private volatile String m2mAccessToken;
    private volatile long m2mTokenExpiresAt = 0;

    public LogtoOrganizationRoleMapper(LogtoIdentityProperties properties) {
        this(new RestTemplate(), properties);
    }

    LogtoOrganizationRoleMapper(RestTemplate restTemplate, LogtoIdentityProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    /**
     * 获取用户在指定 Organization 中的权限集合。
     *
     * <p>调用链：先取 M2M token → 调用 Organization Roles API → 聚合 scopes。
     *
     * @param organizationId Logto Organization ID（= AgentHub tenantId）
     * @param userId         Logto user ID（= AgentHub userId）
     * @return 该用户在该组织中的所有 scopes（即 AgentHub permissions），空集合表示无权限
     */
    public Set<String> getPermissions(String organizationId, String userId) {
        if (organizationId == null || organizationId.isBlank()
                || userId == null || userId.isBlank()) {
            return Collections.emptySet();
        }

        String cacheKey = organizationId + ":" + userId;
        CachedPermissions cached = permissionCache.get(cacheKey);
        if (cached != null && !cached.isExpired()) {
            return cached.permissions;
        }

        Set<String> permissions = fetchPermissionsFromLogto(organizationId, userId);
        long ttlMs = TimeUnit.MINUTES.toMillis(5);
        permissionCache.put(cacheKey, new CachedPermissions(permissions, ttlMs));
        return Collections.unmodifiableSet(permissions);
    }

    /**
     * 使权限缓存失效，用于用户角色变更后主动刷新。
     */
    public void invalidateCache(String organizationId, String userId) {
        if (organizationId != null) {
            permissionCache.keySet().removeIf(k -> k.startsWith(organizationId + ":"));
        }
        if (userId != null) {
            permissionCache.keySet().removeIf(k -> k.endsWith(":" + userId));
        }
    }

    /**
     * 刷新全部权限缓存（例如定时全量刷新）。
     */
    public void refreshAll() {
        permissionCache.clear();
    }

    // ------------------------------------------------------------------
    // M2M token management
    // ------------------------------------------------------------------

    private String getM2MAccessToken() {
        long now = System.currentTimeMillis();
        if (m2mAccessToken != null && now < m2mTokenExpiresAt - 30_000) {
            return m2mAccessToken;
        }
        return fetchM2MToken();
    }

    private synchronized String fetchM2MToken() {
        long now = System.currentTimeMillis();
        if (m2mAccessToken != null && now < m2mTokenExpiresAt - 30_000) {
            return m2mAccessToken;
        }

        String tokenUrl = properties.getM2MTokenUrl();
        log.debug("Fetching Logto M2M access token from {}", tokenUrl);

        Map<String, String> body = new LinkedHashMap<>();
        body.put("grant_type", "client_credentials");
        body.put("client_id", properties.getClientId());
        body.put("client_secret", properties.getClientSecret());
        body.put("resource", properties.getManagementApiBaseUrl() + properties.getOrganizationRolesPath());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        body.forEach((k, v) -> form.add(k, v));

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    tokenUrl,
                    HttpMethod.POST,
                    new HttpEntity<>(form, headers),
                    Map.class);
        } catch (RestClientException ex) {
            log.error("Failed to obtain Logto M2M token from {}: {}", tokenUrl, ex.getMessage());
            throw new IllegalStateException("Cannot obtain Logto M2M access token", ex);
        }

        Map<?, ?> bodyResp = response.getBody();
        if (bodyResp == null || !Boolean.TRUE.equals(bodyResp.get("success"))) {
            String msg = bodyResp != null ? String.valueOf(bodyResp.get("error_description") != null
                    ? bodyResp.get("error_description") : bodyResp.get("error")) : "empty response";
            throw new IllegalStateException("Logto M2M token request failed: " + msg);
        }

        Map<?, ?> data = (Map<?, ?>) bodyResp.get("data");
        if (data == null) {
            throw new IllegalStateException("Logto M2M token response missing 'data'");
        }

        String token = Objects.toString(data.get("access_token"), null);
        if (token == null || token.isBlank()) {
            throw new IllegalStateException("Logto M2M token response missing 'access_token'");
        }

        // compute expiry (default 1h if not provided)
        long expiresIn = 3600;
        Object expiresInObj = data.get("expires_in");
        if (expiresInObj instanceof Number) {
            expiresIn = ((Number) expiresInObj).longValue();
        } else if (expiresInObj instanceof String) {
            try {
                expiresIn = Long.parseLong((String) expiresInObj);
            } catch (NumberFormatException ignored) {
            }
        }

        m2mAccessToken = token;
        m2mTokenExpiresAt = System.currentTimeMillis() + expiresIn * 1000;
        log.debug("Logto M2M token obtained, expires in {}s", expiresIn);

        return token;
    }

    // ------------------------------------------------------------------
    // Organization Roles API
    // ------------------------------------------------------------------

    private Set<String> fetchPermissionsFromLogto(String organizationId, String userId) {
        String token = getM2MAccessToken();
        String url = properties.getOrganizationRolesUrl(organizationId, userId);
        log.debug("Fetching Logto organization roles: org={}, user={}", organizationId, userId);

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        ResponseEntity<Map> response;
        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.GET,
                    new HttpEntity<>(headers),
                    Map.class);
        } catch (RestClientException ex) {
            log.warn("Logto organization roles API call failed for org={}, user={}: {}",
                    organizationId, userId, ex.getMessage());
            return Collections.emptySet();
        }

        Map<?, ?> body = response.getBody();
        if (body == null) {
            return Collections.emptySet();
        }

        // Logto v1 API response shape:
        // { "organizationRoles": [ { "id": "...", "name": "agent_admin", "scopes": [...] } ] }
        // or paginated: { "organizationRoles": [...], "total": N }
        Object rolesObj = body.get("organizationRoles");
        if (!(rolesObj instanceof Collection)) {
            return Collections.emptySet();
        }

        Set<String> scopes = new LinkedHashSet<>();
        for (Object item : (Collection<?>) rolesObj) {
            if (!(item instanceof Map)) continue;
            Map<?, ?> role = (Map<?, ?>) item;
            Object scopesField = role.get("scopes");
            if (scopesField instanceof Collection) {
                for (Object s : (Collection<?>) scopesField) {
                    if (s != null) {
                        scopes.add(String.valueOf(s).trim());
                    }
                }
            }
            // also pick up role name as a permission token (e.g. "agent_admin")
            Object name = role.get("name");
            if (name != null) {
                scopes.add(String.valueOf(name).trim());
            }
        }

        log.debug("Resolved {} permission(s) for org={}, user={} from Logto",
                scopes.size(), organizationId, userId);
        return scopes;
    }

    // ------------------------------------------------------------------
    // inner types
    // ------------------------------------------------------------------

    private static final class CachedPermissions {
        private final Set<String> permissions;
        private final long expiresAt;

        CachedPermissions(Set<String> permissions, long ttlMs) {
            this.permissions = permissions;
            this.expiresAt = System.currentTimeMillis() + ttlMs;
        }

        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }
}
