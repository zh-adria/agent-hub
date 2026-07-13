package com.agenthub.client.auth;

import java.util.Map;

public interface IdentityService {
    AuthenticatedPrincipal introspect(String token);

    boolean authorize(AuthenticatedPrincipal principal, String tenantId, String action);

    Map<String, Object> introspectionResponse(String token);

    Map<String, Object> userInfo(AuthenticatedPrincipal principal, boolean authorizationPresent);
}
