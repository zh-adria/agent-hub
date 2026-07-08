-- AgentHub 数据库初始化脚本 (Flyway V1)
-- 创建所有表结构

CREATE DATABASE IF NOT EXISTS agent_hub 
  CHARACTER SET utf8mb4 
  COLLATE utf8mb4_unicode_ci;

USE agent_hub;

-- 1. agent 表
CREATE TABLE agent (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT 'Agent名称',
    description TEXT COMMENT '描述',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    config JSON COMMENT '配置信息',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    created_by BIGINT NOT NULL DEFAULT 0 COMMENT '创建人',
    updated_by BIGINT NOT NULL DEFAULT 0 COMMENT '更新人',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0 COMMENT '软删除',
    INDEX idx_status (status),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent表';

-- 2. function_definition 表
CREATE TABLE function_definition (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT '所属Agent',
    name VARCHAR(100) NOT NULL COMMENT '函数名称',
    description TEXT COMMENT '描述',
    parameters JSON COMMENT '参数定义',
    return_type VARCHAR(50) COMMENT '返回类型',
    version VARCHAR(20) NOT NULL DEFAULT '1.0.0',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_agent_id (agent_id),
    INDEX idx_status (status),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='函数定义表';

-- 3. session 表
CREATE TABLE session (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT '关联Agent',
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT '用户ID',
    context JSON COMMENT '会话上下文',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:活跃 0:结束',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ended_at DATETIME COMMENT '结束时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_agent_id (agent_id),
    INDEX idx_user_id (user_id),
    INDEX idx_status (status),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='会话表';

-- 4. tool_execution 表
CREATE TABLE tool_execution (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    function_id BIGINT NOT NULL COMMENT '函数ID',
    input JSON COMMENT '输入参数',
    output JSON COMMENT '输出结果',
    status TINYINT NOT NULL COMMENT '执行状态',
    error_message TEXT COMMENT '错误信息',
    started_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    finished_at DATETIME COMMENT '完成时间',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    INDEX idx_session_id (session_id),
    INDEX idx_function_id (function_id),
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE,
    FOREIGN KEY (function_id) REFERENCES function_definition(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='工具执行记录表';

-- 5. api_endpoint 表
CREATE TABLE api_endpoint (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_id BIGINT NOT NULL COMMENT 'Agent ID',
    path VARCHAR(200) NOT NULL COMMENT 'API路径',
    method VARCHAR(10) NOT NULL COMMENT 'HTTP方法',
    description TEXT COMMENT '描述',
    auth_required TINYINT NOT NULL DEFAULT 1 COMMENT '是否需要认证',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0,
    UNIQUE KEY uk_agent_path (agent_id, path, method),
    FOREIGN KEY (agent_id) REFERENCES agent(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='API端点表';

-- 6. audit_log 表
CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL DEFAULT 0 COMMENT '操作用户',
    action VARCHAR(50) NOT NULL COMMENT '操作类型',
    resource_type VARCHAR(50) NOT NULL COMMENT '资源类型',
    resource_id BIGINT COMMENT '资源ID',
    details JSON COMMENT '详情',
    ip_address VARCHAR(45) COMMENT 'IP地址',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_resource (resource_type, resource_id),
    INDEX idx_created_at (created_at)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='审计日志表';

-- 7. user 表
CREATE TABLE user (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE COMMENT '用户名',
    email VARCHAR(100) NOT NULL UNIQUE COMMENT '邮箱',
    password_hash VARCHAR(255) NOT NULL COMMENT '密码哈希',
    role VARCHAR(20) NOT NULL DEFAULT 'user' COMMENT '角色',
    status TINYINT NOT NULL DEFAULT 1 COMMENT '1:启用 0:禁用',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='用户表';

-- 8. agent_template 表
CREATE TABLE agent_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(100) NOT NULL COMMENT '模板名称',
    description TEXT COMMENT '描述',
    config JSON COMMENT '模板配置',
    is_public TINYINT NOT NULL DEFAULT 0 COMMENT '是否公开',
    created_by BIGINT NOT NULL DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent模板表';

-- 9. agent_function_template 表
CREATE TABLE agent_function_template (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    agent_template_id BIGINT NOT NULL COMMENT '模板ID',
    function_name VARCHAR(100) NOT NULL COMMENT '函数名称',
    parameters JSON COMMENT '参数定义',
    FOREIGN KEY (agent_template_id) REFERENCES agent_template(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='Agent函数模板表';

-- 10. message 表
CREATE TABLE message (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    session_id BIGINT NOT NULL COMMENT '会话ID',
    role VARCHAR(20) NOT NULL COMMENT '角色: user/assistant/system',
    content TEXT NOT NULL COMMENT '消息内容',
    metadata JSON COMMENT '元数据',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_session_id (session_id),
    FOREIGN KEY (session_id) REFERENCES session(id) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='消息表';

-- 11. configuration 表
CREATE TABLE configuration (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    key VARCHAR(100) NOT NULL UNIQUE COMMENT '配置键',
    value TEXT COMMENT '配置值',
    description TEXT COMMENT '描述',
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='系统配置表';

-- 12. notification 表
CREATE TABLE notification (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL COMMENT '用户ID',
    title VARCHAR(200) NOT NULL COMMENT '标题',
    content TEXT COMMENT '内容',
    type VARCHAR(20) NOT NULL COMMENT '类型',
    is_read TINYINT NOT NULL DEFAULT 0 COMMENT '是否已读',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    INDEX idx_user_id (user_id),
    INDEX idx_is_read (is_read)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COMMENT='通知表';
