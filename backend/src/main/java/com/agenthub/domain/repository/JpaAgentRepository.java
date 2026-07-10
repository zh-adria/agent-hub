package com.agenthub.domain.repository;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.AgentEntity;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Repository
public class JpaAgentRepository implements AgentRepository {

    private final AgentJpaRepository jpaRepository;

    public JpaAgentRepository(AgentJpaRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }

    @Override
    public boolean existsById(String id) {
        return jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId()).isPresent();
    }

    @Override
    public Agent save(Agent agent) {
        AgentEntity entity = toEntity(agent);
        AgentEntity saved = jpaRepository.save(entity);
        return toDomain(saved);
    }

    @Override
    public Optional<Agent> findById(String id) {
        return jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId()).map(this::toDomain);
    }

    @Override
    public List<Agent> findAll() {
        List<Agent> result = new ArrayList<>();
        for (AgentEntity entity : jpaRepository.findByTenantId(TenantContext.tenantId())) {
            result.add(toDomain(entity));
        }
        return result;
    }

    @Override
    public void deleteById(String id) {
        jpaRepository.findByIdAndTenantId(Long.parseLong(id), TenantContext.tenantId())
                .ifPresent(jpaRepository::delete);
    }

    private AgentEntity toEntity(Agent domain) {
        AgentEntity entity = domain.getId() != null
                ? jpaRepository.findByIdAndTenantId(Long.parseLong(domain.getId()), TenantContext.tenantId()).orElse(new AgentEntity())
                : new AgentEntity();
        if (domain.getId() != null) {
            entity.setId(Long.parseLong(domain.getId()));
        }
        entity.setTenantId(TenantContext.tenantId());
        entity.setName(domain.getName());
        entity.setDescription(domain.getDescription());
        entity.setSystemPrompt(domain.getPrompt() != null ? domain.getPrompt() : "");
        entity.setLlmProvider(domain.getProvider() != null ? domain.getProvider() : "openai");
        entity.setLlmModel(domain.getModel() != null ? domain.getModel() : "gpt-4o-mini");
        entity.setTemperature(domain.getTemperature() != null ? domain.getTemperature() : 0.7);
        entity.setMaxTokens(domain.getMaxTokens() != null ? domain.getMaxTokens() : 2048);
        entity.setTools(domain.getFunctionIds() != null ? domain.getFunctionIds() : "[]");
        entity.setStatus(1);
        entity.setVersion(entity.getVersion() != null ? entity.getVersion() : 1);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");
        return entity;
    }

    private Agent toDomain(AgentEntity entity) {
        Agent agent = new Agent();
        agent.setId(String.valueOf(entity.getId()));
        agent.setName(entity.getName());
        agent.setDescription(entity.getDescription());
        agent.setPrompt(entity.getSystemPrompt());
        agent.setProvider(entity.getLlmProvider());
        agent.setModel(entity.getLlmModel());
        agent.setTemperature(entity.getTemperature());
        agent.setMaxTokens(entity.getMaxTokens());
        agent.setFunctionIds(entity.getTools());
        return agent;
    }
}
