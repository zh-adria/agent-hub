-- AgentHub 数据库 Schema
-- 共 12 张表，含审计字段 created_by / updated_by

CREATE TABLE IF NOT EXISTS agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 'Agent 名称',
    description TEXT COMMENT '描述',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    config JSON COMMENT 'Agent 配置',
    created_by VARCHAR(64) NOT NULL COMMENT '创建人',
    updated_by VARCHAR(64) NOT NULL COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL COMMENT '软删除',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent 主表';

CREATE TABLE IF NOT EXISTS function_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT '关联 Agent',
    name VARCHAR(100) NOT NULL COMMENT '函数名',
    description TEXT COMMENT '描述',
    parameters JSON COMMENT '参数定义',
    return_type VARCHAR(50) COMMENT '返回类型',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    INDEX idx_agent_id (agent_id),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数定义表';

CREATE TABLE IF NOT EXISTS session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT '关联 Agent',
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    context JSON COMMENT '会话上下文',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:活跃 0:结束',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    INDEX idx_agent_user (agent_id, user_id),
    INDEX idx_status (status),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

CREATE TABLE IF NOT EXISTS message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '关联会话',
    role VARCHAR(20) NOT NULL COMMENT 'user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    tool_calls JSON COMMENT '工具调用记录',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session (session_id),
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

CREATE TABLE IF NOT EXISTS tool_invocation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    message_id BIGINT NOT NULL COMMENT '关联消息',
    function_name VARCHAR(100) NOT NULL COMMENT '函数名',
    arguments JSON COMMENT '调用参数',
    result TEXT COMMENT '执行结果',
    status VARCHAR(20) NOT NULL COMMENT 'success/error/timeout',
    duration INT COMMENT '耗时(ms)',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_message (message_id),
    FOREIGN KEY (message_id) REFERENCES message(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具调用记录表';

CREATE TABLE IF NOT EXISTS user_permission (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    permission_type VARCHAR(20) NOT NULL COMMENT 'read/write/admin',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_agent (user_id, agent_id),
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户权限表';

CREATE TABLE IF NOT EXISTS audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) COMMENT '操作用户',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型',
    resource_id VARCHAR(64) COMMENT '资源ID',
    details JSON COMMENT '详情',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user (user_id),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

CREATE TABLE IF NOT EXISTS system_config (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    config_key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    config_value TEXT COMMENT '配置值',
    description TEXT COMMENT '描述',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

CREATE TABLE IF NOT EXISTS vector_store (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT '关联 Agent',
    document_id VARCHAR(64) NOT NULL COMMENT '文档ID',
    content TEXT NOT NULL COMMENT '内容',
    embedding JSON COMMENT '向量',
    metadata JSON COMMENT '元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_agent (agent_id),
    INDEX idx_document (document_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='向量存储表';

CREATE TABLE IF NOT EXISTS rate_limit (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id VARCHAR(64) NOT NULL COMMENT '用户标识',
    resource_key VARCHAR(100) NOT NULL COMMENT '资源键',
    limit_value INT NOT NULL COMMENT '限制值',
    current_value INT NOT NULL DEFAULT 0 COMMENT '当前值',
    window_start DATETIME NOT NULL COMMENT '窗口开始',
    window_end DATETIME NOT NULL COMMENT '窗口结束',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uk_user_resource (user_id, resource_key),
    INDEX idx_window (window_end)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='限流表';

CREATE TABLE IF NOT EXISTS llm_provider (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(50) NOT NULL UNIQUE COMMENT '提供商名称',
    provider_type VARCHAR(20) NOT NULL COMMENT 'openai/anthropic/azure等',
    api_base_url VARCHAR(255) COMMENT 'API地址',
    api_key_encrypted TEXT COMMENT '加密后的API Key',
    models JSON COMMENT '支持的模型列表',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    priority INT NOT NULL DEFAULT 0 COMMENT '优先级',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='LLM提供商表';

CREATE TABLE IF NOT EXISTS knowledge_base (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '知识库名称',
    agent_id BIGINT NOT NULL COMMENT '关联 Agent',
    description TEXT COMMENT '描述',
    config JSON COMMENT '配置',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    created_by VARCHAR(64) NOT NULL,
    updated_by VARCHAR(64) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted_at DATETIME DEFAULT NULL,
    INDEX idx_agent_id (agent_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='知识库表';
