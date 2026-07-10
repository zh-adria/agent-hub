package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.EvaluationRunEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface EvaluationRunJpaRepository extends JpaRepository<EvaluationRunEntity, Long> {
    Optional<EvaluationRunEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<EvaluationRunEntity> findByTenantIdOrderByCreatedAtDesc(Long tenantId);
}
