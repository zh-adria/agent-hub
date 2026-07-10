package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.SessionEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SessionJpaRepository extends JpaRepository<SessionEntity, String> {
    Optional<SessionEntity> findByIdAndTenantId(String id, Long tenantId);
    List<SessionEntity> findByTenantId(Long tenantId);
    List<SessionEntity> findByAgentId(Long agentId);
    List<SessionEntity> findByAgentIdAndTenantId(Long agentId, Long tenantId);
}
