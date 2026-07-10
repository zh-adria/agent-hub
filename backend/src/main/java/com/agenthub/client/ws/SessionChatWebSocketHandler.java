package com.agenthub.client.ws;

import com.agenthub.client.auth.AuthContext;
import com.agenthub.client.auth.AuthenticatedPrincipal;
import com.agenthub.client.auth.MockIdentityService;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.service.SessionMessageService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
public class SessionChatWebSocketHandler extends TextWebSocketHandler {
    private final SessionMessageService sessionMessageService;
    private final MockIdentityService identityService;
    private final ObjectMapper objectMapper;

    public SessionChatWebSocketHandler(
            SessionMessageService sessionMessageService,
            MockIdentityService identityService,
            ObjectMapper objectMapper) {
        this.sessionMessageService = sessionMessageService;
        this.identityService = identityService;
        this.objectMapper = objectMapper;
    }

    @Override
    protected void handleTextMessage(WebSocketSession socketSession, TextMessage textMessage) throws Exception {
        try {
            RequestContext context = authenticate(socketSession.getUri());
            if (!identityService.authorize(context.principal, context.tenantId, "session:message")) {
                socketSession.sendMessage(new TextMessage(event("error", null, "Permission denied: session:message")));
                return;
            }
            TenantContext.set(toNumericTenantId(context.tenantId), context.tenantId, context.principal.getUserId());
            AuthContext.set(context.principal);

            Map<String, Object> payload = objectMapper.readValue(textMessage.getPayload(), new TypeReference<Map<String, Object>>() {});
            String sessionId = String.valueOf(payload.get("sessionId"));
            String content = payload.get("content") != null ? String.valueOf(payload.get("content")) : "";
            socketSession.sendMessage(new TextMessage(event("started", sessionId, null)));
            Message response = sessionMessageService.send(sessionId, sessionMessageService.newUserMessage(sessionId, content));
            for (String chunk : chunks(response.getContent())) {
                socketSession.sendMessage(new TextMessage(event("chunk", sessionId, chunk)));
            }
            socketSession.sendMessage(new TextMessage(event("completed", sessionId, response.getContent())));
        } catch (Exception ex) {
            socketSession.sendMessage(new TextMessage(event("error", null, ex.getMessage())));
        } finally {
            AuthContext.clear();
            TenantContext.clear();
        }
    }

    private RequestContext authenticate(URI uri) {
        Map<String, String> query = UriComponentsBuilder.fromUri(uri).build().getQueryParams().toSingleValueMap();
        String token = query.get("token") != null ? query.get("token") : "mock-token";
        AuthenticatedPrincipal principal = identityService.introspect(token);
        if (!principal.isActive()) {
            throw new IllegalArgumentException("Inactive bearer token");
        }
        String tenantId = query.get("tenantId") != null ? query.get("tenantId") : principal.getTenantId();
        if (!principal.hasTenant(tenantId)) {
            throw new IllegalArgumentException("User is not assigned to tenant: " + tenantId);
        }
        return new RequestContext(principal, tenantId);
    }

    private String[] chunks(String content) {
        String safe = content != null ? content : "";
        if (safe.isEmpty()) {
            return new String[]{""};
        }
        int size = 64;
        int count = (safe.length() + size - 1) / size;
        String[] chunks = new String[count];
        for (int i = 0; i < count; i++) {
            int start = i * size;
            chunks[i] = safe.substring(start, Math.min(start + size, safe.length()));
        }
        return chunks;
    }

    private String event(String type, String sessionId, String data) throws Exception {
        Map<String, Object> event = new LinkedHashMap<>();
        event.put("type", type);
        if (sessionId != null) event.put("sessionId", sessionId);
        if (data != null) event.put("data", data);
        return objectMapper.writeValueAsString(event);
    }

    private Long toNumericTenantId(String externalTenantId) {
        if (externalTenantId == null || externalTenantId.trim().isEmpty()) return 1L;
        String value = externalTenantId.trim();
        int dash = value.lastIndexOf('-');
        if (dash >= 0 && dash + 1 < value.length()) {
            String suffix = value.substring(dash + 1).replaceFirst("^0+", "");
            if (!suffix.isEmpty() && suffix.matches("\\d+")) {
                return Long.parseLong(suffix);
            }
        }
        return value.matches("\\d+") ? Long.parseLong(value) : 1L;
    }

    private static class RequestContext {
        private final AuthenticatedPrincipal principal;
        private final String tenantId;

        private RequestContext(AuthenticatedPrincipal principal, String tenantId) {
            this.principal = principal;
            this.tenantId = tenantId;
        }
    }
}
