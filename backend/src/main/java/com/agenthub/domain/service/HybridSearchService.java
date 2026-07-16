package com.agenthub.domain.service;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import com.agenthub.infra.persistence.repository.DocumentChunkJpaRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class HybridSearchService {

    private final VectorSearchService vectorSearchService;
    private final DocumentChunkJpaRepository chunkRepository;
    private final RerankService rerankService;
    private final ObjectMapper objectMapper;

    public HybridSearchService(
            VectorSearchService vectorSearchService,
            DocumentChunkJpaRepository chunkRepository,
            RerankService rerankService,
            ObjectMapper objectMapper) {
        this.vectorSearchService = vectorSearchService;
        this.chunkRepository = chunkRepository;
        this.rerankService = rerankService;
        this.objectMapper = objectMapper;
    }

    public List<Map<String, Object>> search(Long tenantId, Long knowledgeBaseId, String query, int topK) {
        return search(tenantId, knowledgeBaseId, query, topK, new ArrayList<>());
    }

    public List<Map<String, Object>> search(Long tenantId, Long knowledgeBaseId, String query, int topK, List<String> accessTags) {
        int candidateLimit = Math.max(topK * 3, topK);
        Map<Long, Candidate> candidates = new LinkedHashMap<>();

        for (Map<String, Object> hit : vectorSearchService.search(tenantId, knowledgeBaseId, query, candidateLimit)) {
            Long chunkId = longValue(hit.get("chunkId"));
            Candidate candidate = candidates.computeIfAbsent(chunkId, id -> Candidate.fromVector(hit));
            candidate.vectorScore = doubleValue(hit.get("score"));
        }

        for (String keyword : keywords(query)) {
            for (DocumentChunkEntity chunk : chunkRepository.searchByKeyword(knowledgeBaseId, tenantId, keyword)) {
                Candidate candidate = candidates.computeIfAbsent(chunk.getId(), id -> Candidate.fromChunk(chunk));
                candidate.keywordScore = Math.max(candidate.keywordScore, rerankService.keywordScore(query, chunk.getContent()));
            }
        }

        return candidates.values().stream()
                .filter(candidate -> canAccess(candidate.metadata, accessTags))
                .peek(candidate -> candidate.score = rerankService.rerank(query, candidate.content, candidate.vectorScore, candidate.keywordScore))
                .sorted(Comparator.comparingDouble((Candidate item) -> item.score).reversed())
                .limit(topK)
                .map(Candidate::toResponse)
                .collect(Collectors.toList());
    }

    private List<String> keywords(String query) {
        List<String> terms = new ArrayList<>();
        if (query == null) {
            return terms;
        }
        for (String token : query.toLowerCase(Locale.ROOT).split("[^a-z0-9\\u4e00-\\u9fa5]+")) {
            if (!token.isEmpty()) {
                terms.add(token);
            }
        }
        return terms;
    }

    private Long longValue(Object value) {
        if (value instanceof Number) return ((Number) value).longValue();
        return Long.parseLong(String.valueOf(value));
    }

    private double doubleValue(Object value) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        return Double.parseDouble(String.valueOf(value));
    }

    private boolean canAccess(String metadata, List<String> accessTags) {
        Set<String> required = metadataAccessTags(metadata);
        if (required.isEmpty()) {
            return true;
        }
        Set<String> allowed = new HashSet<>();
        if (accessTags != null) {
            for (String tag : accessTags) {
                if (tag != null && !tag.trim().isEmpty()) {
                    allowed.add(tag.trim());
                }
            }
        }
        for (String tag : required) {
            if (allowed.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> metadataAccessTags(String metadata) {
        Set<String> result = new HashSet<>();
        if (metadata == null || metadata.trim().isEmpty()) {
            return result;
        }
        try {
            Map<String, Object> value = objectMapper.readValue(metadata, new TypeReference<Map<String, Object>>() {});
            Object tags = value.get("accessTags");
            if (tags instanceof Iterable) {
                for (Object tag : (Iterable<?>) tags) {
                    if (tag != null && !String.valueOf(tag).trim().isEmpty()) {
                        result.add(String.valueOf(tag).trim());
                    }
                }
            }
        } catch (Exception ignored) {
            return result;
        }
        return result;
    }

    private static class Candidate {
        private Long chunkId;
        private Long knowledgeBaseId;
        private Long documentId;
        private Integer chunkIndex;
        private String content;
        private String embeddingId;
        private String metadata;
        private double vectorScore;
        private double keywordScore;
        private double score;

        private static Candidate fromVector(Map<String, Object> hit) {
            Candidate candidate = new Candidate();
            candidate.chunkId = longValueStatic(hit.get("chunkId"));
            candidate.knowledgeBaseId = longValueStatic(hit.get("knowledgeBaseId"));
            candidate.documentId = longValueStatic(hit.get("documentId"));
            candidate.chunkIndex = ((Number) hit.get("chunkIndex")).intValue();
            candidate.content = String.valueOf(hit.get("content"));
            candidate.embeddingId = String.valueOf(hit.get("embeddingId"));
            candidate.metadata = hit.get("metadata") != null ? String.valueOf(hit.get("metadata")) : null;
            return candidate;
        }

        private static Candidate fromChunk(DocumentChunkEntity chunk) {
            Candidate candidate = new Candidate();
            candidate.chunkId = chunk.getId();
            candidate.knowledgeBaseId = chunk.getKnowledgeBaseId();
            candidate.documentId = chunk.getDocumentId();
            candidate.chunkIndex = chunk.getChunkIndex();
            candidate.content = chunk.getContent();
            candidate.embeddingId = chunk.getEmbeddingId();
            candidate.metadata = chunk.getMetadata();
            return candidate;
        }

        private Map<String, Object> toResponse() {
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("score", score);
            response.put("vectorScore", vectorScore);
            response.put("keywordScore", keywordScore);
            response.put("chunkId", chunkId);
            response.put("knowledgeBaseId", knowledgeBaseId);
            response.put("documentId", documentId);
            response.put("chunkIndex", chunkIndex);
            response.put("content", content);
            response.put("embeddingId", embeddingId);
            response.put("metadata", metadata);
            return response;
        }

        private static Long longValueStatic(Object value) {
            if (value instanceof Number) return ((Number) value).longValue();
            return Long.parseLong(String.valueOf(value));
        }
    }
}
