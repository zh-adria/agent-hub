package com.agenthub.client.tokenrouter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "token-router")
public class TokenRouterProperties {
    private String baseUrl = "http://127.0.0.1:8082";
    private String completionPath = "/api/chat/completions";
    private String streamCompletionPath = "/api/chat/completions/stream";

    public String completionUrl() {
        return join(baseUrl, completionPath);
    }

    public String streamCompletionUrl() {
        return join(baseUrl, streamCompletionPath);
    }

    private String join(String base, String path) {
        String trimmedBase = base.endsWith("/") ? base.substring(0, base.length() - 1) : base;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        return trimmedBase + normalizedPath;
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCompletionPath() { return completionPath; }
    public void setCompletionPath(String completionPath) { this.completionPath = completionPath; }
    public String getStreamCompletionPath() { return streamCompletionPath; }
    public void setStreamCompletionPath(String streamCompletionPath) { this.streamCompletionPath = streamCompletionPath; }
}
