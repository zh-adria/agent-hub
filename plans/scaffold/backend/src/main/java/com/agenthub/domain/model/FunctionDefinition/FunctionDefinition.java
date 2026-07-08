package com.agenthub.domain.model.FunctionDefinition;

import lombok.Data;
import java.time.LocalDateTime;

/**
 * FunctionDefinition 聚合根
 */
@Data
public class FunctionDefinition {
    private Long id;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
