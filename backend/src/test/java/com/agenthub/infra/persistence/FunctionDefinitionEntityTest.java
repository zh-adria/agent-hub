package com.agenthub.infra.persistence;

import com.agenthub.infra.persistence.entity.FunctionDefinitionEntity;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
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
class FunctionDefinitionEntityTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private FunctionDefinitionJpaRepository jpaRepository;

    @Test
    void persistsAndFindsFunction() {
        FunctionDefinitionEntity entity = new FunctionDefinitionEntity();
        entity.setTenantId(1L);
        entity.setName("weather_lookup");
        entity.setDescription("Query city weather");
        entity.setParameters("{\"city\":\"string\"}");
        entity.setStatus(1);
        entity.setCreatedBy("system");
        entity.setUpdatedBy("system");

        entityManager.persist(entity);
        entityManager.flush();

        Optional<FunctionDefinitionEntity> found = jpaRepository.findById(entity.getId());

        assertThat(found).isPresent();
        assertThat(found.get().getName()).isEqualTo("weather_lookup");
        assertThat(found.get().getDescription()).isEqualTo("Query city weather");
        assertThat(found.get().getParameters()).isEqualTo("{\"city\":\"string\"}");
    }

    @Test
    void searchByNameKeyword() {
        FunctionDefinitionEntity f1 = new FunctionDefinitionEntity();
        f1.setTenantId(1L);
        f1.setName("weather_forecast");
        f1.setDescription("7-day forecast");
        f1.setStatus(1);
        f1.setCreatedBy("system");
        f1.setUpdatedBy("system");

        FunctionDefinitionEntity f2 = new FunctionDefinitionEntity();
        f2.setTenantId(1L);
        f2.setName("stock_price");
        f2.setDescription("Get stock prices");
        f2.setStatus(1);
        f2.setCreatedBy("system");
        f2.setUpdatedBy("system");

        entityManager.persist(f1);
        entityManager.persist(f2);
        entityManager.flush();

        List<FunctionDefinitionEntity> results = jpaRepository.search(1L, "weather");

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("weather_forecast");
    }

    @Test
    void findByTenantIdAndStatus() {
        FunctionDefinitionEntity active = new FunctionDefinitionEntity();
        active.setTenantId(1L);
        active.setName("active_func");
        active.setStatus(1);
        active.setCreatedBy("system");
        active.setUpdatedBy("system");

        entityManager.persist(active);
        entityManager.flush();

        List<FunctionDefinitionEntity> results = jpaRepository.findByTenantIdAndStatus(1L, 1);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getName()).isEqualTo("active_func");
    }
}
