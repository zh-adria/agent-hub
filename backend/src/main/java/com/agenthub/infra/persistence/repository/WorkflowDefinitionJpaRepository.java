package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.WorkflowDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface WorkflowDefinitionJpaRepository extends JpaRepository<WorkflowDefinitionEntity, Long> {
    Optional<WorkflowDefinitionEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<WorkflowDefinitionEntity> findByTenantId(Long tenantId);
}
