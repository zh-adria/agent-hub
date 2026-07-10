package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.EvaluationCaseResultEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface EvaluationCaseResultJpaRepository extends JpaRepository<EvaluationCaseResultEntity, Long> {
    List<EvaluationCaseResultEntity> findByRunIdAndTenantId(Long runId, Long tenantId);
}
