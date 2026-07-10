package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.DocumentChunkEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DocumentChunkJpaRepository extends JpaRepository<DocumentChunkEntity, Long> {
    List<DocumentChunkEntity> findByDocumentIdAndTenantIdOrderByChunkIndexAsc(Long documentId, Long tenantId);
    List<DocumentChunkEntity> findByKnowledgeBaseIdAndTenantId(Long knowledgeBaseId, Long tenantId);

    @Query("SELECT c FROM DocumentChunkEntity c WHERE c.knowledgeBaseId = :knowledgeBaseId AND c.tenantId = :tenantId AND LOWER(c.content) LIKE LOWER(CONCAT('%', :keyword, '%'))")
    List<DocumentChunkEntity> searchByKeyword(Long knowledgeBaseId, Long tenantId, String keyword);
}
