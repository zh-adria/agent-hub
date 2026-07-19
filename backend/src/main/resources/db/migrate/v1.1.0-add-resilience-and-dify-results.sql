-- AgentHub Database Migration: Add resilience columns and Dify migration result table
-- Run this script on existing databases that were created before v1.1.0
-- ========================================================================

-- Add resilience columns to function_definition table
ALTER TABLE function_definition
    ADD COLUMN IF NOT EXISTS circuit_breaker_policy JSON COMMENT 'circuit breaker config: failureRateThreshold, waitDurationInOpenStateMs' AFTER retry_policy,
    ADD COLUMN IF NOT EXISTS fallback_response TEXT COMMENT 'fallback response when all retries exhausted' AFTER circuit_breaker_policy;

-- Create Dify migration result table
CREATE TABLE IF NOT EXISTS dify_migration_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    source_name VARCHAR(255) COMMENT 'original Dify resource name',
    status VARCHAR(32) NOT NULL COMMENT 'SUCCEEDED/FAILED',
    target_type VARCHAR(64) COMMENT 'Agent/FunctionDefinition/WorkflowDefinition/KnowledgeBase',
    source_type VARCHAR(64) COMMENT 'dify-app/dify-tool/dify-workflow/dify-knowledge',
    error_message TEXT COMMENT 'error details if failed',
    mapping_detail JSON COMMENT 'full mapping metadata',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_dify_result_tenant (tenant_id),
    INDEX idx_dify_result_status (status),
    INDEX idx_dify_result_created (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Dify migration import result';
