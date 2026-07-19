package com.agenthub.infra.persistence.entity;

import com.agenthub.infra.persistence.encryption.EncryptedStringConverter;
import javax.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "ah_bot_binding")
public class BotBindingEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tenant_id", nullable = false)
    private Long tenantId;

    @Column(name = "channel", nullable = false, length = 32)
    private String channel;

    @Column(name = "channel_bot_id", nullable = false, length = 128)
    private String channelBotId;

    @Column(name = "agent_id", nullable = false, length = 64)
    private String agentId;

    @Column(name = "secret", length = 128)
    @Convert(converter = EncryptedStringConverter.class)
    private String secret;

    @Column(name = "status", nullable = false)
    private Integer status = 1;

    @Column(name = "created_by", nullable = false, length = 64)
    private String createdBy;

    @Column(name = "updated_by", nullable = false, length = 64)
    private String updatedBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getTenantId() { return tenantId; }
    public void setTenantId(Long tenantId) { this.tenantId = tenantId; }
    public String getChannel() { return channel; }
    public void setChannel(String channel) { this.channel = channel; }
    public String getChannelBotId() { return channelBotId; }
    public void setChannelBotId(String channelBotId) { this.channelBotId = channelBotId; }
    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }
    public String getSecret() { return secret; }
    public void setSecret(String secret) { this.secret = secret; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getCreatedBy() { return createdBy; }
    public void setCreatedBy(String createdBy) { this.createdBy = createdBy; }
    public String getUpdatedBy() { return updatedBy; }
    public void setUpdatedBy(String updatedBy) { this.updatedBy = updatedBy; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
