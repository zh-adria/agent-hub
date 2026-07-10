package com.agenthub.infra.persistence;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import com.agenthub.infra.persistence.entity.KnowledgeBaseEntity;
import com.agenthub.infra.persistence.entity.RagDocumentEntity;
import com.agenthub.infra.persistence.entity.VectorEmbeddingEntity;
import com.agenthub.infra.persistence.repository.DocumentChunkJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.RagDocumentJpaRepository;
import com.agenthub.infra.persistence.repository.VectorEmbeddingJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class RagEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private KnowledgeBaseJpaRepository knowledgeBaseRepository;

    @Autowired
    private RagDocumentJpaRepository documentRepository;

    @Autowired
    private DocumentChunkJpaRepository chunkRepository;

    @Autowired
    private VectorEmbeddingJpaRepository vectorRepository;

    @Test
    void persistsKnowledgeBaseDocumentAndChunks() {
        KnowledgeBaseEntity kb = new KnowledgeBaseEntity();
        kb.setTenantId(1L);
        kb.setName("policy-kb");
        kb.setDescription("Policy documents");
        kb.setEmbeddingProvider("llm-gateway");
        kb.setEmbeddingModel("text-embedding");
        kb.setChunkSize(800);
        kb.setChunkOverlap(120);
        kb.setStatus(1);
        kb.setCreatedBy("system");
        kb.setUpdatedBy("system");
        entityManager.persist(kb);

        RagDocumentEntity document = new RagDocumentEntity();
        document.setTenantId(1L);
        document.setKnowledgeBaseId(kb.getId());
        document.setTitle("handbook.md");
        document.setMimeType("text/markdown");
        document.setStatus(1);
        document.setCreatedBy("system");
        document.setUpdatedBy("system");
        entityManager.persist(document);

        DocumentChunkEntity chunk = new DocumentChunkEntity();
        chunk.setTenantId(1L);
        chunk.setKnowledgeBaseId(kb.getId());
        chunk.setDocumentId(document.getId());
        chunk.setChunkIndex(0);
        chunk.setContent("policy content");
        chunk.setTokenCount(2);
        entityManager.persist(chunk);

        VectorEmbeddingEntity embedding = new VectorEmbeddingEntity();
        embedding.setTenantId(1L);
        embedding.setKnowledgeBaseId(kb.getId());
        embedding.setDocumentId(document.getId());
        embedding.setChunkId(chunk.getId());
        embedding.setProvider("local");
        embedding.setModel("hashing-embedding-128");
        embedding.setDimension(3);
        embedding.setVector("[1.0,0.0,0.0]");
        entityManager.persist(embedding);
        entityManager.flush();

        assertThat(knowledgeBaseRepository.findByTenantId(1L)).hasSize(1);
        assertThat(documentRepository.findByKnowledgeBaseIdAndTenantId(kb.getId(), 1L)).hasSize(1);

        List<DocumentChunkEntity> chunks = chunkRepository.findByDocumentIdAndTenantIdOrderByChunkIndexAsc(document.getId(), 1L);
        assertThat(chunks).hasSize(1);
        assertThat(chunks.get(0).getContent()).isEqualTo("policy content");
        assertThat(vectorRepository.findByKnowledgeBaseIdAndTenantId(kb.getId(), 1L)).hasSize(1);
    }
}
