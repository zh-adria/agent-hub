package com.agenthub.client.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.BadJWTException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Logto Cloud Identity Service 实现。
 *
 * <p>通过 OIDC JWKS 本地校验 JWT access token，并调用 Logto Organization Roles Management API
 * 获取用户 RBAC 数据（Organization ID → tenantId，Role scopes → permissions）。
 *
 * <p>激活方式：{@code agenthub.identity.provider=logto}
 *
 * <h3>Logto Cloud token claims 解析映射</h3>
 * <table>
 *   <tr><th>Logto JWT claim</th><th>AgentHub field</th><th>说明</th></tr>
 *   <tr><td>{@code sub}</td><td>userId</td><td>Logto user ID (UUID)</td></tr>
 *   <tr><td>{@code name} / {@code username}</td><td>username</td><td>用户可读名称</td></tr>
 *   <tr><td>{@code org_id}</td><td>tenantId</td><td>当前 token 关联的 Organization ID</td></tr>
 *   <tr><td>{@code scope}</td><td>permissions</td><td>空格分隔的 scope 列表（来自 roles）</td></tr>
 *   <tr><td>{@code roles} (custom claim)</td><td>roles</td><td>用户在 org 中的 role 名称列表</td></tr>
 * </table>
 *
 * <h3>Token 类型</h3>
 * <p>Logto 颁发两类 token：
 * <ul>
 *   <li><b>Access Token</b>：用于访问 AgentHub，包含用户身份和 scope 声明</li>
 *   <li><b>M2M Token</b>：用于服务端调用 Logto Management API（由 OrganizationRoleMapper 管理）</li>
 * </ul>
 */
@Service
@ConditionalOnProperty(prefix = "agenthub.identity", name = "provider", havingValue = "logto")
public class LogtoIdentityService implements IdentityService {

    private static final Logger log = LoggerFactory.getLogger(LogtoIdentityService.class);
    private static final Pattern SCOPE_SPLIT = Pattern.compile("[,\\s]+");

    private final RestTemplate restTemplate;
    private final LogtoIdentityProperties properties;
    private final LogtoJwksService jwksService;
    private final LogtoOrganizationRoleMapper roleMapper;

    @Value("${agenthub.identity.logto.expected-audience:}")
    private String expectedAudience;

    @Value("${agenthub.identity.logto.enable-userinfo-fallback:true}")
    private boolean enableUserinfoFallback;

    @Value("${agenthub.identity.logto.fallback-to-scope-only:false}")
    private boolean fallbackToScopeOnly;

    public LogtoIdentityService(
            LogtoIdentityProperties properties,
            LogtoJwksService jwksService,
            LogtoOrganizationRoleMapper roleMapper) {
        this(new RestTemplate(), properties, jwksService, roleMapper);
    }

    LogtoIdentityService(
            RestTemplate restTemplate,
            LogtoIdentityProperties properties,
            LogtoJwksService jwksService,
            LogtoOrganizationRoleMapper roleMapper) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.jwksService = jwksService;
        this.roleMapper = roleMapper;
    }

    // ------------------------------------------------------------------
    // IdentityService contract
    // ------------------------------------------------------------------

    @Override
    public AuthenticatedPrincipal introspect(String token) {
        if (token == null || token.isBlank()) {
            return inactive("anonymous", null);
        }

        // 1. JWKS 本地校验
        JWTClaimsSet claims;
        try {
            claims = jwksService.validateAndParse(token, resolveExpectedAudience());
        } catch (IllegalArgumentException ex) {
            log.debug("Logto JWT validation failed: {}", ex.getMessage());
            return inactive("anonymous", token);
        }

        // 2. 解析标准 claims
        String userId = text(claims.getSubject()).orElse(null);
        String username = stringClaim(claims, "name")
                .or(() -> stringClaim(claims, "preferred_username"))
                .or(() -> stringClaim(claims, "username"))
                .orElse(userId);
        String orgId = stringClaim(claims, "org_id")
                .or(() -> stringClaim(claims, "organizationId"))
                .orElse(null);

        // 3. 解析 token 内嵌 scope / roles
        Set<String> tokenScopes = parseScopes(safeStringClaim(claims, "scope"));
        Set<String> tokenRoles = safeStringListClaim(claims, "roles");

        // 4. 若 token 无 org_id，尝试从 userinfo 补充
        if (orgId == null && enableUserinfoFallback) {
            orgId = fetchOrgIdFromUserinfo(token);
        }

        // 5. 通过 OrganizationRoleMapper 获取完整权限集
        //    （若 org_id 存在则调 Logto API；否则仅用 token scope 兜底）
        Set<String> permissions = new LinkedHashSet<>(tokenScopes);
        Set<String> roles = new LinkedHashSet<>(tokenRoles);

        if (orgId != null && !orgId.isBlank()) {
            try {
                Set<String> apiPermissions = roleMapper.getPermissions(orgId, userId);
                permissions.addAll(apiPermissions);

                // 从权限名反推 roles（权限中不含冒号的视为 role code）
                for (String p : apiPermissions) {
                    if (!p.contains(":")) {
                        roles.add(p);
                    }
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch Logto organization roles for org={}, user={}: {}",
                        orgId, userId, ex.getMessage());
                if (fallbackToScopeOnly) {
                    // 仅使用 token scope 作为权限
                } else {
                    // 仍然允许请求通过，但权限可能不完整
                    log.debug("Falling back to token-inferred permissions only");
                }
            }
        }

        // 6. 构造 tenant 上下文
        String tenantId = orgId != null ? orgId : userId;
        Set<String> tenantIds = new LinkedHashSet<>();
        if (tenantId != null && !tenantId.isBlank()) {
            tenantIds.add(tenantId);
        }

        return new AuthenticatedPrincipal(
                true, userId, username, tenantId, tenantIds, roles, permissions, token);
    }

    @Override
    public boolean authorize(AuthenticatedPrincipal principal, String tenantId, String action) {
        if (principal == null || !principal.isActive() || !principal.hasTenant(tenantId)) {
            return false;
        }
        // Logto RBAC 决策已通过 OrganizationRoleMapper 在 introspect 阶段完成
        // 这里直接使用本地缓存的 permissions 做最终判定
        return principal.hasPermission(action);
    }

    @Override
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

    @Override
    public Map<String, Object> userInfo(AuthenticatedPrincipal principal, boolean authorizationPresent) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("userId", principal.getUserId());
        response.put("tenantId", principal.getTenantId());
        response.put("username", principal.getUsername());
        response.put("roles", new ArrayList<>(principal.getRoles()));
        response.put("permissions", new ArrayList<>(principal.getPermissions()));
        response.put("authorizationPresent", authorizationPresent);
        return response;
    }

    // ------------------------------------------------------------------
    // helpers
    // ------------------------------------------------------------------

    private String resolveExpectedAudience() {
        if (expectedAudience != null && !expectedAudience.isBlank()) {
            return expectedAudience;
        }
        // fallback: use clientId as audience
        String cid = properties.getClientId();
        return (cid != null && !cid.isBlank()) ? cid : null;
    }

    /**
     * 调用 Logto /oidc/userinfo 获取额外用户信息（如组织 ID）。
     * 仅在 token claims 中未包含 org_id 时使用，降低调用频率。
     */
    private String fetchOrgIdFromUserinfo(String token) {
        String url = properties.getUserinfoUri();
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    url, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
            Map<?, ?> body = response.getBody();
            if (body == null) return null;
            // Logto userinfo 返回 organizations: [{ id: "org-xxx", name: "..." }]
            Object orgs = body.get("organizations");
            if (orgs instanceof List && !((List<?>) orgs).isEmpty()) {
                Object first = ((List<?>) orgs).get(0);
                if (first instanceof Map) {
                    Object id = ((Map<?, ?>) first).get("id");
                    if (id != null) return String.valueOf(id);
                }
            }
            // fallback: org_id at top level
            Object orgId = body.get("org_id");
            if (orgId != null) return String.valueOf(orgId);
        } catch (HttpStatusCodeException ex) {
            log.debug("Logto userinfo returned {}: {}", ex.getStatusCode(), ex.getMessage());
        } catch (RestClientException ex) {
            log.debug("Logto userinfo call failed: {}", ex.getMessage());
        }
        return null;
    }

    private AuthenticatedPrincipal inactive(String userId, String token) {
        return new AuthenticatedPrincipal(
                false, userId, userId, null, Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(), token);
    }

    private Set<String> parseScopes(String scopeClaim) {
        if (scopeClaim == null || scopeClaim.isBlank()) {
            return new LinkedHashSet<>();
        }
        // Decode in case it's URL-encoded
        try {
            scopeClaim = URLDecoder.decode(scopeClaim, StandardCharsets.UTF_8);
        } catch (Exception ignored) {
        }
        Set<String> scopes = new LinkedHashSet<>();
        for (String s : SCOPE_SPLIT.split(scopeClaim)) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                scopes.add(trimmed);
            }
        }
        return scopes;
    }

    private Set<String> parseStringList(String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }
        Set<String> result = new LinkedHashSet<>();
        for (String s : SCOPE_SPLIT.split(value)) {
            String trimmed = s.trim();
            if (!trimmed.isEmpty()) {
                result.add(trimmed);
            }
        }
        return result;
    }

    private Optional<String> text(String value) {
        return Optional.ofNullable(value)
                .filter(v -> !v.isBlank())
                .map(String::trim);
    }

    /**
     * 安全调用 {@link JWTClaimsSet#getStringClaim(String)}，忽略解析异常。
     */
    private Optional<String> stringClaim(JWTClaimsSet claims, String name) {
        try {
            String value = claims.getStringClaim(name);
            return text(value);
        } catch (java.text.ParseException ex) {
            return Optional.empty();
        }
    }

    private String safeStringClaim(JWTClaimsSet claims, String name) {
        try {
            return claims.getStringClaim(name);
        } catch (java.text.ParseException ex) {
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private Set<String> safeStringListClaim(JWTClaimsSet claims, String name) {
        try {
            List<String> list = claims.getStringListClaim(name);
            return new LinkedHashSet<>(list);
        } catch (java.text.ParseException ex) {
            return Collections.emptySet();
        }
    }

    private List<Map<String, Object>> tenantSummaries(Set<String> tenantIds) {
        List<Map<String, Object>> tenants = new ArrayList<>();
        for (String tid : tenantIds) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("tenantId", tid);
            item.put("name", tid);
            tenants.add(item);
        }
        return tenants;
    }
}
