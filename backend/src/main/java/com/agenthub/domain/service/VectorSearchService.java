package com.agenthub.domain.service;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import com.agenthub.infra.persistence.entity.VectorEmbeddingEntity;
import com.agenthub.infra.persistence.repository.DocumentChunkJpaRepository;
import com.agenthub.infra.persistence.repository.VectorEmbeddingJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class VectorSearchService {

    private final EmbeddingService embeddingService;
    private final VectorEmbeddingJpaRepository vectorRepository;
    private final DocumentChunkJpaRepository chunkRepository;
    private final ObjectMapper objectMapper;
    private final MilvusVectorStoreAdapter milvusVectorStoreAdapter;

    public VectorSearchService(
            EmbeddingService embeddingService,
            VectorEmbeddingJpaRepository vectorRepository,
            DocumentChunkJpaRepository chunkRepository,
            ObjectMapper objectMapper,
            MilvusVectorStoreAdapter milvusVectorStoreAdapter) {
        this.embeddingService = embeddingService;
        this.vectorRepository = vectorRepository;
        this.chunkRepository = chunkRepository;
        this.objectMapper = objectMapper;
        this.milvusVectorStoreAdapter = milvusVectorStoreAdapter;
    }

    public VectorEmbeddingEntity indexChunk(Long tenantId, DocumentChunkEntity chunk) {
        float[] vector = embeddingService.embed(chunk.getContent());
        if (milvusVectorStoreAdapter.enabled()) {
            try {
                milvusVectorStoreAdapter.upsert(tenantId, chunk, vector, embeddingService.provider(), embeddingService.model());
            } catch (Exception ignored) {
                // Keep local vector table as the durable fallback.
            }
        }
        VectorEmbeddingEntity entity = vectorRepository.findByChunkIdAndTenantId(chunk.getId(), tenantId)
                .orElse(new VectorEmbeddingEntity());
        entity.setTenantId(tenantId);
        entity.setKnowledgeBaseId(chunk.getKnowledgeBaseId());
        entity.setDocumentId(chunk.getDocumentId());
        entity.setChunkId(chunk.getId());
        entity.setProvider(embeddingService.provider());
        entity.setModel(embeddingService.model());
        entity.setDimension(vector.length);
        entity.setVector(toJson(vector));
        return vectorRepository.save(entity);
    }

    public List<Map<String, Object>> search(Long tenantId, Long knowledgeBaseId, String query, int topK) {
        float[] queryVector = embeddingService.embed(query);
        if (milvusVectorStoreAdapter.enabled()) {
            try {
                List<Map<String, Object>> hits = milvusVectorStoreAdapter.search(tenantId, knowledgeBaseId, queryVector, topK);
                if (!hits.isEmpty()) {
                    return hydrateMilvusHits(hits, tenantId);
                }
            } catch (Exception ignored) {
                // Fall through to JPA vector table.
            }
        }
        return vectorRepository.findByKnowledgeBaseIdAndTenantId(knowledgeBaseId, tenantId).stream()
                .map(item -> score(item, queryVector))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingDouble((SearchHit hit) -> hit.score).reversed())
                .limit(topK)
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    private List<Map<String, Object>> hydrateMilvusHits(List<Map<String, Object>> hits, Long tenantId) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> hit : hits) {
            Long chunkId = longValue(hit.get("chunkId"));
            Optional<DocumentChunkEntity> chunk = chunkRepository.findById(chunkId)
                    .filter(item -> tenantId.equals(item.getTenantId()));
            if (!chunk.isPresent()) {
                continue;
            }
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("score", doubleValue(hit.get("score")));
            response.put("chunkId", chunk.get().getId());
            response.put("knowledgeBaseId", chunk.get().getKnowledgeBaseId());
            response.put("documentId", chunk.get().getDocumentId());
            response.put("chunkIndex", chunk.get().getChunkIndex());
            response.put("content", chunk.get().getContent());
            response.put("embeddingId", String.valueOf(hit.get("embeddingId")));
            result.add(response);
        }
        return result;
    }

    private Optional<SearchHit> score(VectorEmbeddingEntity embedding, float[] queryVector) {
        Optional<DocumentChunkEntity> chunk = chunkRepository.findById(embedding.getChunkId());
        if (!chunk.isPresent()) {
            return Optional.empty();
        }
        float[] vector = fromJson(embedding.getVector());
        return Optional.of(new SearchHit(chunk.get(), cosine(queryVector, vector), embedding));
    }

    private Map<String, Object> toResponse(SearchHit hit) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("score", hit.score);
        response.put("chunkId", hit.chunk.getId());
        response.put("knowledgeBaseId", hit.chunk.getKnowledgeBaseId());
        response.put("documentId", hit.chunk.getDocumentId());
        response.put("chunkIndex", hit.chunk.getChunkIndex());
        response.put("content", hit.chunk.getContent());
        response.put("embeddingId", String.valueOf(hit.embedding.getId()));
        return response;
    }

    private double cosine(float[] left, float[] right) {
        int length = Math.min(left.length, right.length);
        double dot = 0;
        double leftNorm = 0;
        double rightNorm = 0;
        for (int i = 0; i < length; i++) {
            dot += left[i] * right[i];
            leftNorm += left[i] * left[i];
            rightNorm += right[i] * right[i];
        }
        if (leftNorm == 0 || rightNorm == 0) {
            return 0;
        }
        return dot / (Math.sqrt(leftNorm) * Math.sqrt(rightNorm));
    }

    private Long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private double doubleValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private String toJson(float[] vector) {
        List<Float> values = new ArrayList<>();
        for (float value : vector) {
            values.add(value);
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to serialize vector", ex);
        }
    }

    private float[] fromJson(String vector) {
        try {
            List<Float> values = objectMapper.readValue(vector, new TypeReference<List<Float>>() {});
            float[] result = new float[values.size()];
            for (int i = 0; i < values.size(); i++) {
                result[i] = values.get(i);
            }
            return result;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Failed to parse vector", ex);
        }
    }

    private static class SearchHit {
        private final DocumentChunkEntity chunk;
        private final double score;
        private final VectorEmbeddingEntity embedding;

        private SearchHit(DocumentChunkEntity chunk, double score, VectorEmbeddingEntity embedding) {
            this.chunk = chunk;
            this.score = score;
            this.embedding = embedding;
        }
    }
}
