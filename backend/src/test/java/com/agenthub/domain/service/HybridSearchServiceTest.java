package com.agenthub.domain.service;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import com.agenthub.infra.persistence.repository.DocumentChunkJpaRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HybridSearchServiceTest {

    @Test
    void filtersRestrictedChunksByAccessTags() {
        VectorSearchService vectorSearchService = mock(VectorSearchService.class);
        DocumentChunkJpaRepository chunkRepository = mock(DocumentChunkJpaRepository.class);
        RerankService rerankService = mock(RerankService.class);
        HybridSearchService service = new HybridSearchService(
                vectorSearchService,
                chunkRepository,
                rerankService,
                new ObjectMapper());
        when(vectorSearchService.search(eq(1L), eq(10L), eq("policy"), anyInt())).thenReturn(Collections.emptyList());
        when(chunkRepository.searchByKeyword(10L, 1L, "policy"))
                .thenReturn(Arrays.asList(
                        chunk(1L, "public policy", null),
                        chunk(2L, "finance policy", "{\"accessTags\":[\"finance\"]}"),
                        chunk(3L, "legal policy", "{\"accessTags\":[\"legal\"]}")));
        when(rerankService.keywordScore("policy", "public policy")).thenReturn(1.0);
        when(rerankService.keywordScore("policy", "finance policy")).thenReturn(1.0);
        when(rerankService.keywordScore("policy", "legal policy")).thenReturn(1.0);
        when(rerankService.rerank("policy", "public policy", 0.0, 1.0)).thenReturn(1.0);
        when(rerankService.rerank("policy", "finance policy", 0.0, 1.0)).thenReturn(1.0);
        when(rerankService.rerank("policy", "legal policy", 0.0, 1.0)).thenReturn(1.0);

        List<Map<String, Object>> noTags = service.search(1L, 10L, "policy", 2, Collections.emptyList());
        List<Map<String, Object>> finance = service.search(1L, 10L, "policy", 3, Collections.singletonList("finance"));

        assertThat(noTags)
                .extracting(item -> item.get("chunkId"))
                .containsExactly(1L);
        assertThat(finance)
                .extracting(item -> item.get("chunkId"))
                .containsExactly(1L, 2L);
    }

    private DocumentChunkEntity chunk(Long id, String content, String metadata) {
        DocumentChunkEntity chunk = new DocumentChunkEntity();
        chunk.setId(id);
        chunk.setTenantId(1L);
        chunk.setKnowledgeBaseId(10L);
        chunk.setDocumentId(100L + id);
        chunk.setChunkIndex(id.intValue());
        chunk.setContent(content);
        chunk.setEmbeddingId(String.valueOf(id));
        chunk.setMetadata(metadata);
        return chunk;
    }
}
