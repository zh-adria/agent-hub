package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.FunctionDefinitionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface FunctionDefinitionJpaRepository extends JpaRepository<FunctionDefinitionEntity, Long> {
    Optional<FunctionDefinitionEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<FunctionDefinitionEntity> findByTenantId(Long tenantId);
    List<FunctionDefinitionEntity> findByTenantIdAndStatus(Long tenantId, Integer status);

    @Query("SELECT f FROM FunctionDefinitionEntity f WHERE f.tenantId = :tenantId AND (LOWER(f.name) LIKE LOWER(CONCAT('%', :keyword, '%')) OR LOWER(f.description) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    List<FunctionDefinitionEntity> search(Long tenantId, String keyword);
}
