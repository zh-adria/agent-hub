package com.agenthub.client.auth;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.*;

@Service
@ConditionalOnProperty(prefix = "agenthub.identity", name = "provider", havingValue = "iam", matchIfMissing = true)
public class IamIdentityService implements IdentityService {
    private final RestTemplate restTemplate;
    private final IamIdentityProperties properties;

    public IamIdentityService(IamIdentityProperties properties) {
        this(new RestTemplate(), properties);
    }

    IamIdentityService(RestTemplate restTemplate, IamIdentityProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    @Override
    public AuthenticatedPrincipal introspect(String token) {
        Map<String, Object> response = introspectionResponse(token);
        boolean active = booleanValue(response.get("active"));
        String userId = firstText(response.get("uid"), response.get("user_id"), response.get("sub"));
        String username = firstText(response.get("sub"), response.get("preferred_username"), userId);
        String tenantId = firstText(response.get("tenant"), response.get("tenantId"));
        Set<String> roles = stringSet(response.get("roles"));
        Set<String> permissions = permissions(response);
        Set<String> tenants = new LinkedHashSet<>();
        if (tenantId != null && !tenantId.trim().isEmpty()) {
            tenants.add(tenantId);
        }
        return new AuthenticatedPrincipal(active, userId, username, tenantId, tenants, roles, permissions, token);
    }

    @Override
    public boolean authorize(AuthenticatedPrincipal principal, String tenantId, String action) {
        if (principal == null || !principal.isActive() || !principal.hasTenant(tenantId)) {
            return false;
        }
        String token = principal.getAccessToken();
        if (token == null || token.trim().isEmpty()) {
            return principal.hasPermission(action);
        }

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(token);
        Map<String, Object> target = new LinkedHashMap<>();
        target.put("tenantCode", tenantId);
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("permission", action);
        body.put("target", target);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.authorizeUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);
            Map<?, ?> responseBody = response.getBody();
            Object data = responseBody != null ? responseBody.get("data") : null;
            if (data instanceof Map) {
                return booleanValue(((Map<?, ?>) data).get("allowed"));
            }
            return responseBody != null && booleanValue(responseBody.get("allowed"));
        } catch (RestClientException ex) {
            return false;
        }
    }

    @Override
    public Map<String, Object> introspectionResponse(String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("token", token);
        body.add("client_id", properties.getClientId());
        body.add("client_secret", properties.getClientSecret());

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    properties.introspectUrl(),
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    Map.class);
            Map<String, Object> responseBody = response.getBody();
            return responseBody != null ? responseBody : inactive();
        } catch (RestClientException ex) {
            return inactive();
        }
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

    private Map<String, Object> inactive() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("active", false);
        return response;
    }

    private Set<String> permissions(Map<String, Object> response) {
        Set<String> values = stringSet(response.get("perms"));
        values.addAll(stringSet(response.get("permissions")));
        values.addAll(splitScopes(response.get("scope")));
        return values;
    }

    private Set<String> splitScopes(Object value) {
        Set<String> values = new LinkedHashSet<>();
        if (value == null) {
            return values;
        }
        for (String item : String.valueOf(value).split("[, ]")) {
            String trimmed = item.trim();
            if (!trimmed.isEmpty()) {
                values.add(trimmed);
            }
        }
        return values;
    }

    private Set<String> stringSet(Object value) {
        Set<String> values = new LinkedHashSet<>();
        if (value instanceof Collection) {
            for (Object item : (Collection<?>) value) {
                if (item != null && !String.valueOf(item).trim().isEmpty()) {
                    values.add(String.valueOf(item).trim());
                }
            }
        } else if (value != null) {
            values.addAll(splitScopes(value));
        }
        return values;
    }

    private boolean booleanValue(Object value) {
        return Boolean.TRUE.equals(value) || "true".equalsIgnoreCase(String.valueOf(value));
    }

    private String firstText(Object... values) {
        for (Object value : values) {
            if (value != null && !String.valueOf(value).trim().isEmpty()) {
                return String.valueOf(value).trim();
            }
        }
        return null;
    }
}
