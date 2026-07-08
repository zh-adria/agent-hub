-- AgentHub Database Schema
-- Phase 1: Core Tables with Audit Fields
-- =============================================

-- 租户表
CREATE TABLE IF NOT EXISTS tenant (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(128) NOT NULL,
    code VARCHAR(64) UNIQUE NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    config JSON COMMENT 'tenant config: quotas, features',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_tenant_status (status),
    INDEX idx_tenant_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='租户表';

-- 用户表
CREATE TABLE IF NOT EXISTS user_account (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    username VARCHAR(64) NOT NULL,
    email VARCHAR(128),
    phone VARCHAR(32),
    password_hash VARCHAR(255) NOT NULL,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    last_login_at DATETIME,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_tenant_username (tenant_id, username),
    INDEX idx_user_tenant (tenant_id),
    INDEX idx_user_email (email),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- Agent 定义表
CREATE TABLE IF NOT EXISTS agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    system_prompt TEXT NOT NULL,
    llm_provider VARCHAR(32) NOT NULL COMMENT 'openai/anthropic/qwen',
    llm_model VARCHAR(64) NOT NULL,
    temperature DECIMAL(3,2) DEFAULT 0.70,
    max_tokens INT DEFAULT 2048,
    tools JSON COMMENT 'tool definitions',
    metadata JSON COMMENT 'custom metadata',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    version INT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_agent_tenant (tenant_id),
    INDEX idx_agent_status (status),
    INDEX idx_agent_provider_model (llm_provider, llm_model),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent定义表';

-- Agent 版本表
CREATE TABLE IF NOT EXISTS agent_version (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL,
    tenant_id BIGINT NOT NULL,
    version INT NOT NULL,
    snapshot JSON NOT NULL COMMENT 'full agent config snapshot',
    change_log TEXT,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent_version_agent (agent_id),
    INDEX idx_agent_version_tenant (tenant_id),
    FOREIGN KEY (agent_id) REFERENCES agent(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent版本快照表';

-- 函数定义表
CREATE TABLE IF NOT EXISTS function_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    endpoint_url VARCHAR(512) COMMENT 'HTTP endpoint',
    protocol VARCHAR(16) COMMENT 'HTTP/MCP/gRPC',
    parameters JSON NOT NULL COMMENT 'JSON Schema',
    headers JSON COMMENT 'HTTP headers',
    auth_config JSON COMMENT 'auth config',
    timeout_ms INT DEFAULT 30000,
    retry_policy JSON COMMENT 'retry config',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_func_tenant_name (tenant_id, name),
    INDEX idx_func_tenant (tenant_id),
    INDEX idx_func_status (status),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数定义表';

-- 函数权限表
CREATE TABLE IF NOT EXISTS function_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    function_id BIGINT NOT NULL,
    allowed_agent_ids JSON COMMENT 'null means all agents',
    denied_agent_ids JSON,
    max_calls_per_minute INT DEFAULT 60,
    max_tokens_per_call INT DEFAULT 4096,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_func_perm (tenant_id, function_id),
    INDEX idx_func_perm_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (function_id) REFERENCES function_definition(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数权限控制表';

-- 会话表
CREATE TABLE IF NOT EXISTS session (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    agent_id BIGINT NOT NULL,
    user_id BIGINT COMMENT 'session initiator',
    context JSON NOT NULL COMMENT 'conversation context',
    state VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT 'active/completed/failed/timeout',
    turn_count INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    total_cost DECIMAL(10,4) DEFAULT 0.0000,
    ended_at DATETIME,
    metadata JSON COMMENT 'custom metadata',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_session_tenant (tenant_id),
    INDEX idx_session_agent (agent_id),
    INDEX idx_session_user (user_id),
    INDEX idx_session_state (state),
    INDEX idx_session_created (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 审计日志表
CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    operator_id BIGINT NOT NULL COMMENT 'operator user id',
    operator_name VARCHAR(64) NOT NULL,
    action VARCHAR(64) NOT NULL COMMENT 'CREATE/UPDATE/DELETE/EXECUTE',
    resource_type VARCHAR(64) NOT NULL COMMENT 'AGENT/FUNCTION/SESSION',
    resource_id VARCHAR(128) NOT NULL,
    detail JSON COMMENT 'operation detail',
    ip_address VARCHAR(45),
    user_agent TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_audit_tenant (tenant_id),
    INDEX idx_audit_resource (resource_type, resource_id),
    INDEX idx_audit_operator (operator_id),
    INDEX idx_audit_time (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 成本记录表
CREATE TABLE IF NOT EXISTS cost_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    session_id VARCHAR(64) NOT NULL,
    agent_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NOT NULL,
    prompt_tokens INT NOT NULL DEFAULT 0,
    completion_tokens INT NOT NULL DEFAULT 0,
    total_tokens INT NOT NULL DEFAULT 0,
    cost DECIMAL(10,6) NOT NULL DEFAULT 0.000000,
    currency VARCHAR(8) NOT NULL DEFAULT 'USD',
    recorded_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_cost_tenant (tenant_id),
    INDEX idx_cost_session (session_id),
    INDEX idx_cost_agent (agent_id),
    INDEX idx_cost_time (recorded_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (agent_id) REFERENCES agent(id),
    FOREIGN KEY (session_id) REFERENCES session(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='成本记录表';

-- 熔断规则表
CREATE TABLE IF NOT EXISTS circuit_breaker_rule (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(64) NOT NULL,
    failure_threshold INT NOT NULL DEFAULT 5 COMMENT 'consecutive failures',
    reset_timeout_seconds INT NOT NULL DEFAULT 60,
    half_open_max_calls INT NOT NULL DEFAULT 3,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_cb_tenant_provider_model (tenant_id, provider, model),
    INDEX idx_cb_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='熔断规则表';

-- 配额表
CREATE TABLE IF NOT EXISTS quota (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    scope VARCHAR(32) NOT NULL COMMENT 'GLOBAL/AGENT/USER',
    scope_id BIGINT COMMENT 'agent_id or user_id',
    quota_type VARCHAR(32) NOT NULL COMMENT 'REQUESTS/TOKENS/COST',
    limit_value BIGINT NOT NULL COMMENT 'max allowed',
    window_seconds INT NOT NULL DEFAULT 86400 COMMENT '1 day',
    current_usage BIGINT NOT NULL DEFAULT 0,
    window_start DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:disabled',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_quota_tenant_scope (tenant_id, scope, scope_id),
    INDEX idx_quota_tenant_type (tenant_id, quota_type),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='配额表';

-- 密钥表
CREATE TABLE IF NOT EXISTS api_key (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    key_hash VARCHAR(255) NOT NULL COMMENT 'SHA-256 hash',
    key_prefix VARCHAR(16) NOT NULL COMMENT 'first 8 chars for identification',
    permissions JSON COMMENT 'allowed scopes',
    expires_at DATETIME,
    last_used_at DATETIME,
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:active, 0:revoked',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_key_tenant (tenant_id),
    INDEX idx_key_user (user_id),
    INDEX idx_key_prefix (key_prefix),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API密钥表';

-- 通知表
CREATE TABLE IF NOT EXISTS notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    type VARCHAR(32) NOT NULL COMMENT 'INFO/WARNING/ERROR',
    title VARCHAR(255) NOT NULL,
    content TEXT,
    data JSON COMMENT 'notification payload',
    read TINYINT NOT NULL DEFAULT 0,
    read_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_notification_tenant_user (tenant_id, user_id, read),
    INDEX idx_notification_time (created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (user_id) REFERENCES user_account(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
