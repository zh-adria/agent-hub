-- AgentHub 初始数据
-- 创建 admin 用户和测试 Agent

-- 注意：实际用户数据应通过应用层创建，此处仅为示例
-- INSERT INTO user_permission (user_id, agent_id, permission_type) VALUES ('admin', 1, 'admin');

-- 测试 Agent 数据
INSERT INTO agent (name, description, version, status, config, created_by, updated_by)
VALUES 
    ('Weather Assistant', '天气查询助手，支持城市天气查询', '1.0.0', 1, '{"model":"gpt-4o-mini","temperature":0.7}', 'system', 'system'),
    ('Code Review Bot', '代码审查机器人，支持多语言代码审查', '1.0.0', 1, '{"model":"gpt-4o","temperature":0.3}', 'system', 'system')
ON DUPLICATE KEY UPDATE updated_by = 'system';
