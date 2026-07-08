package com.agenthub.client.api;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.service.AgentService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class AgentApiImpl implements AgentApi {
    private final AgentService agentService;
    
    public AgentApiImpl(AgentService agentService) {
        this.agentService = agentService;
    }
    
    @Override
    @PostMapping
    public Agent createAgent(@RequestBody Agent agent) {
        return agentService.createAgent(agent);
    }
    
    @Override
    @GetMapping
    public List<Agent> listAgents() {
        return agentService.listAgents();
    }
    
    @Override
    @GetMapping("/{id}")
    public Agent getAgent(@PathVariable String id) {
        return agentService.getAgent(id);
    }
    
    @Override
    @PutMapping("/{id}")
    public Agent updateAgent(@PathVariable String id, @RequestBody Agent agent) {
        return agentService.updateAgent(id, agent);
    }
    
    @Override
    @DeleteMapping("/{id}")
    public void deleteAgent(@PathVariable String id) {
        agentService.deleteAgent(id);
    }
}
