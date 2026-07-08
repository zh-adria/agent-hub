package com.agenthub.domain.model.Agent;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Agent 聚合根
 */
@Data
public class Agent {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
