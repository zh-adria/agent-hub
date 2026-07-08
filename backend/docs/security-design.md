# AgentHub Security Design

## 1. Authentication (JWT)

### 1.1 Token Structure
- Header: `Authorization: Bearer <token>`
- Expiration: 2 hours (access token), 7 days (refresh token)
- Storage: HTTP-only cookie (refresh), localStorage (access)

### 1.2 Login Flow
1. User submits credentials
2. Server validates and returns JWT pair
3. Client stores tokens securely
4. Access token sent in Authorization header
5. Refresh token used to obtain new access token

## 2. Authorization (RBAC)

### 2.1 Roles
- **Admin**: Full system access
- **User**: Create/manage own agents and sessions
- **Guest**: Read-only access to public agents

### 2.2 Permission Matrix
| Resource | Admin | User | Guest |
|----------|-------|------|-------|
| Agent CRUD | All | Own | Read public |
| Function invoke | All | All | All |
| Session | All | Own | None |

## 3. API Rate Limiting

### 3.1 Limits
- **/api/llm/invoke**: 10 requests/minute per user
- **/api/functions/{id}/invoke**: 30 requests/minute per user
- **/api/sessions**: 5 requests/minute per user

### 3.2 Implementation
- Token bucket algorithm
- Redis for distributed counter
- Response headers: `X-RateLimit-Limit`, `X-RateLimit-Remaining`

## 4. Audit Logging

### 4.1 Logged Events
- Authentication (login/logout)
- Authorization failures
- Agent CRUD operations
- Function invocations
- Session creation/deletion

### 4.2 Log Format
```
timestamp | userId | action | resource | status | ip | userAgent
```

### 4.3 Storage
- Elasticsearch for search/analysis
- 90-day retention policy

## 5. Data Protection

### 5.1 Sensitive Data
- Passwords: bcrypt (12 rounds)
- AgentHub-owned function/sandbox credentials: AES-256-GCM encrypted
- LLM provider API keys: owned by Token Router, not stored as AgentHub source-of-truth secrets
- PII: Masked in logs

### 5.2 Input Validation
- OWASP Top 10 protection
- SQL injection prevention (parameterized queries)
- XSS prevention (input sanitization)
- CSRF tokens for state-changing operations

## 6. Security Headers
- `Content-Security-Policy`
- `X-Content-Type-Options: nosniff`
- `X-Frame-Options: DENY`
- `Strict-Transport-Security`

## 7. Incident Response
- Security monitoring dashboard
- Automated alerting for anomalies
- Incident runbook documented
