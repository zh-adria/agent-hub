package com.agenthub.client.api;

import com.agenthub.domain.service.AiIntegrationProperties;
import com.agenthub.domain.service.MilvusVectorStoreAdapter;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
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
    private final StringRedisTemplate redisTemplate;
    private final RedisProperties redisProperties;

    public HealthController(AiIntegrationProperties aiIntegrationProperties,
                            MilvusVectorStoreAdapter milvusVectorStoreAdapter,
                            DataSource dataSource,
                            StringRedisTemplate redisTemplate,
                            RedisProperties redisProperties) {
        this.aiIntegrationProperties = aiIntegrationProperties;
        this.milvusVectorStoreAdapter = milvusVectorStoreAdapter;
        this.dataSource = dataSource;
        this.redisTemplate = redisTemplate;
        this.redisProperties = redisProperties;
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
        integrations.put("milvusCollectionPrefix", aiIntegrationProperties.getMilvus().getCollectionPrefix());
        integrations.put("milvusCollectionStrategy", aiIntegrationProperties.getMilvus().getCollectionPrefix() + "_t{tenantId}_kb{knowledgeBaseId}");
        integrations.put("milvusPartitionStrategy", "collection-per-tenant-knowledge-base");
        integrations.put("redisHost", redisProperties.getHost());
        integrations.put("redisPort", redisProperties.getPort());
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
        boolean redisReady = redisReady();
        boolean milvusReady = !aiIntegrationProperties.getMilvus().isEnabled()
                || milvusVectorStoreAdapter.available()
                || aiIntegrationProperties.getMilvus().isFallbackOnFailure();
        Map<String, Object> status = new HashMap<>();
        status.put("status", databaseReady && redisReady && milvusReady ? "UP" : "DOWN");
        status.put("database", databaseReady);
        status.put("redis", redisReady);
        status.put("redisHost", redisProperties.getHost());
        status.put("redisPort", redisProperties.getPort());
        status.put("milvusConfigured", aiIntegrationProperties.getMilvus().isEnabled());
        status.put("milvusAvailable", milvusVectorStoreAdapter.available());
        status.put("milvusFallbackOnFailure", aiIntegrationProperties.getMilvus().isFallbackOnFailure());
        status.put("milvusCollectionStrategy", aiIntegrationProperties.getMilvus().getCollectionPrefix() + "_t{tenantId}_kb{knowledgeBaseId}");
        return status;
    }

    private boolean databaseReady() {
        try (Connection connection = dataSource.getConnection()) {
            return connection.isValid(2);
        } catch (Exception ex) {
            return false;
        }
    }

    private boolean redisReady() {
        try {
            RedisCallback<Boolean> callback = connection -> "PONG".equalsIgnoreCase(connection.ping());
            return Boolean.TRUE.equals(redisTemplate.execute(callback));
        } catch (Exception ex) {
            return false;
        }
    }
}
