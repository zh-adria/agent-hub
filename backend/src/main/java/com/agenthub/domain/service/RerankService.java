package com.agenthub.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@Service
public class RerankService {
    private final AiIntegrationProperties properties;

    public RerankService() {
        this(new AiIntegrationProperties());
    }

    public RerankService(AiIntegrationProperties properties) {
        this.properties = properties;
    }

    public double rerank(String query, String content, double vectorScore, double keywordScore) {
        if (externalEnabled()) {
            try {
                return externalRerank(query, content, vectorScore, keywordScore);
            } catch (Exception ex) {
                if (!properties.getRerank().isFallbackOnFailure()) {
                    throw new IllegalStateException("External rerank failed", ex);
                }
            }
        }
        double overlapScore = overlapScore(query, content);
        double lengthPenalty = lengthPenalty(content);
        return (0.55 * vectorScore) + (0.30 * keywordScore) + (0.20 * overlapScore) - lengthPenalty;
    }

    public double keywordScore(String query, String content) {
        Set<String> terms = queryTerms(query);
        if (terms.isEmpty() || content == null || content.isEmpty()) {
            return 0;
        }
        String normalizedContent = content.toLowerCase(Locale.ROOT);
        int matches = 0;
        for (String term : terms) {
            if (normalizedContent.contains(term)) {
                matches++;
            }
        }
        return Math.min(1.0, (double) matches / terms.size());
    }

    private double overlapScore(String query, String content) {
        Set<String> queryTerms = queryTerms(query);
        Set<String> contentTerms = queryTerms(content);
        if (queryTerms.isEmpty() || contentTerms.isEmpty()) {
            return 0;
        }
        int overlap = 0;
        for (String term : queryTerms) {
            if (contentTerms.contains(term)) {
                overlap++;
            }
        }
        return (double) overlap / queryTerms.size();
    }

    private double lengthPenalty(String content) {
        if (content == null) return 0;
        return Math.min(0.08, Math.max(0, content.length() - 1200) / 20000.0);
    }

    private Set<String> queryTerms(String text) {
        Set<String> terms = new LinkedHashSet<>();
        if (text == null) {
            return terms;
        }
        String normalized = text.toLowerCase(Locale.ROOT);
        for (String token : normalized.split("[^a-z0-9\\u4e00-\\u9fa5]+")) {
            if (token.isEmpty()) continue;
            terms.add(token);
            addCjkTerms(terms, token);
        }
        return terms;
    }

    private void addCjkTerms(Set<String> terms, String token) {
        for (int i = 0; i < token.length(); i++) {
            char current = token.charAt(i);
            if (!isCjk(current)) continue;
            terms.add(String.valueOf(current));
            if (i + 1 < token.length() && isCjk(token.charAt(i + 1))) {
                terms.add("" + current + token.charAt(i + 1));
            }
        }
    }

    private boolean isCjk(char value) {
        return value >= '\u4e00' && value <= '\u9fa5';
    }

    private boolean externalEnabled() {
        return properties.getRerank().isExternalEnabled()
                && properties.getRerank().getUrl() != null
                && !properties.getRerank().getUrl().trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private double externalRerank(String query, String content, double vectorScore, double keywordScore) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("model", properties.getRerank().getModel());
        request.put("query", query);
        request.put("content", content);
        request.put("vectorScore", vectorScore);
        request.put("keywordScore", keywordScore);
        Map<String, Object> response = restTemplate().postForObject(properties.getRerank().getUrl(), request, Map.class);
        Object score = response != null ? response.get("score") : null;
        if (score == null) {
            throw new IllegalArgumentException("External rerank response missing score");
        }
        return Double.parseDouble(String.valueOf(score));
    }

    private RestTemplate restTemplate() {
        int timeout = properties.getRerank().getTimeoutMs() != null && properties.getRerank().getTimeoutMs() > 0
                ? properties.getRerank().getTimeoutMs()
                : 3000;
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeout);
        factory.setReadTimeout(timeout);
        return new RestTemplate(factory);
    }
}
