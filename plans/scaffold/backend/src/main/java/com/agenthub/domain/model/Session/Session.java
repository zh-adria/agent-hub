package com.agenthub.domain.model.Session;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * Session 聚合根
 */
@Data
public class Session {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
