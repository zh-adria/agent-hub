# AgentHub Project Boundary

Updated: 2026-07-08

## Positioning

AgentHub is the enterprise Agent infrastructure platform.

Token Router (`C:\Users\16813\Downloads\token-router`) is the enterprise LLM gateway and governance layer.

The two systems integrate through HTTP/OpenAI-compatible model invocation. AgentHub should not duplicate Token Router's provider routing, quota, budget, billing, or model-governance responsibilities.

## Ownership Split

| Area | AgentHub | Token Router |
|------|----------|--------------|
| Agent lifecycle | Owns Agent CRUD, version, deployment, status | Does not own |
| Agent Studio | Owns visual creation and management UX | Does not own |
| Function registry | Owns Function Registry, Function Market, tool schemas | Does not own registry |
| Tool execution | Owns function invocation, sandbox policy, runtime result handling | Enforces gateway-level tool permission metadata only |
| Session and memory | Owns session lifecycle, message history, memory, context compression | Does not own |
| Workflow/runtime | Owns ReAct/workflow runtime and step orchestration | Does not own configurable Agent runtime |
| Sandbox | Owns sandbox execution and isolation | Does not own |
| Vector store/RAG | Owns vector-store integration, document lifecycle, retrieval/rerank orchestration, or delegates to external RAG | May validate `knowledgeBaseId` and record retrieval metadata |
| LLM provider access | Calls Token Router for model invocation | Owns provider config, endpoint, API key encryption, health probe |
| Model routing | Sends model intent and business metadata | Owns routing, fallback, circuit breaker, quota, budget |
| Audit and billing | Correlates by agent/session/function IDs | Owns LLM usage, cost, route decision, provider/model audit |

## Integration Contract

AgentHub should call one of Token Router's model invocation endpoints:

- `POST /api/chat/completions`
- `POST /api/chat/completions/stream`
- `POST /v1/chat/completions`

AgentHub should pass:

- Top-level: `businessTag`, `userId`, `policyId`, `modelHint`, `messages`, `temperature`, `maxTokens`, `stream`.
- `extensions.agentId`
- `extensions.agentSessionId`
- `extensions.agentStepId`
- `extensions.agentStepType`
- `extensions.toolNames` or `extensions.tools`
- `extensions.knowledgeBaseId`
- `extensions.traceId`

AgentHub should consume from Token Router:

- Provider and model used.
- Usage and cost.
- Route decision and reason.
- Audit identifiers or fields for agent/session/step correlation.

## Non-Goals

AgentHub should not implement:

- Central provider API key vault for all LLM vendors.
- Model-level routing, budget fallback, or circuit breaker logic already owned by Token Router.
- LLM provider billing and cost aggregation as the source of truth.
- Provider health management and model scorecard governance as the source of truth.
- A second OpenAI-compatible LLM gateway for enterprise clients.

## Current Next Task

Implement the AgentHub-to-Token-Router invocation contract in AgentHub runtime calls.

Immediate task breakdown:

- Add or align `TokenRouterClient` for non-stream and stream completions.
- Pass Agent correlation metadata through `extensions`.
- Consume Token Router provider/model/usage/cost/route-decision fields.
- Keep Agent workflow, Function execution, Session memory, and RAG ownership in AgentHub.
