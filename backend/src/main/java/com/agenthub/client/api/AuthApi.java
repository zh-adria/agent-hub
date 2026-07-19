package com.agenthub.client.api;

import com.agenthub.client.auth.AuthContext;
import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.IdentityService;
import com.agenthub.client.auth.SaTokenIdentityService;
import com.agenthub.client.auth.UnauthorizedException;
import cn.dev33.satoken.stp.StpUtil;
import cn.dev33.satoken.session.SaSession;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@RestController
@RequestMapping("/api/auth")
public class AuthApi {
    private static final Logger log = LoggerFactory.getLogger(AuthApi.class);

    private final RestTemplate restTemplate;
    private final IdentityService identityService;
    private final String identityProvider;

    @Autowired(required = false)
    private SaTokenIdentityService saTokenIdentityService;

    public AuthApi(
            IdentityService identityService,
            @Value("${agenthub.identity.provider:sa-token}") String identityProvider) {
        this.restTemplate = new RestTemplate();
        this.identityService = identityService;
        this.identityProvider = identityProvider;
    }

    // ------------------------------------------------------------------
    // Login
    // ------------------------------------------------------------------

    @PostMapping("/login")
    public Map<String, Object> login(@RequestBody Map<String, Object> body) {
        if ("mock".equalsIgnoreCase(identityProvider)) {
            return mockLogin(body);
        }
        if ("sa-token".equalsIgnoreCase(identityProvider)) {
            return saTokenLogin(body);
        }
        throw new UnauthorizedException("Unsupported identity provider: " + identityProvider);
    }

    // ------------------------------------------------------------------
    // Sa-Token native endpoints (pass-through)
    // ------------------------------------------------------------------

    /**
     * 获取当前登录用户信息（Sa-Token 原生接口）。
     *
     * <p>等价于 GET /stp/session-info，返回当前 token 对应的用户 Session 数据。
     */
    @GetMapping("/sa-token/session-info")
    public Map<String, Object> saTokenSessionInfo() {
        if (!"sa-token".equalsIgnoreCase(identityProvider)) {
            throw new UnauthorizedException("Sa-Token provider is not configured");
        }
        if (!StpUtil.isLogin()) {
            return Map.of("code", 2001, "msg", "未登录");
        }
        Map<String, Object> info = new LinkedHashMap<>();
        info.put("loginId", StpUtil.getLoginId());
        info.put("tokenName", StpUtil.getTokenName());
        info.put("tokenValue", StpUtil.getTokenValue());
        info.put("permissions", new ArrayList<>(StpUtil.getPermissionList()));
        info.put("roles", new ArrayList<>(StpUtil.getRoleList()));
        SaSession session = StpUtil.getSession();
        Map<String, Object> sessionData = new LinkedHashMap<>();
        sessionData.put("username", session.getString(SaTokenIdentityService.SESSION_KEY_USERNAME));
        sessionData.put("tenantId", session.getString(SaTokenIdentityService.SESSION_KEY_TENANT_ID));
        sessionData.put("permissions", new ArrayList<>(StpUtil.getPermissionList()));
        sessionData.put("roles", new ArrayList<>(StpUtil.getRoleList()));
        info.put("sessionData", sessionData);
        return info;
    }

    /**
     * Sa-Token 权限校验（原生接口）。
     *
     * <p>等价于 GET /stp/check-permission
     */
    @GetMapping("/sa-token/check-permission")
    public Map<String, Object> saTokenCheckPermission(@RequestParam String permission) {
        if (!"sa-token".equalsIgnoreCase(identityProvider)) {
            throw new UnauthorizedException("Sa-Token provider is not configured");
        }
        boolean allowed = StpUtil.hasPermission(permission);
        return Map.of("permission", permission, "allowed", allowed);
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
        if (saTokenIdentityService != null) {
            saTokenIdentityService.doLogout();
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("ok", true);
        return response;
    }

    // ------------------------------------------------------------------
    // private helpers
    // ------------------------------------------------------------------

    private Map<String, Object> saTokenLogin(Map<String, Object> body) {
        String username = text(body.get("username"));
        String password = text(body.get("password"));
        String tenantId = text(body.get("tenantId"), "default");

        if (username == null || password == null) {
            throw new UnauthorizedException("username and password are required");
        }

        // 简单凭证校验（生产环境应替换为数据库校验）
        if (!"admin".equals(username) || !"admin123".equals(password)) {
            // 尝试从 Sa-Token Session 读取（如果已有登录态）
            if (StpUtil.isLogin()) {
                return buildLoginResponse(StpUtil.getLoginId(), StpUtil.getTokenValue());
            }
            throw new UnauthorizedException("Invalid username or password");
        }

        // 执行 Sa-Token 登录
        String userId = "user-" + tenantId + "-" + username;
        Set<String> tenantIds = new LinkedHashSet<>(Collections.singleton(tenantId));
        Set<String> roles = new LinkedHashSet<>(Collections.singleton("admin"));
        Set<String> permissions = Set.of(
                "agent:create", "agent:read", "agent:update", "agent:delete",
                "function:create", "function:read", "function:invoke",
                "session:create", "session:read", "session:message",
                "knowledge:create", "knowledge:read", "knowledge:search",
                "workflow:read", "workflow:execute",
                "audit:read"
        );

        if (saTokenIdentityService != null) {
            saTokenIdentityService.doLogin(userId, username, tenantId, tenantIds, roles, permissions);
        } else {
            // fallback: 直接使用 Sa-Token API
            StpUtil.login(userId);
            SaSession session = StpUtil.getSession();
            session.set(SaTokenIdentityService.SESSION_KEY_USERNAME, username);
            session.set(SaTokenIdentityService.SESSION_KEY_TENANT_ID, tenantId);
            session.set(SaTokenIdentityService.SESSION_KEY_TENANT_IDS, tenantIds);
            session.set(SaTokenIdentityService.SESSION_KEY_ROLES, roles);
            session.set(SaSession.PERMISSION_LIST, new ArrayList<>(permissions));
            session.set(SaSession.ROLE_LIST, new ArrayList<>(roles));
        }

        String tokenValue = StpUtil.getTokenValue();
        return buildLoginResponse(userId, tokenValue);
    }

    private Map<String, Object> buildLoginResponse(Object loginId, Object tokenValue) {
        String userIdStr = loginId != null ? String.valueOf(loginId) : "unknown";
        String tokenStr = tokenValue != null ? String.valueOf(tokenValue) : "";
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("code", 200);
        response.put("msg", "登录成功");
        response.put("data", Map.of(
                "userId", userIdStr,
                "tokenName", StpUtil.getTokenName(),
                "tokenValue", tokenStr
        ));
        return response;
    }

    private Map<String, Object> mockLogin(Map<String, Object> body) {
        String tenantCode = text(body.get("tenantCode"), "tenant-001");
        String username = text(body.get("username"), "admin");
        String mockToken = "tenant-002".equals(tenantCode) ? "tenant-002-token" : "mock-token";

        // mock 模式：兼容旧版返回格式（测试依赖）
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("accessToken", mockToken);
        response.put("tokenType", "Bearer");
        response.put("tenantId", tenantCode);
        response.put("username", username);
        return response;
    }

    private String text(Object value) {
        return text(value, null);
    }

    private String text(Object value, String fallback) {
        if (value == null || String.valueOf(value).trim().isEmpty()) {
            return fallback;
        }
        return String.valueOf(value).trim();
    }
}
