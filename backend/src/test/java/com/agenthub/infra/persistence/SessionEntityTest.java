package com.agenthub.infra.persistence;

import com.agenthub.infra.persistence.entity.SessionEntity;
import com.agenthub.infra.persistence.repository.SessionJpaRepository;
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
class SessionEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private SessionJpaRepository jpaRepository;

    @Test
    void persistsAndFindsSession() {
        SessionEntity entity = new SessionEntity();
        entity.setId("session-1");
        entity.setTenantId(1L);
        entity.setAgentId(1L);
        entity.setUserId(1L);
        entity.setContext("{}");
        entity.setState("ACTIVE");
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");

        entityManager.persist(entity);
        entityManager.flush();

        Optional<SessionEntity> found = jpaRepository.findById("session-1");

        assertThat(found).isPresent();
        assertThat(found.get().getState()).isEqualTo("ACTIVE");
        assertThat(found.get().getAgentId()).isEqualTo(1L);
    }

    @Test
    void findByAgentId() {
        SessionEntity session = new SessionEntity();
        session.setId("session-2");
        session.setTenantId(1L);
        session.setAgentId(1L);
        session.setContext("{}");
        session.setState("ACTIVE");
        session.setCreatedBy("system");
        session.setUpdatedBy("system");

        entityManager.persist(session);
        entityManager.flush();

        List<SessionEntity> results = jpaRepository.findByAgentId(1L);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getId()).isEqualTo("session-2");
    }

    @Test
    void deleteByIdRemovesSession() {
        SessionEntity session = new SessionEntity();
        session.setId("session-3");
        session.setTenantId(1L);
        session.setAgentId(1L);
        session.setContext("{}");
        session.setState("ACTIVE");
        session.setCreatedBy("system");
        session.setUpdatedBy("system");

        entityManager.persist(session);
        entityManager.flush();

        jpaRepository.deleteById("session-3");

        assertThat(jpaRepository.findById("session-3")).isEmpty();
    }
}
