package com.agenthub.infra.persistence;

import com.agenthub.infra.persistence.entity.AgentEntity;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@ActiveProfiles("test")
class AgentEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private AgentJpaRepository jpaRepository;

    @Test
    void persistsAndFindsAgent() {
        AgentEntity entity = new AgentEntity();
        entity.setTenantId(1L);
        entity.setName("客服助手");
        entity.setDescription("处理客户咨询");
        entity.setSystemPrompt("你是一个专业的客服代表。");
        entity.setLlmProvider("openai");
        entity.setLlmModel("gpt-4o-mini");
        entity.setStatus(1);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");

        entityManager.persist(entity);
        entityManager.flush();

        Optional<AgentEntity> found = jpaRepository.findById(entity.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("客服助手");
        assertThat(found.get().getLlmModel()).isEqualTo("gpt-4o-mini");
        assertThat(found.get().getSystemPrompt()).isEqualTo("你是一个专业的客服代表。");
    }

    @Test
    void findByTenantId() {
        AgentEntity agent = new AgentEntity();
        agent.setTenantId(1L);
        agent.setName("test-agent");
        agent.setSystemPrompt("You are a helpful assistant.");
        agent.setLlmProvider("openai");
        agent.setLlmModel("gpt-4o-mini");
        agent.setStatus(1);
        agent.setCreatedBy("system");
        agent.setUpdatedBy("system");

        entityManager.persist(agent);
        entityManager.flush();

        List<AgentEntity> results = jpaRepository.findByTenantId(1L);
        assertThat(results).hasSize(1);
    }
}
