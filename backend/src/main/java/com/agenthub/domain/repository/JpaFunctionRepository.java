package com.agenthub.domain.repository;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.FunctionDefinitionEntity;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaFunctionRepository implements FunctionRepository {

    private final FunctionDefinitionJpaRepository jpaRepository;

    public JpaFunctionRepository(FunctionDefinitionJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId()).isPresent();
    }

    @Override
    public FunctionDefinition save(FunctionDefinition function) {
        FunctionDefinitionEntity entity = toEntity(function);
        FunctionDefinitionEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<FunctionDefinition> findById(String id) {
        return jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId()).map(this::toDomain);
    }

    @Override
    public List<FunctionDefinition> findAll() {
        List<FunctionDefinition> result = new ArrayList<>();
        for (FunctionDefinitionEntity entity : jpaRepository.findByTenantId(TenantContext.tenantId())) {
            result.add(toDomain(entity));
        }
        return result;
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId())
                .ifPresent(jpaRepository::delete);
    }

    @Override
    public List<FunctionDefinition> findByNameContainingOrDescriptionContaining(String nameKeyword, String descriptionKeyword) {
        List<FunctionDefinition> result = new ArrayList<>();
        for (FunctionDefinitionEntity entity : jpaRepository.search(TenantContext.tenantId(), nameKeyword)) {
            result.add(toDomain(entity));
        }
        return result;
    }

    private FunctionDefinitionEntity toEntity(FunctionDefinition domain) {
        FunctionDefinitionEntity entity = domain.getId() != null
                ? jpaRepository.findByIdAndTenantId(Long.parseLong(domain.getId()), TenantContext.tenantId()).orElse(new FunctionDefinitionEntity())
                : new FunctionDefinitionEntity();
        if (domain.getId() != null) {
            entity.setId(Long.parseLong(domain.getId()));
        }
        entity.setTenantId(TenantContext.tenantId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setEndpointUrl(domain.getEndpoint());
        entity.setProtocol(domain.getImplementation() != null ? domain.getImplementation() : "http");
        entity.setMethod(domain.getMethod() != null ? domain.getMethod() : "GET");
        entity.setImplementation(domain.getImplementation());
        entity.setParameters(domain.getParameters());
        entity.setTimeoutMs(domain.getTimeoutMs() != null ? domain.getTimeoutMs() : 30000);
        entity.setRetryPolicy(domain.getRetryPolicy());
        entity.setCircuitBreakerPolicy(domain.getCircuitBreakerPolicy());
        entity.setFallbackResponse(domain.getFallbackResponse());
        entity.setStatus(1);
        if (entity.getCreatedBy() == null) {
            entity.setCreatedBy(TenantContext.userId());
        }
        entity.setUpdatedBy(TenantContext.userId());
        return entity;
    }

    private FunctionDefinition toDomain(FunctionDefinitionEntity entity) {
        FunctionDefinition domain = new FunctionDefinition();
        domain.setId(String.valueOf(entity.getId()));
        domain.setName(entity.getName());
        domain.setDescription(entity.getDescription());
        domain.setEndpoint(entity.getEndpointUrl());
        domain.setMethod(entity.getMethod() != null ? entity.getMethod() : entity.getProtocol());
        domain.setParameters(entity.getParameters());
        domain.setTimeoutMs(entity.getTimeoutMs());
        domain.setImplementation(entity.getImplementation() != null ? entity.getImplementation() : entity.getProtocol());
        domain.setRetryPolicy(entity.getRetryPolicy());
        domain.setCircuitBreakerPolicy(entity.getCircuitBreakerPolicy());
        domain.setFallbackResponse(entity.getFallbackResponse());
        return domain;
    }
}
