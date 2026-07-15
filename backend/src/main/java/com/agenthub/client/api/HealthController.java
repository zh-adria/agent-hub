package com.agenthub.client.api;

import com.agenthub.domain.service.AiIntegrationProperties;
import com.agenthub.domain.service.MilvusVectorStoreAdapter;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    private final AiIntegrationProperties aiIntegrationProperties;
    private final MilvusVectorStoreAdapter milvusVectorStoreAdapter;
    private final DataSource dataSource;

    public HealthController(AiIntegrationProperties aiIntegrationProperties, MilvusVectorStoreAdapter milvusVectorStoreAdapter, DataSource dataSource) {
        this.aiIntegrationProperties = aiIntegrationProperties;
        this.milvusVectorStoreAdapter = milvusVectorStoreAdapter;
        this.dataSource = dataSource;
    }

    @GetMapping("/api/health")
    public Map<String, Object> health() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        status.put("service", "agent-hub-backend");
        Map<String, Object> integrations = new HashMap<>();
        integrations.put("externalEmbedding", aiIntegrationProperties.getEmbedding().isExternalEnabled());
        integrations.put("embeddingFallbackOnFailure", aiIntegrationProperties.getEmbedding().isFallbackOnFailure());
        integrations.put("externalRerank", aiIntegrationProperties.getRerank().isExternalEnabled());
        integrations.put("rerankFallbackOnFailure", aiIntegrationProperties.getRerank().isFallbackOnFailure());
        integrations.put("milvus", aiIntegrationProperties.getMilvus().isEnabled());
        integrations.put("milvusAvailable", milvusVectorStoreAdapter.available());
        integrations.put("milvusFallbackOnFailure", aiIntegrationProperties.getMilvus().isFallbackOnFailure());
        status.put("integrations", integrations);
        return status;
    }

    @GetMapping("/api/health/live")
    public Map<String, Object> live() {
        Map<String, Object> status = new HashMap<>();
        status.put("status", "UP");
        return status;
    }

    @GetMapping("/api/health/ready")
    public Map<String, Object> ready() {
        boolean databaseReady = databaseReady();
        Map<String, Object> status = new HashMap<>();
        status.put("status", databaseReady ? "UP" : "DOWN");
        status.put("database", databaseReady);
        status.put("milvusConfigured", aiIntegrationProperties.getMilvus().isEnabled());
        status.put("milvusAvailable", milvusVectorStoreAdapter.available());
        return status;
    }

    private boolean databaseReady() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }
}
