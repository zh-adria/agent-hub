package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.RagDocumentEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RagDocumentJpaRepository extends JpaRepository<RagDocumentEntity, Long> {
    Optional<RagDocumentEntity> findByIdAndKnowledgeBaseIdAndTenantId(Long id, Long knowledgeBaseId, Long tenantId);
    List<RagDocumentEntity> findByKnowledgeBaseIdAndTenantId(Long knowledgeBaseId, Long tenantId);
}
