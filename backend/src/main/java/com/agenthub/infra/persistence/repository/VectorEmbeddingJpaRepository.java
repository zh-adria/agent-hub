package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.VectorEmbeddingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface VectorEmbeddingJpaRepository extends JpaRepository<VectorEmbeddingEntity, Long> {
    List<VectorEmbeddingEntity> findByKnowledgeBaseIdAndTenantId(Long knowledgeBaseId, Long tenantId);
    Optional<VectorEmbeddingEntity> findByChunkIdAndTenantId(Long chunkId, Long tenantId);
}
