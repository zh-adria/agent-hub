package com.agenthub.client.assembler;

import com.agenthub.domain.aggregate.Agent;
import com.agenthub.client.dto.AgentDTO;

public class AgentAssembler {
    public static AgentDTO toDTO(Agent agent) {
        AgentDTO dto = new AgentDTO();
        dto.setId(agent.getId());
        dto.setName(agent.getName());
        return dto;
    }
}
