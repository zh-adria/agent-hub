-- AgentHub Initial Data
-- Phase 1: Essential seed data
-- =============================================

-- 系统租户（默认租户）
INSERT INTO tenant (id, name, code, status, config, created_by, updated_by) 
VALUES (1, 'System', 'SYSTEM', 1, '{"maxAgents": 100, "maxFunctions": 500}', 'system', 'system')
ON DUPLICATE KEY UPDATE name = name;

-- 管理员用户（密码: admin123 的 BCrypt 哈希）
INSERT INTO user_account (id, tenant_id, username, email, password_hash, status, created_by, updated_by) 
VALUES (1, 1, 'admin', 'admin@agenthub.local', '$2a$10$rO1k2q3w4e5r6t7y8u9i0o1p2a3s4d5f6g7h8j9k0l1z2x3c4v5b6n7m8', 1, 'system', 'system')
ON DUPLICATE KEY UPDATE username = username;

-- 默认 Agent：通用助手
INSERT INTO agent (id, tenant_id, name, description, system_prompt, llm_provider, llm_model, temperature, max_tokens, tools, status, version, created_by, updated_by)
VALUES (1, 1, '通用助手', 'Default general-purpose assistant', 'You are a helpful assistant.', 'openai', 'gpt-4o-mini', 0.70, 2048, '[]', 1, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE name = name;

-- 默认函数：计算器
INSERT INTO function_definition (id, tenant_id, name, description, endpoint_url, protocol, parameters, status, created_by, updated_by)
VALUES (1, 1, 'calculator', 'Basic arithmetic calculator', NULL, 'builtin', 
'{"type": "object", "properties": {"expression": {"type": "string", "description": "Math expression"}}, "required": ["expression"]}', 
1, 'system', 'system')
ON DUPLICATE KEY UPDATE name = name;

-- 默认函数权限：允许通用助手使用计算器
INSERT INTO function_permission (id, tenant_id, function_id, allowed_agent_ids, max_calls_per_minute, created_by, updated_by)
VALUES (1, 1, 1, '[1]', 60, 'system', 'system')
ON DUPLICATE KEY UPDATE function_id = function_id;

-- 默认熔断规则
INSERT INTO circuit_breaker_rule (id, tenant_id, provider, model, failure_threshold, reset_timeout_seconds, half_open_max_calls, status, created_by, updated_by)
VALUES 
  (1, 1, 'openai', 'gpt-4o-mini', 5, 60, 3, 1, 'system', 'system'),
  (2, 1, 'openai', 'gpt-4o', 5, 60, 3, 1, 'system', 'system'),
  (3, 1, 'anthropic', 'claude-3-5-sonnet-latest', 5, 60, 3, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE id = id;

-- 默认配额：全局每日请求限额
INSERT INTO quota (id, tenant_id, scope, scope_id, quota_type, limit_value, window_seconds, status, created_by, updated_by)
VALUES (1, 1, 'GLOBAL', 0, 'REQUESTS', 10000, 86400, 1, 'system', 'system')
ON DUPLICATE KEY UPDATE id = id;

-- 默认 API Key（生产环境必须替换）
INSERT INTO api_key (tenant_id, user_id, name, key_hash, key_prefix, permissions, status, created_by, updated_by)
VALUES (1, 1, 'default-key', 
  'sha256:aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa', 
  'agenthub', '{"scope": "global"}', 1, 'system', 'system')
ON DUPLICATE KEY UPDATE name = name;
