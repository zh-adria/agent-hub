-- AgentHub 初始数据
-- 创建 admin 用户和测试 Agent

INSERT INTO user (username, email, password_hash, role) VALUES
('admin', 'admin@agenthub.com', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'admin');

INSERT INTO agent (name, description, version, config, status, created_by) VALUES
('Weather Agent', '天气查询Agent，支持城市天气查询', '1.0.0', '{"model": "gpt-4", "temperature": 0.7}', 1, 1);

INSERT INTO function_definition (agent_id, name, description, parameters, return_type, version, status, created_by) VALUES
(1, 'get_weather', '获取指定城市的天气信息', '{"type": "object", "properties": {"city": {"type": "string", "description": "城市名称"}}, "required": ["city"]}', 'string', '1.0.0', 1, 1);

INSERT INTO session (agent_id, user_id, context, status, created_by) VALUES
(1, 1, '{}', 1, 1);