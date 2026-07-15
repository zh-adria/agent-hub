package com.agenthub.client.api;

import com.agenthub.client.auth.AuthContext;
import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.IdentityService;
import com.agenthub.domain.context.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;

@Component("agentHubRequestContextFilter")
public class RequestContextFilter extends OncePerRequestFilter {
    private final IdentityService identityService;
    private final ObjectMapper objectMapper;

    public RequestContextFilter(IdentityService identityService, ObjectMapper objectMapper) {
        this.identityService = identityService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        try {
            String token = bearerToken(request);
            if (token == null) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Missing bearer token");
                return;
            }
            AuthenticatedPrincipal principal = identityService.introspect(token);
            if (!principal.isActive()) {
                writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "UNAUTHORIZED", "Inactive bearer token");
                return;
            }
            String externalTenantId = headerOrDefault(request, "X-Tenant-Id", principal.getTenantId());
            if (!principal.hasTenant(externalTenantId)) {
                writeError(response, HttpServletResponse.SC_FORBIDDEN, "FORBIDDEN", "User is not assigned to tenant: " + externalTenantId);
                return;
            }
            TenantContext.set(toNumericTenantId(externalTenantId), externalTenantId, principal.getUserId());
            AuthContext.set(principal);
            filterChain.doFilter(request, response);
        } finally {
            AuthContext.clear();
            TenantContext.clear();
        }
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return "OPTIONS".equalsIgnoreCase(request.getMethod())
                || path.startsWith("/ws/")
                || path.startsWith("/mock")
                || path.equals("/api/auth/login")
                || path.equals("/api/health")
                || path.startsWith("/api/health/");
    }

    private String bearerToken(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization == null || authorization.trim().isEmpty()) {
            return null;
        }
        String value = authorization.trim();
        if (value.toLowerCase().startsWith("bearer ")) {
            return value.substring(7).trim();
        }
        return value;
    }

    private String headerOrDefault(HttpServletRequest request, String name, String fallback) {
        String value = request.getHeader(name);
        return value != null && !value.trim().isEmpty() ? value.trim() : fallback;
    }

    private Long toNumericTenantId(String externalTenantId) {
        if (externalTenantId == null || externalTenantId.trim().isEmpty()) {
            return 1L;
        }
        String value = externalTenantId.trim();
        if (value.matches("\\d+")) {
            return Long.parseLong(value);
        }
        int dash = value.lastIndexOf('-');
        if (dash >= 0 && dash + 1 < value.length()) {
            String suffix = value.substring(dash + 1).replaceFirst("^0+", "");
            if (!suffix.isEmpty() && suffix.matches("\\d+")) {
                return Long.parseLong(suffix);
            }
        }
        return 1L;
    }

    private void writeError(HttpServletResponse response, int status, String error, String message) throws IOException {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("error", error);
        body.put("message", message);
        objectMapper.writeValue(response.getWriter(), body);
    }
}
