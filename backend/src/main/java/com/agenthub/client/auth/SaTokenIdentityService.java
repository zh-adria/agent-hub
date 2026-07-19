package com.agenthub.client.auth;

import cn.dev33.satoken.session.SaSession;
import cn.dev33.satoken.stp.StpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import java.util.*;

/**
 * Sa-Token Identity Service 实现。
 *
 * <p>基于 Sa-Token SSO 框架实现 {@link IdentityService} 接口：
 * <ol>
 *   <li>登录：调用 {@code StpUtil.login(userId)} 创建登录 Session</li>
 *   <li>Token 校验：调用 {@code StpUtil.getLoginIdByToken(token)} 校验 token 有效性</li>
 *   <li>用户信息：从 Sa-Token Session 中读取用户上下文</li>
 *   <li>权限判定：调用 {@code StpUtil.hasPermission(action)}</li>
 * </ol>
 *
 * <p>激活方式：{@code agenthub.identity.provider=sa-token}
 *
 * <p>Sa-Token 默认使用 Redis（已配置）存储 Session 和 Token 数据。
 * 权限数据通过 Session 的 PERMISSION_LIST 和 ROLE_LIST 存储。
 */
@Service
@ConditionalOnProperty(prefix = "agenthub.identity", name = "provider", havingValue = "sa-token")
public class SaTokenIdentityService implements IdentityService {

    private static final Logger log = LoggerFactory.getLogger(SaTokenIdentityService.class);

    // Sa-Token Session keys
    public static final String SESSION_KEY_USERNAME = "username";
    public static final String SESSION_KEY_TENANT_ID = "tenantId";
    public static final String SESSION_KEY_TENANT_IDS = "tenantIds";
    public static final String SESSION_KEY_ROLES = "roles";

    // ------------------------------------------------------------------
    // IdentityService contract
    // ------------------------------------------------------------------

    @Override
    public AuthenticatedPrincipal introspect(String token) {
        if (token == null || token.isBlank()) {
            return inactive("anonymous", null);
        }

        try {
            // Sa-Token token 校验：返回 loginId 或 null
            Object loginIdObj = StpUtil.getLoginIdByToken(token);
            if (loginIdObj == null) {
                return inactive("anonymous", token);
            }

            String userId = String.valueOf(loginIdObj);

            // 从 Token Session 读取用户信息
            SaSession session = StpUtil.getTokenSessionByToken(token);
            if (session == null) {
                return inactive("anonymous", token);
            }

            String username = session.getString(SESSION_KEY_USERNAME);
            if (username == null || username.isEmpty()) {
                username = userId;
            }

            String tenantId = session.getString(SESSION_KEY_TENANT_ID);
            if (tenantId == null || tenantId.isEmpty()) {
                tenantId = userId;
            }

            @SuppressWarnings("unchecked")
            Set<String> tenantIds = (Set<String>) session.get(SESSION_KEY_TENANT_IDS);
            if (tenantIds == null || tenantIds.isEmpty()) {
                tenantIds = new LinkedHashSet<>();
                if (tenantId != null && !tenantId.isEmpty()) {
                    tenantIds.add(tenantId);
                }
            }

            @SuppressWarnings("unchecked")
            Set<String> roles = (Set<String>) session.get(SESSION_KEY_ROLES);
            if (roles == null) {
                roles = new LinkedHashSet<>();
            }

            // 从 Sa-Token 获取权限列表
            Set<String> permissions = new LinkedHashSet<>(StpUtil.getPermissionList());

            return new AuthenticatedPrincipal(
                    true, userId, username, tenantId, tenantIds, roles, permissions, token);

        } catch (Exception ex) {
            log.debug("Sa-Token introspect error: {}", ex.getMessage());
            return inactive("anonymous", token);
        }
    }

    @Override
    public boolean authorize(AuthenticatedPrincipal principal, String tenantId, String action) {
        if (principal == null || !principal.isActive() || !principal.hasTenant(tenantId)) {
            return false;
        }

        // 优先使用本地缓存的 permissions（introspect 时已从 Sa-Token 获取）
        if (principal.hasPermission(action)) {
            return true;
        }

        // 兜底：直接调用 Sa-Token 实时校验
        try {
            return StpUtil.hasPermission(action);
        } catch (Exception ex) {
            return false;
        }
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
    // Session management helpers
    // ------------------------------------------------------------------

    /**
     * 执行登录，将用户信息写入 Sa-Token Session。
     *
     * @param userId     用户 ID
     * @param username   用户名
     * @param tenantId   当前租户 ID
     * @param tenantIds  用户所属的所有租户
     * @param roles      用户角色列表
     * @param permissions 用户权限列表
     */
    public void doLogin(
            String userId,
            String username,
            String tenantId,
            Set<String> tenantIds,
            Set<String> roles,
            Set<String> permissions) {
        // Sa-Token 登录（建立 token-session 映射）
        StpUtil.login(userId);

        // 将业务数据写入 Session
        SaSession session = StpUtil.getSession();
        session.set(SESSION_KEY_USERNAME, username != null ? username : userId);
        session.set(SESSION_KEY_TENANT_ID, tenantId != null ? tenantId : userId);
        session.set(SESSION_KEY_TENANT_IDS, tenantIds != null ? tenantIds : Collections.singleton(userId));
        session.set(SESSION_KEY_ROLES, roles != null ? roles : Collections.emptySet());

        // 写入 Sa-Token 内置权限和角色列表
        session.set(SaSession.PERMISSION_LIST, new ArrayList<>(permissions != null ? permissions : Collections.emptySet()));
        session.set(SaSession.ROLE_LIST, new ArrayList<>(roles != null ? roles : Collections.emptySet()));

        log.debug("Sa-Token login: userId={}, tenantId={}, permissions={}",
                userId, tenantId, permissions);
    }

    /**
     * 登出，销毁 Sa-Token Session。
     */
    public void doLogout() {
        try {
            StpUtil.logout();
        } catch (Exception ex) {
            log.warn("Sa-Token logout error: {}", ex.getMessage());
        }
    }

    /**
     * 踢人下线（管理员操作）。
     *
     * @param userId 要踢下线的用户 ID
     */
    public void kickout(String userId) {
        try {
            StpUtil.logout(userId);
        } catch (Exception ex) {
            log.warn("Sa-Token kickout error for user={}: {}", userId, ex.getMessage());
        }
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private AuthenticatedPrincipal inactive(String userId, String token) {
        return new AuthenticatedPrincipal(
                false, userId, userId, null, Collections.emptySet(),
                Collections.emptySet(), Collections.emptySet(), token);
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
