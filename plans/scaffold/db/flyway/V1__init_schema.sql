-- AgentHub 初始化脚本 V1
-- 包含：表结构 + 初始数据

-- 12张表 DDL（同 schema.sql，此处省略具体 DDL，实际使用时复制 schema.sql 内容）
-- 注意：Flyway 要求每次迁移包含完整可执行 SQL

-- 初始数据
INSERT INTO llm_provider (name, provider_type, api_base_url, models, status, priority, created_by, updated_by)
VALUES 
    ('OpenAI', 'openai', 'https://api.openai.com/v1', '["gpt-4o","gpt-4o-mini","gpt-4-turbo"]', 1, 10, 'system', 'system'),
    ('Anthropic', 'anthropic', 'https://api.anthropic.com', '["claude-3-5-sonnet-20240620","claude-3-haiku-20240307"]', 1, 9, 'system', 'system'),
    ('Azure OpenAI', 'azure', NULL, '[]', 1, 8, 'system', 'system')
ON DUPLICATE KEY UPDATE updated_by = 'system';

INSERT INTO system_config (config_key, config_value, description, created_by, updated_by)
VALUES 
    ('system.name', 'AgentHub', '系统名称', 'system', 'system'),
    ('system.version', '1.0.0', '系统版本', 'system', 'system'),
    ('llm.default_provider', 'OpenAI', '默认LLM提供商', 'system', 'system'),
    ('agent.max_tools_per_agent', '10', '单个Agent最大工具数', 'system', 'system'),
    ('security.jwt.expire_minutes', '120', 'JWT过期时间(分钟)', 'system', 'system')
ON DUPLICATE KEY UPDATE config_value = config_value, updated_by = 'system';
