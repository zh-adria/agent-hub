package com.agenthub.client.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agenthub.identity.iam")
public class IamIdentityProperties {
    private String baseUrl = "http://127.0.0.1:8080/iam";
    private String loginPath = "/api/auth/login";
    private String logoutPath = "/api/auth/logout";
    private String introspectPath = "/oauth/introspect";
    private String authorizePath = "/api/authz/check";
    private String clientId = "demo-client";
    private String clientSecret = "demo-secret";

    public String loginUrl() {
        return join(baseUrl, loginPath);
    }

    public String logoutUrl() {
        return join(baseUrl, logoutPath);
    }

    public String introspectUrl() {
        return join(baseUrl, introspectPath);
    }

    public String authorizeUrl() {
        return join(baseUrl, authorizePath);
    }

    private String join(String base, String path) {
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + normalizedPath;
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getLoginPath() { return loginPath; }
    public void setLoginPath(String loginPath) { this.loginPath = loginPath; }
    public String getLogoutPath() { return logoutPath; }
    public void setLogoutPath(String logoutPath) { this.logoutPath = logoutPath; }
    public String getIntrospectPath() { return introspectPath; }
    public void setIntrospectPath(String introspectPath) { this.introspectPath = introspectPath; }
    public String getAuthorizePath() { return authorizePath; }
    public void setAuthorizePath(String authorizePath) { this.authorizePath = authorizePath; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getClientSecret() { return clientSecret; }
    public void setClientSecret(String clientSecret) { this.clientSecret = clientSecret; }
}
