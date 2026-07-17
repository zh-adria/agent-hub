package com.agenthub.infra.persistence.repository;

import com.agenthub.infra.persistence.entity.BotWebhookEventEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface BotWebhookEventJpaRepository extends JpaRepository<BotWebhookEventEntity, Long> {
    Optional<BotWebhookEventEntity> findByTenantIdAndChannelAndMessageId(Long tenantId, String channel, String messageId);
    long countByTenantId(Long tenantId);
}
