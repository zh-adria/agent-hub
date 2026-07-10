package com.agenthub.domain.repository;

import com.agenthub.domain.model.Agent;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    boolean existsById(String id);
    Agent save(Agent agent);
    Optional<Agent> findById(String id);
    List<Agent> findAll();
    void deleteById(String id);
}
