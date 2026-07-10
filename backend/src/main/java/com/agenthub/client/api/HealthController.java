package com.agenthub.client.api;

import com.agenthub.domain.service.AiIntegrationProperties;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final AiIntegrationProperties aiIntegrationProperties;

    public HealthController(AiIntegrationProperties aiIntegrationProperties) {
        this.aiIntegrationProperties = aiIntegrationProperties;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "agent-hub-backend");
        Map<String, Object> integrations = new HashMap<>();
        integrations.put("externalEmbedding", aiIntegrationProperties.getEmbedding().isExternalEnabled());
        integrations.put("externalRerank", aiIntegrationProperties.getRerank().isExternalEnabled());
        integrations.put("milvus", aiIntegrationProperties.getMilvus().isEnabled());
        status.put("integrations", integrations);
        return status;
    }
}
