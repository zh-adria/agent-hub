package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.StepRecordEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StepRecordJpaRepository extends JpaRepository<StepRecordEntity, Long> {
    List<StepRecordEntity> findByTraceIdAndTenantIdOrderByStartedAtAsc(String traceId, Long tenantId);
    long countByTenantId(Long tenantId);
    long countByTenantIdAndStatus(Long tenantId, String status);
}
