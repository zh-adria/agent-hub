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
    method VARCHAR(16) COMMENT 'HTTP method',
    implementation VARCHAR(32) COMMENT 'http/mcp/local',
    parameters JSON NOT NULL COMMENT 'JSON Schema',
    headers JSON COMMENT 'HTTP headers',
    auth_config JSON COMMENT 'auth config',
    timeout_ms INT DEFAULT 30000,
    retry_policy JSON COMMENT 'retry config: maxAttempts, waitDurationMs',
    circuit_breaker_policy JSON COMMENT 'circuit breaker config: failureRateThreshold, waitDurationInOpenStateMs',
    fallback_response TEXT COMMENT 'fallback response when all retries exhausted',
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
-- RAG knowledge base table
CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    embedding_provider VARCHAR(32) NOT NULL,
    embedding_model VARCHAR(128) NOT NULL,
    chunk_size INT NOT NULL DEFAULT 800,
    chunk_overlap INT NOT NULL DEFAULT 120,
    metadata JSON,
    status TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_kb_tenant (tenant_id),
    INDEX idx_kb_status (status),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG knowledge base';

-- RAG document table
CREATE TABLE IF NOT EXISTS rag_document (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    title VARCHAR(256) NOT NULL,
    source_uri VARCHAR(1024),
    mime_type VARCHAR(128),
    content_hash VARCHAR(128),
    metadata JSON,
    status TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_doc_kb (knowledge_base_id),
    INDEX idx_doc_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document';

-- RAG document chunk table
CREATE TABLE IF NOT EXISTS document_chunk (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_index INT NOT NULL,
    content TEXT NOT NULL,
    token_count INT,
    embedding_id VARCHAR(128),
    metadata JSON,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_chunk_doc (document_id, chunk_index),
    INDEX idx_chunk_kb (knowledge_base_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id),
    FOREIGN KEY (document_id) REFERENCES rag_document(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG document chunk';

-- RAG vector embedding table
CREATE TABLE IF NOT EXISTS vector_embedding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    knowledge_base_id BIGINT NOT NULL,
    document_id BIGINT NOT NULL,
    chunk_id BIGINT NOT NULL,
    provider VARCHAR(32) NOT NULL,
    model VARCHAR(128) NOT NULL,
    dimension INT NOT NULL,
    vector TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_vec_kb (knowledge_base_id),
    UNIQUE KEY uk_vec_chunk (chunk_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (knowledge_base_id) REFERENCES knowledge_base(id),
    FOREIGN KEY (document_id) REFERENCES rag_document(id),
    FOREIGN KEY (chunk_id) REFERENCES document_chunk(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='RAG vector embedding';

CREATE TABLE IF NOT EXISTS workflow_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    description TEXT,
    definition JSON NOT NULL,
    status TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_workflow_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Multi-Agent workflow definition';

CREATE TABLE IF NOT EXISTS trace (
    id VARCHAR(64) PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    session_id VARCHAR(64),
    workflow_id BIGINT,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL,
    user_id VARCHAR(64),
    metadata JSON,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    INDEX idx_trace_tenant_started (tenant_id, started_at),
    INDEX idx_trace_session (session_id),
    INDEX idx_trace_workflow (workflow_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Execution trace';

CREATE TABLE IF NOT EXISTS step_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    trace_id VARCHAR(64) NOT NULL,
    session_id VARCHAR(64),
    workflow_id BIGINT,
    step_key VARCHAR(128) NOT NULL,
    agent_id VARCHAR(64),
    status VARCHAR(32) NOT NULL,
    input TEXT,
    output TEXT,
    error TEXT,
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME,
    INDEX idx_step_trace (trace_id, started_at),
    INDEX idx_step_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (trace_id) REFERENCES trace(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Trace step record';

CREATE TABLE IF NOT EXISTS evaluation_run (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    name VARCHAR(128) NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL,
    total_cases INT NOT NULL DEFAULT 0,
    passed_cases INT NOT NULL DEFAULT 0,
    score DOUBLE DEFAULT 0,
    created_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_eval_run_tenant (tenant_id, created_at),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Offline evaluation run';

CREATE TABLE IF NOT EXISTS evaluation_case_result (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    run_id BIGINT NOT NULL,
    case_key VARCHAR(128) NOT NULL,
    input TEXT NOT NULL,
    expected TEXT,
    actual TEXT,
    passed TINYINT NOT NULL DEFAULT 0,
    error TEXT,
    INDEX idx_eval_case_run (run_id),
    INDEX idx_eval_case_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (run_id) REFERENCES evaluation_run(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Offline evaluation case result';

CREATE TABLE IF NOT EXISTS llm_usage_audit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    business_tag VARCHAR(128),
    user_id VARCHAR(128),
    policy_id VARCHAR(128),
    agent_id VARCHAR(64),
    agent_session_id VARCHAR(64),
    agent_step_id VARCHAR(64),
    agent_step_type VARCHAR(64),
    trace_id VARCHAR(64),
    tool_names TEXT,
    knowledge_base_id VARCHAR(64),
    provider VARCHAR(64),
    model VARCHAR(128),
    prompt_tokens INT,
    completion_tokens INT,
    total_tokens INT,
    cost DECIMAL(12, 6),
    route_decision VARCHAR(64),
    route_reason TEXT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_llm_audit_tenant_created (tenant_id, created_at),
    INDEX idx_llm_audit_agent (agent_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM usage audit';

CREATE TABLE IF NOT EXISTS bot_binding (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    channel_bot_id VARCHAR(128) NOT NULL,
    agent_id VARCHAR(64) NOT NULL,
    secret VARCHAR(128),
    status TINYINT NOT NULL DEFAULT 1,
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bot_binding_channel (tenant_id, channel, channel_bot_id),
    INDEX idx_bot_binding_tenant (tenant_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Enterprise bot channel binding';

CREATE TABLE IF NOT EXISTS bot_webhook_event (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    tenant_id BIGINT NOT NULL,
    channel VARCHAR(32) NOT NULL,
    message_id VARCHAR(128) NOT NULL,
    binding_id BIGINT NOT NULL,
    session_id VARCHAR(128) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_bot_event_message (tenant_id, channel, message_id),
    INDEX idx_bot_event_binding (binding_id),
    FOREIGN KEY (tenant_id) REFERENCES tenant(id),
    FOREIGN KEY (binding_id) REFERENCES bot_binding(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Bot webhook idempotency event';

-- Dify migration result table
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
