package com.agenthub.infra.repository;

import com.agenthub.domain.aggregate.Agent;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentRepository extends JpaRepository<Agent, Long> {
}
