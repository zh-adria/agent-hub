package com.agenthub.client.api;

import com.agenthub.domain.service.AiIntegrationProperties;
import com.agenthub.domain.service.MilvusVectorStoreAdapter;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class HealthControllerTest {

    @Test
    void readyExposesDatabaseRedisAndMilvusDependencyStatus() throws Exception {
        AiIntegrationProperties properties = new AiIntegrationProperties();
        properties.getMilvus().setEnabled(true);
        properties.getMilvus().setFallbackOnFailure(false);
        MilvusVectorStoreAdapter milvus = mock(MilvusVectorStoreAdapter.class);
        when(milvus.available()).thenReturn(true);
        DataSource dataSource = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.isValid(2)).thenReturn(true);
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        when(redisTemplate.execute(any(RedisCallback.class))).thenReturn(true);
        RedisProperties redisProperties = new RedisProperties();
        redisProperties.setHost("redis.prod");
        redisProperties.setPort(6380);
        HealthController controller = new HealthController(
                properties,
                milvus,
                dataSource,
                redisTemplate,
                redisProperties);

        Map<String, Object> response = controller.ready();

        assertThat(response.get("status")).isEqualTo("UP");
        assertThat(response.get("database")).isEqualTo(true);
        assertThat(response.get("redis")).isEqualTo(true);
        assertThat(response.get("redisHost")).isEqualTo("redis.prod");
        assertThat(response.get("redisPort")).isEqualTo(6380);
        assertThat(response.get("milvusConfigured")).isEqualTo(true);
        assertThat(response.get("milvusAvailable")).isEqualTo(true);
    }
}
