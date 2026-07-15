package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.LLMUsageAuditEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LLMUsageAuditJpaRepository extends JpaRepository<LLMUsageAuditEntity, Long> {
    List<LLMUsageAuditEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);

    long countByTenantId(Long tenantId);
}
