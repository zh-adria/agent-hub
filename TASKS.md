# AgentHub Task Backlog

Updated: 2026-07-08

## Product Positioning

AgentHub is the enterprise Agent infrastructure platform. It owns Agent lifecycle, Function registry, Session state, workflow/runtime orchestration, memory, sandbox execution, and vector-store-backed RAG.

Token Router (`C:\Users\16813\Downloads\token-router`) is the LLM gateway and governance layer. AgentHub should call Token Router for model invocation instead of duplicating provider routing, provider key management, quota, budget, billing, or model governance.

Canonical boundary document: [PROJECT_BOUNDARY.md](PROJECT_BOUNDARY.md).

## Current Task

Next recommended task: implement the AgentHub-to-Token-Router invocation contract in AgentHub runtime calls.

Success criteria:

- Agent runtime calls Token Router for non-stream and stream completions.
- AgentHub passes `businessTag`, `userId`, `policyId`, `modelHint`, and Agent correlation metadata in `extensions`.
- AgentHub consumes provider/model/usage/cost/route-decision fields from Token Router responses.
- AgentHub does not store LLM provider API keys as source-of-truth secrets.
- AgentHub keeps Agent/Function/Session/Workflow/RAG ownership.

## Boundary-Aligned Next Tasks

### P0. Token Router Client Integration - Todo

- Add or align `TokenRouterClient` for completion and streaming completion calls.
- Map Agent runtime inputs to Token Router request fields.
- Support metadata fields: `agentId`, `agentSessionId`, `agentStepId`, `agentStepType`, `toolNames`, `knowledgeBaseId`, `traceId`.
- Handle Token Router errors and policy denials as Agent runtime step failures.

### P0. Runtime Metadata Propagation - Todo

- Ensure Agent execution, Function invocation, and Session flows pass correlation metadata consistently.
- Ensure stream responses preserve step/session correlation.
- Add tests for metadata propagation.

### P1. Cost and Audit Attribution - Todo

- Consume Token Router usage/cost/route-decision fields.
- Attribute LLM cost to Agent, Session, Function, user, and tenant dimensions.
- Keep Token Router as the source of truth for provider/model cost and billing totals.

### P1. RAG Ownership Hardening - Todo

- Keep document ingestion, chunking, embedding, vector collection lifecycle, retrieval, and rerank in AgentHub or an external RAG service.
- Pass `knowledgeBaseId` to Token Router only for gateway policy validation and audit correlation.
- Do not move vector-store ownership into Token Router.

### P1. Workflow Runtime Hardening - Todo

- Keep configurable Agent workflow definitions in AgentHub.
- Ensure workflow step IDs and step types are passed to Token Router as metadata.
- Avoid implementing workflow runtime inside Token Router.

### P2. Documentation and Examples - Todo

- Add example AgentHub requests to Token Router for non-stream and stream calls.
- Add failure examples for tool policy denial, budget denial, and provider fallback.
- Document local development startup order for AgentHub + Token Router.
