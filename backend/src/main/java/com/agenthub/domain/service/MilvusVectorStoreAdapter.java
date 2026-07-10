package com.agenthub.domain.service;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Component
public class MilvusVectorStoreAdapter {
    private final AiIntegrationProperties properties;
    private final RestTemplate restTemplate = new RestTemplate();

    public MilvusVectorStoreAdapter(AiIntegrationProperties properties) {
        this.properties = properties;
    }

    public boolean enabled() {
        return properties.getMilvus().isEnabled()
                && properties.getMilvus().getUrl() != null
                && !properties.getMilvus().getUrl().trim().isEmpty();
    }

    public void upsert(Long tenantId, DocumentChunkEntity chunk, float[] vector, String provider, String model) {
        if (!enabled()) {
            return;
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenantId", tenantId);
        request.put("collection", collectionName(tenantId, chunk.getKnowledgeBaseId()));
        request.put("knowledgeBaseId", chunk.getKnowledgeBaseId());
        request.put("documentId", chunk.getDocumentId());
        request.put("chunkId", chunk.getId());
        request.put("provider", provider);
        request.put("model", model);
        request.put("vector", toList(vector));
        restTemplate.postForObject(properties.getMilvus().getUrl() + "/vectors/upsert", request, Map.class);
    }

    @SuppressWarnings("unchecked")
    public List<Map<String, Object>> search(Long tenantId, Long knowledgeBaseId, float[] queryVector, int topK) {
        if (!enabled()) {
            return new ArrayList<>();
        }
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("tenantId", tenantId);
        request.put("collection", collectionName(tenantId, knowledgeBaseId));
        request.put("knowledgeBaseId", knowledgeBaseId);
        request.put("topK", topK);
        request.put("vector", toList(queryVector));
        Map<String, Object> response = restTemplate.postForObject(properties.getMilvus().getUrl() + "/vectors/search", request, Map.class);
        Object hits = response != null ? response.get("hits") : null;
        if (hits instanceof List) {
            return (List<Map<String, Object>>) hits;
        }
        return new ArrayList<>();
    }

    private String collectionName(Long tenantId, Long knowledgeBaseId) {
        return properties.getMilvus().getCollectionPrefix() + "_t" + tenantId + "_kb" + knowledgeBaseId;
    }

    private List<Float> toList(float[] vector) {
        List<Float> values = new ArrayList<>();
        for (float item : vector) {
            values.add(item);
        }
        return values;
    }
}
