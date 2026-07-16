package com.agenthub.client.tokenrouter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

@Component
@ConfigurationProperties(prefix = "token-router")
public class TokenRouterProperties {
    private String baseUrl = "http://127.0.0.1:8082";
    private String completionPath = "/api/chat/completions";
    private String streamCompletionPath = "/api/chat/completions/stream";

    public String completionUrl() {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(completionPath)
                .build()
                .toUriString();
    }

    public String streamCompletionUrl() {
        return UriComponentsBuilder.fromHttpUrl(baseUrl)
                .path(streamCompletionPath)
                .build()
                .toUriString();
    }

    public String getBaseUrl() { return baseUrl; }
    public void setBaseUrl(String baseUrl) { this.baseUrl = baseUrl; }
    public String getCompletionPath() { return completionPath; }
    public void setCompletionPath(String completionPath) { this.completionPath = completionPath; }
    public String getStreamCompletionPath() { return streamCompletionPath; }
    public void setStreamCompletionPath(String streamCompletionPath) { this.streamCompletionPath = streamCompletionPath; }
}
