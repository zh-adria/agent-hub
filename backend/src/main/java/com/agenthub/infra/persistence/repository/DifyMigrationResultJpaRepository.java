package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.DifyMigrationResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DifyMigrationResultJpaRepository extends JpaRepository<DifyMigrationResultEntity, Long> {
    List<DifyMigrationResultEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, String status);
}
