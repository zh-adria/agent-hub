package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.KnowledgeBaseEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface KnowledgeBaseJpaRepository extends JpaRepository<KnowledgeBaseEntity, Long> {
    Optional<KnowledgeBaseEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<KnowledgeBaseEntity> findByTenantId(Long tenantId);
    List<KnowledgeBaseEntity> findByTenantIdAndStatus(Long tenantId, Integer status);
}
