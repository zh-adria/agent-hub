package com.agenthub.client.impl;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import com.agenthub.infra.persistence.entity.KnowledgeBaseEntity;
import com.agenthub.infra.persistence.entity.RagDocumentEntity;
import com.agenthub.domain.service.HybridSearchService;
import com.agenthub.domain.service.VectorSearchService;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.DocumentChunkJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.RagDocumentJpaRepository;
import com.agenthub.infra.persistence.repository.VectorEmbeddingJpaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/knowledge-bases")
public class RagApiImpl {

    private final KnowledgeBaseJpaRepository knowledgeBaseRepository;
    private final RagDocumentJpaRepository documentRepository;
    private final DocumentChunkJpaRepository chunkRepository;
    private final VectorEmbeddingJpaRepository vectorRepository;
    private final VectorSearchService vectorSearchService;
    private final HybridSearchService hybridSearchService;

    public RagApiImpl(
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            RagDocumentJpaRepository documentRepository,
            DocumentChunkJpaRepository chunkRepository,
            VectorEmbeddingJpaRepository vectorRepository,
            VectorSearchService vectorSearchService,
            HybridSearchService hybridSearchService) {
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.chunkRepository = chunkRepository;
        this.vectorRepository = vectorRepository;
        this.vectorSearchService = vectorSearchService;
        this.hybridSearchService = hybridSearchService;
    }

    @PostMapping
    public Map<String, Object> createKnowledgeBase(@RequestBody Map<String, Object> payload) {
        KnowledgeBaseEntity entity = new KnowledgeBaseEntity();
        entity.setTenantId(tenantId());
        entity.setName((String) payload.get("name"));
        entity.setDescription((String) payload.get("description"));
        entity.setEmbeddingProvider(stringValue(payload.get("embeddingProvider"), "llm-gateway"));
        entity.setEmbeddingModel(stringValue(payload.get("embeddingModel"), "text-embedding"));
        entity.setChunkSize(intValue(payload.get("chunkSize"), 800));
        entity.setChunkOverlap(intValue(payload.get("chunkOverlap"), 120));
        entity.setStatus(1);
        entity.setCreatedBy(TenantContext.userId());
        entity.setUpdatedBy(TenantContext.userId());
        return mapKnowledgeBase(knowledgeBaseRepository.save(entity));
    }

    @GetMapping
    public List<Map<String, Object>> listKnowledgeBases() {
        return knowledgeBaseRepository.findByTenantId(tenantId()).stream()
                .map(this::mapKnowledgeBase)
                .collect(Collectors.toList());
    }

    @GetMapping("/{knowledgeBaseId}")
    public Map<String, Object> getKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        return knowledgeBaseRepository.findByIdAndTenantId(knowledgeBaseId, tenantId())
                .map(this::mapKnowledgeBase)
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + knowledgeBaseId));
    }

    @PutMapping("/{knowledgeBaseId}")
    public Map<String, Object> updateKnowledgeBase(@PathVariable Long knowledgeBaseId, @RequestBody Map<String, Object> payload) {
        KnowledgeBaseEntity entity = knowledgeBaseRepository.findByIdAndTenantId(knowledgeBaseId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + knowledgeBaseId));
        if (payload.containsKey("name")) entity.setName((String) payload.get("name"));
        if (payload.containsKey("description")) entity.setDescription((String) payload.get("description"));
        if (payload.containsKey("embeddingProvider")) entity.setEmbeddingProvider((String) payload.get("embeddingProvider"));
        if (payload.containsKey("embeddingModel")) entity.setEmbeddingModel((String) payload.get("embeddingModel"));
        if (payload.containsKey("chunkSize")) entity.setChunkSize(intValue(payload.get("chunkSize"), entity.getChunkSize()));
        if (payload.containsKey("chunkOverlap")) entity.setChunkOverlap(intValue(payload.get("chunkOverlap"), entity.getChunkOverlap()));
        entity.setUpdatedBy(TenantContext.userId());
        return mapKnowledgeBase(knowledgeBaseRepository.save(entity));
    }

    @DeleteMapping("/{knowledgeBaseId}")
    public void deleteKnowledgeBase(@PathVariable Long knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        for (RagDocumentEntity document : documentRepository.findByKnowledgeBaseIdAndTenantId(knowledgeBaseId, tenantId())) {
            deleteDocumentCascade(document.getId());
        }
        knowledgeBaseRepository.deleteById(knowledgeBaseId);
    }

    @PostMapping("/{knowledgeBaseId}/documents")
    public Map<String, Object> createDocument(@PathVariable Long knowledgeBaseId, @RequestBody Map<String, Object> payload) {
        requireKnowledgeBase(knowledgeBaseId);
        RagDocumentEntity entity = new RagDocumentEntity();
        entity.setTenantId(tenantId());
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setTitle((String) payload.get("title"));
        entity.setSourceUri((String) payload.get("sourceUri"));
        entity.setMimeType(stringValue(payload.get("mimeType"), "text/plain"));
        entity.setContentHash((String) payload.get("contentHash"));
        entity.setStatus(1);
        entity.setCreatedBy(TenantContext.userId());
        entity.setUpdatedBy(TenantContext.userId());
        return mapDocument(documentRepository.save(entity));
    }

    @GetMapping("/{knowledgeBaseId}/documents")
    public List<Map<String, Object>> listDocuments(@PathVariable Long knowledgeBaseId) {
        requireKnowledgeBase(knowledgeBaseId);
        return documentRepository.findByKnowledgeBaseIdAndTenantId(knowledgeBaseId, tenantId()).stream()
                .map(this::mapDocument)
                .collect(Collectors.toList());
    }

    @PutMapping("/{knowledgeBaseId}/documents/{documentId}")
    public Map<String, Object> updateDocument(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> payload) {
        RagDocumentEntity entity = documentRepository.findByIdAndKnowledgeBaseIdAndTenantId(documentId, knowledgeBaseId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
        if (payload.containsKey("title")) entity.setTitle((String) payload.get("title"));
        if (payload.containsKey("sourceUri")) entity.setSourceUri((String) payload.get("sourceUri"));
        if (payload.containsKey("mimeType")) entity.setMimeType((String) payload.get("mimeType"));
        if (payload.containsKey("contentHash")) entity.setContentHash((String) payload.get("contentHash"));
        entity.setUpdatedBy(TenantContext.userId());
        return mapDocument(documentRepository.save(entity));
    }

    @DeleteMapping("/{knowledgeBaseId}/documents/{documentId}")
    public void deleteDocument(@PathVariable Long knowledgeBaseId, @PathVariable Long documentId) {
        requireDocument(knowledgeBaseId, documentId);
        deleteDocumentCascade(documentId);
    }

    @PostMapping("/{knowledgeBaseId}/documents/{documentId}/chunks")
    public Map<String, Object> createChunk(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId,
            @RequestBody Map<String, Object> payload) {
        requireDocument(knowledgeBaseId, documentId);
        DocumentChunkEntity entity = new DocumentChunkEntity();
        entity.setTenantId(tenantId());
        entity.setKnowledgeBaseId(knowledgeBaseId);
        entity.setDocumentId(documentId);
        entity.setChunkIndex(intValue(payload.get("chunkIndex"), nextChunkIndex(documentId)));
        entity.setContent((String) payload.get("content"));
        entity.setTokenCount(intValue(payload.get("tokenCount"), null));
        entity.setEmbeddingId((String) payload.get("embeddingId"));
        DocumentChunkEntity saved = chunkRepository.save(entity);
        if (saved.getEmbeddingId() == null || saved.getEmbeddingId().trim().isEmpty()) {
            saved.setEmbeddingId(String.valueOf(vectorSearchService.indexChunk(tenantId(), saved).getId()));
            saved = chunkRepository.save(saved);
        }
        return mapChunk(saved);
    }

    @GetMapping("/{knowledgeBaseId}/documents/{documentId}/chunks")
    public List<Map<String, Object>> listChunks(@PathVariable Long knowledgeBaseId, @PathVariable Long documentId) {
        requireDocument(knowledgeBaseId, documentId);
        return chunkRepository.findByDocumentIdAndTenantIdOrderByChunkIndexAsc(documentId, tenantId()).stream()
                .map(this::mapChunk)
                .collect(Collectors.toList());
    }

    @PutMapping("/{knowledgeBaseId}/documents/{documentId}/chunks/{chunkId}")
    public Map<String, Object> updateChunk(
            @PathVariable Long knowledgeBaseId,
            @PathVariable Long documentId,
            @PathVariable Long chunkId,
            @RequestBody Map<String, Object> payload) {
        requireDocument(knowledgeBaseId, documentId);
        DocumentChunkEntity entity = chunkRepository.findById(chunkId)
                .filter(chunk -> tenantId().equals(chunk.getTenantId()) && documentId.equals(chunk.getDocumentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found: " + chunkId));
        if (payload.containsKey("content")) entity.setContent((String) payload.get("content"));
        if (payload.containsKey("chunkIndex")) entity.setChunkIndex(intValue(payload.get("chunkIndex"), entity.getChunkIndex()));
        if (payload.containsKey("tokenCount")) entity.setTokenCount(intValue(payload.get("tokenCount"), entity.getTokenCount()));
        DocumentChunkEntity saved = chunkRepository.save(entity);
        saved.setEmbeddingId(String.valueOf(vectorSearchService.indexChunk(tenantId(), saved).getId()));
        return mapChunk(chunkRepository.save(saved));
    }

    @DeleteMapping("/{knowledgeBaseId}/documents/{documentId}/chunks/{chunkId}")
    public void deleteChunk(@PathVariable Long knowledgeBaseId, @PathVariable Long documentId, @PathVariable Long chunkId) {
        requireDocument(knowledgeBaseId, documentId);
        DocumentChunkEntity entity = chunkRepository.findById(chunkId)
                .filter(chunk -> tenantId().equals(chunk.getTenantId()) && documentId.equals(chunk.getDocumentId()))
                .orElseThrow(() -> new ResourceNotFoundException("Chunk not found: " + chunkId));
        deleteEmbeddingForChunk(entity.getId());
        chunkRepository.delete(entity);
    }

    @PostMapping("/{knowledgeBaseId}/search")
    public List<Map<String, Object>> search(@PathVariable Long knowledgeBaseId, @RequestBody Map<String, Object> payload) {
        requireKnowledgeBase(knowledgeBaseId);
        String query = stringValue(payload.get("query"), "");
        int topK = intValue(payload.get("topK"), 5);
        return hybridSearchService.search(tenantId(), knowledgeBaseId, query, topK);
    }

    private void requireKnowledgeBase(Long knowledgeBaseId) {
        knowledgeBaseRepository.findByIdAndTenantId(knowledgeBaseId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Knowledge base not found: " + knowledgeBaseId));
    }

    private void requireDocument(Long knowledgeBaseId, Long documentId) {
        documentRepository.findByIdAndKnowledgeBaseIdAndTenantId(documentId, knowledgeBaseId, tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Document not found: " + documentId));
    }

    private void deleteDocumentCascade(Long documentId) {
        for (DocumentChunkEntity chunk : chunkRepository.findByDocumentIdAndTenantIdOrderByChunkIndexAsc(documentId, tenantId())) {
            deleteEmbeddingForChunk(chunk.getId());
            chunkRepository.delete(chunk);
        }
        documentRepository.deleteById(documentId);
    }

    private void deleteEmbeddingForChunk(Long chunkId) {
        vectorRepository.findByChunkIdAndTenantId(chunkId, tenantId())
                .ifPresent(vectorRepository::delete);
    }

    private Integer nextChunkIndex(Long documentId) {
        return chunkRepository.findByDocumentIdAndTenantIdOrderByChunkIndexAsc(documentId, tenantId()).size();
    }

    private Long tenantId() {
        return TenantContext.tenantId();
    }

    private Map<String, Object> mapKnowledgeBase(KnowledgeBaseEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("name", entity.getName());
        response.put("description", entity.getDescription());
        response.put("embeddingProvider", entity.getEmbeddingProvider());
        response.put("embeddingModel", entity.getEmbeddingModel());
        response.put("chunkSize", entity.getChunkSize());
        response.put("chunkOverlap", entity.getChunkOverlap());
        response.put("status", entity.getStatus());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());
        return response;
    }

    private Map<String, Object> mapDocument(RagDocumentEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("knowledgeBaseId", entity.getKnowledgeBaseId());
        response.put("title", entity.getTitle());
        response.put("sourceUri", entity.getSourceUri());
        response.put("mimeType", entity.getMimeType());
        response.put("contentHash", entity.getContentHash());
        response.put("status", entity.getStatus());
        response.put("createdAt", entity.getCreatedAt());
        response.put("updatedAt", entity.getUpdatedAt());
        return response;
    }

    private Map<String, Object> mapChunk(DocumentChunkEntity entity) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", entity.getId());
        response.put("knowledgeBaseId", entity.getKnowledgeBaseId());
        response.put("documentId", entity.getDocumentId());
        response.put("chunkIndex", entity.getChunkIndex());
        response.put("content", entity.getContent());
        response.put("tokenCount", entity.getTokenCount());
        response.put("embeddingId", entity.getEmbeddingId());
        response.put("createdAt", entity.getCreatedAt());
        return response;
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) return Integer.parseInt((String) value);
        return fallback;
    }
}

