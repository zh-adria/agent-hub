package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.AgentEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface AgentJpaRepository extends JpaRepository<AgentEntity, Long> {
    Optional<AgentEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<AgentEntity> findByTenantId(Long tenantId);
    List<AgentEntity> findByTenantIdAndStatus(Long tenantId, Integer status);
}
