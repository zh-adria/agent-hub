package com.agenthub.domain.service;

import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Locale;
import java.util.Map;

@Service
public class HashingEmbeddingService implements EmbeddingService {

    private static final int DIMENSION = 128;
    private final AiIntegrationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public HashingEmbeddingService(AiIntegrationProperties properties) {
        this.properties = properties;
    }

    @Override
    public float[] embed(String text) {
        if (externalEnabled()) {
            try {
                float[] external = externalEmbed(text);
                if (external.length > 0) {
                    return external;
                }
            } catch (Exception ignored) {
                // Local hashing keeps RAG usable when the external gateway is unavailable.
            }
        }
        return localEmbed(text);
    }

    private float[] localEmbed(String text) {
        float[] vector = new float[DIMENSION];
        String normalized = text == null ? "" : text.toLowerCase(Locale.ROOT);
        String[] tokens = normalized.split("[^a-z0-9\\u4e00-\\u9fa5]+");
        for (String token : tokens) {
            if (token.isEmpty()) continue;
            addFeature(vector, token, 1.0f);
            addCjkFeatures(vector, token);
        }
        normalize(vector);
        return vector;
    }

    @Override
    public String provider() {
        if (externalEnabled()) {
            return properties.getEmbedding().getProvider();
        }
        return "local";
    }

    @Override
    public String model() {
        if (externalEnabled()) {
            return properties.getEmbedding().getModel();
        }
        return "hashing-embedding-128";
    }

    private boolean externalEnabled() {
        return properties.getEmbedding().isExternalEnabled()
                && properties.getEmbedding().getUrl() != null
                && !properties.getEmbedding().getUrl().trim().isEmpty();
    }

    @SuppressWarnings("unchecked")
    private float[] externalEmbed(String text) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("input", text != null ? text : "");
        request.put("model", properties.getEmbedding().getModel());
        Map<String, Object> response = restTemplate.postForObject(properties.getEmbedding().getUrl(), request, Map.class);
        Object embedding = response != null ? response.get("embedding") : null;
        if (embedding == null && response != null && response.get("data") instanceof List) {
            List<Object> data = (List<Object>) response.get("data");
            if (!data.isEmpty() && data.get(0) instanceof Map) {
                embedding = ((Map<String, Object>) data.get(0)).get("embedding");
            }
        }
        if (!(embedding instanceof List)) {
            return new float[0];
        }
        List<Object> values = new ArrayList<>((List<Object>) embedding);
        float[] result = new float[values.size()];
        for (int i = 0; i < values.size(); i++) {
            result[i] = Float.parseFloat(String.valueOf(values.get(i)));
        }
        return result;
    }

    private int bucket(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            int value = ((bytes[0] & 0xff) << 8) | (bytes[1] & 0xff);
            return value % DIMENSION;
        } catch (Exception ex) {
            return Math.abs(token.hashCode()) % DIMENSION;
        }
    }

    private void addFeature(float[] vector, String feature, float weight) {
        vector[bucket(feature)] += weight;
    }

    private void addCjkFeatures(float[] vector, String token) {
        ListCharacterView chars = new ListCharacterView(token);
        for (int i = 0; i < chars.length(); i++) {
            char current = chars.charAt(i);
            if (!isCjk(current)) continue;
            addFeature(vector, String.valueOf(current), 0.6f);
            if (i + 1 < chars.length() && isCjk(chars.charAt(i + 1))) {
                addFeature(vector, "" + current + chars.charAt(i + 1), 0.8f);
            }
        }
    }

    private boolean isCjk(char value) {
        return value >= '\u4e00' && value <= '\u9fa5';
    }

    private void normalize(float[] vector) {
        double sum = 0;
        for (float value : vector) {
            sum += value * value;
        }
        if (sum == 0) return;
        float norm = (float) Math.sqrt(sum);
        for (int i = 0; i < vector.length; i++) {
            vector[i] = vector[i] / norm;
        }
    }

    private static class ListCharacterView {
        private final String value;

        private ListCharacterView(String value) {
            this.value = value;
        }

        private int length() {
            return value.length();
        }

        private char charAt(int index) {
            return value.charAt(index);
        }
    }
}
