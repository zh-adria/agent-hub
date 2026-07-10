package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.TraceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface TraceJpaRepository extends JpaRepository<TraceEntity, String> {
    Optional<TraceEntity> findByIdAndTenantId(String id, Long tenantId);
    List<TraceEntity> findByTenantIdOrderByStartedAtDesc(Long tenantId);
}
