package com.agenthub.client.auth;

import com.agenthub.domain.context.TenantContext;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class RbacInterceptor implements HandlerInterceptor {
    private final IdentityService identityService;

    public RbacInterceptor(IdentityService identityService) {
        this.identityService = identityService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String action = actionFor(request);
        if (action == null) {
            return true;
        }
        AuthenticatedPrincipal principal = AuthContext.principal();
        if (!identityService.authorize(principal, TenantContext.externalTenantId(), action)) {
            throw new ForbiddenException("Permission denied: " + action);
        }
        return true;
    }

    private String actionFor(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        if (!path.startsWith("/api/")) {
            return null;
        }
        if (path.equals("/api/health") || path.startsWith("/mock")) {
            return null;
        }
        if (path.startsWith("/api/audit/") || path.startsWith("/api/observability")) {
            return "audit:read";
        }
        if (path.startsWith("/api/traces")) {
            return "trace:read";
        }
        if (path.startsWith("/api/workflows")) {
            if (path.endsWith("/execute")) {
                return "workflow:execute";
            }
            return crudAction("workflow", method, null);
        }
        if (path.startsWith("/api/evaluations")) {
            return "GET".equals(method) ? "evaluation:read" : "evaluation:run";
        }
        if (path.startsWith("/api/bots")) {
            if (path.contains("/webhooks/")) {
                return "session:message";
            }
            return crudAction("bot", method, null);
        }
        if (path.startsWith("/api/mcp")) {
            return "function:create";
        }
        if (path.startsWith("/api/agents")) {
            if (path.contains("/functions")) {
                return "GET".equals(method) ? "agent:read" : "agent:update";
            }
            return crudAction("agent", method, null);
        }
        if (path.startsWith("/api/functions")) {
            if (path.endsWith("/invoke") || path.endsWith("/test")) {
                return "function:invoke";
            }
            return crudAction("function", method, null);
        }
        if (path.startsWith("/api/sessions")) {
            if (path.contains("/messages")) {
                return "GET".equals(method) ? "session:read" : "session:message";
            }
            if (path.endsWith("/status")) {
                return "session:read";
            }
            return crudAction("session", method, "session:message");
        }
        if (path.startsWith("/api/knowledge-bases")) {
            if (path.endsWith("/search")) {
                return "knowledge:search";
            }
            return crudAction("knowledge", method, null);
        }
        return null;
    }

    private String crudAction(String resource, String method, String updateAction) {
        if ("GET".equals(method)) return resource + ":read";
        if ("POST".equals(method)) return resource + ":create";
        if ("PUT".equals(method) || "PATCH".equals(method)) {
            return updateAction != null ? updateAction : resource + ":update";
        }
        if ("DELETE".equals(method)) return resource + ":delete";
        return null;
    }
}
