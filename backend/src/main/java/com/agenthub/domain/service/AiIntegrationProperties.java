package com.agenthub.domain.service;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "agenthub.ai")
public class AiIntegrationProperties {
    private final Embedding embedding = new Embedding();
    private final Rerank rerank = new Rerank();
    private final Milvus milvus = new Milvus();

    public Embedding getEmbedding() {
        return embedding;
    }

    public Rerank getRerank() {
        return rerank;
    }

    public Milvus getMilvus() {
        return milvus;
    }

    public static class Embedding {
        private boolean externalEnabled;
        private String url;
        private String provider = "llm-gateway";
        private String model = "text-embedding";
        private Integer timeoutMs = 3000;
        private boolean fallbackOnFailure = true;

        public boolean isExternalEnabled() { return externalEnabled; }
        public void setExternalEnabled(boolean externalEnabled) { this.externalEnabled = externalEnabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getProvider() { return provider; }
        public void setProvider(String provider) { this.provider = provider; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isFallbackOnFailure() { return fallbackOnFailure; }
        public void setFallbackOnFailure(boolean fallbackOnFailure) { this.fallbackOnFailure = fallbackOnFailure; }
    }

    public static class Rerank {
        private boolean externalEnabled;
        private String url;
        private String model = "rerank-default";
        private Integer timeoutMs = 3000;
        private boolean fallbackOnFailure = true;

        public boolean isExternalEnabled() { return externalEnabled; }
        public void setExternalEnabled(boolean externalEnabled) { this.externalEnabled = externalEnabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getModel() { return model; }
        public void setModel(String model) { this.model = model; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isFallbackOnFailure() { return fallbackOnFailure; }
        public void setFallbackOnFailure(boolean fallbackOnFailure) { this.fallbackOnFailure = fallbackOnFailure; }
    }

    public static class Milvus {
        private boolean enabled;
        private String url;
        private String collectionPrefix = "agenthub";
        private Integer timeoutMs = 3000;
        private boolean fallbackOnFailure = true;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }
        public String getCollectionPrefix() { return collectionPrefix; }
        public void setCollectionPrefix(String collectionPrefix) { this.collectionPrefix = collectionPrefix; }
        public Integer getTimeoutMs() { return timeoutMs; }
        public void setTimeoutMs(Integer timeoutMs) { this.timeoutMs = timeoutMs; }
        public boolean isFallbackOnFailure() { return fallbackOnFailure; }
        public void setFallbackOnFailure(boolean fallbackOnFailure) { this.fallbackOnFailure = fallbackOnFailure; }
    }
}
