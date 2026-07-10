package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.BotBindingEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface BotBindingJpaRepository extends JpaRepository<BotBindingEntity, Long> {
    Optional<BotBindingEntity> findByIdAndTenantId(Long id, Long tenantId);
    List<BotBindingEntity> findByTenantId(Long tenantId);
    Optional<BotBindingEntity> findByTenantIdAndChannelAndChannelBotIdAndStatus(Long tenantId, String channel, String channelBotId, Integer status);
}
