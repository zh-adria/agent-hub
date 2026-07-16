package com.agenthub.domain.service;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import com.agenthub.infra.persistence.entity.WorkflowDefinitionEntity;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;

@Service
public class WorkflowExecutionService {
    private final AgentRepository agentRepository;
    private final SessionRepository sessionRepository;
    private final ReActEngine reActEngine;
    private final TraceService traceService;
    private final ObjectMapper objectMapper;

    public WorkflowExecutionService(
            AgentRepository agentRepository,
            SessionRepository sessionRepository,
            ReActEngine reActEngine,
            TraceService traceService,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.sessionRepository = sessionRepository;
        this.reActEngine = reActEngine;
        this.traceService = traceService;
        this.objectMapper = objectMapper;
    }

    public Map<String, Object> execute(WorkflowDefinitionEntity workflow, Map<String, Object> payload) {
        List<Node> nodes = parseNodes(workflow.getDefinition());
        topologicalSort(nodes);
        String input = stringValue(payload.get("input"), "");
        TraceEntity trace = traceService.start("workflow:" + workflow.getName(), null, workflow.getId(), toJson(payload));
        Map<String, String> outputs = new LinkedHashMap<>();
        List<Map<String, Object>> stepResponses = new ArrayList<>();
        Set<String> completed = new LinkedHashSet<>();
        String status = "SUCCEEDED";
        Long tenantId = TenantContext.tenantId();
        String externalTenantId = TenantContext.externalTenantId();
        String userId = TenantContext.userId();
        ExecutorService executor = Executors.newFixedThreadPool(Math.max(1, Math.min(4, nodes.size())));

        try {
            while (completed.size() < nodes.size()) {
                List<Node> ready = readyNodes(nodes, completed);
                if (ready.isEmpty()) {
                    throw new IllegalArgumentException("Workflow has unresolved dependencies");
                }
                Optional<Node> humanNode = ready.stream().filter(Node::human).findFirst();
                if (humanNode.isPresent()) {
                    Node node = humanNode.get();
                    String nodeInput = buildNodeInput(input, node, outputs);
                    StepRecordEntity step = traceService.startStep(trace.getId(), null, workflow.getId(), node.id, node.agentId, nodeInput);
                    step.setStatus("WAITING_APPROVAL");
                    step.setOutput("Waiting for human approval");
                    step.setEndedAt(LocalDateTime.now());
                    stepResponses.add(mapStep(traceService.saveStep(step)));
                    status = "WAITING_APPROVAL";
                    traceService.finish(trace.getId(), status);
                    return response(workflow, trace, status, outputs, stepResponses, checkpoint(nodes, completed, node.id));
                }

                Map<Node, Future<NodeResult>> futures = new LinkedHashMap<>();
                for (Node node : ready) {
                    String nodeInput = buildNodeInput(input, node, outputs);
                    futures.put(node, executor.submit(() -> {
                        TenantContext.set(tenantId, externalTenantId, userId);
                        try {
                            return executeNodeWithRetry(trace, workflow, node, nodeInput);
                        } finally {
                            TenantContext.clear();
                        }
                    }));
                }

                for (Map.Entry<Node, Future<NodeResult>> entry : futures.entrySet()) {
                    Node node = entry.getKey();
                    try {
                        NodeResult result = entry.getValue().get(node.timeoutMs, TimeUnit.MILLISECONDS);
                        outputs.put(node.id, result.output);
                        completed.add(node.id);
                        stepResponses.add(mapStep(result.step));
                    } catch (Exception ex) {
                        status = "FAILED";
                        entry.getValue().cancel(true);
                        stepResponses.add(errorStep(node, ex));
                        traceService.finish(trace.getId(), status);
                        return response(workflow, trace, status, outputs, stepResponses, checkpoint(nodes, completed, null));
                    }
                }
            }
        } finally {
            executor.shutdownNow();
        }
        traceService.finish(trace.getId(), status);
        return response(workflow, trace, status, outputs, stepResponses, checkpoint(nodes, completed, null));
    }

    private NodeResult executeNodeWithRetry(TraceEntity trace, WorkflowDefinitionEntity workflow, Node node, String nodeInput) {
        Exception last = null;
        int attempts = Math.max(1, node.retry + 1);
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String sessionId = UUID.randomUUID().toString();
            String stepKey = attempts > 1 ? node.id + "#attempt-" + attempt : node.id;
            StepRecordEntity step = traceService.startStep(trace.getId(), sessionId, workflow.getId(), stepKey, node.agentId, nodeInput);
            try {
                String output = executeNode(node, sessionId, nodeInput);
                return new NodeResult(output, traceService.completeStep(step, output));
            } catch (Exception ex) {
                last = ex;
                traceService.failStep(step, ex.getMessage());
            }
        }
        if (!node.fallback.isEmpty()) {
            StepRecordEntity step = traceService.startStep(trace.getId(), null, workflow.getId(), node.id + "#fallback", node.agentId, nodeInput);
            return new NodeResult(node.fallback, traceService.completeStep(step, node.fallback));
        }
        throw new IllegalStateException(last != null ? last.getMessage() : "Workflow node failed");
    }

    private String executeNode(Node node, String sessionId, String input) {
        Agent agent = agentRepository.findById(node.agentId)
                .orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + node.agentId));
        Session session = new Session();
        session.setId(sessionId);
        session.setAgentId(node.agentId);
        session.setUserId(TenantContext.userId());
        session.setMessages(new ArrayList<>());
        session.setStatus(Session.SessionStatus.ACTIVE);
        sessionRepository.save(session);

        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(Message.MessageRole.USER);
        message.setContent(input);
        message.setCreatedAt(LocalDateTime.now());
        List<Message> generated = reActEngine.executeReActLoop(agent, session, message);
        session.setMessages(generated);
        sessionRepository.save(session);
        return lastContent(generated);
    }

    @SuppressWarnings("unchecked")
    private List<Node> parseNodes(String definition) {
        try {
            Object parsed = objectMapper.readValue(definition, Object.class);
            if (parsed instanceof String) {
                parsed = objectMapper.readValue((String) parsed, Object.class);
            }
            Map<String, Object> root = (Map<String, Object>) parsed;
            Object rawNodes = root.get("nodes");
            if (!(rawNodes instanceof List)) {
                throw new IllegalArgumentException("Workflow definition requires nodes array");
            }
            List<Node> nodes = new ArrayList<>();
            for (Object rawNode : (List<Object>) rawNodes) {
                Map<String, Object> item = (Map<String, Object>) rawNode;
                Node node = new Node();
                node.id = stringValue(item.get("id"), "");
                node.agentId = stringValue(item.get("agentId"), "");
                node.input = stringValue(item.get("input"), "");
                node.type = stringValue(item.get("type"), "agent");
                node.retry = intValue(item.get("retry"), 0);
                node.timeoutMs = intValue(item.get("timeoutMs"), 30000);
                node.fallback = stringValue(item.get("fallback"), "");
                Object dependsOn = item.get("dependsOn");
                if (dependsOn instanceof List) {
                    for (Object dependency : (List<Object>) dependsOn) {
                        node.dependsOn.add(String.valueOf(dependency));
                    }
                }
                if (node.id.isEmpty() || (!node.human() && node.agentId.isEmpty())) {
                    throw new IllegalArgumentException("Workflow node requires id and agentId for agent nodes");
                }
                nodes.add(node);
            }
            return nodes;
        } catch (Exception ex) {
            throw new IllegalArgumentException("Invalid workflow definition", ex);
        }
    }

    private List<Node> topologicalSort(List<Node> nodes) {
        Map<String, Node> byId = new LinkedHashMap<>();
        for (Node node : nodes) {
            byId.put(node.id, node);
        }
        List<Node> ordered = new ArrayList<>();
        Set<String> visiting = new HashSet<>();
        Set<String> visited = new HashSet<>();
        for (Node node : nodes) {
            visit(node, byId, visiting, visited, ordered);
        }
        return ordered;
    }

    private List<Node> readyNodes(List<Node> nodes, Set<String> completed) {
        return nodes.stream()
                .filter(node -> !completed.contains(node.id))
                .filter(node -> completed.containsAll(node.dependsOn))
                .collect(Collectors.toList());
    }

    private void visit(Node node, Map<String, Node> byId, Set<String> visiting, Set<String> visited, List<Node> ordered) {
        if (visited.contains(node.id)) return;
        if (visiting.contains(node.id)) {
            throw new IllegalArgumentException("Workflow contains cycle at node: " + node.id);
        }
        visiting.add(node.id);
        for (String dependency : node.dependsOn) {
            Node dependencyNode = byId.get(dependency);
            if (dependencyNode == null) {
                throw new IllegalArgumentException("Unknown workflow dependency: " + dependency);
            }
            visit(dependencyNode, byId, visiting, visited, ordered);
        }
        visiting.remove(node.id);
        visited.add(node.id);
        ordered.add(node);
    }

    private String buildNodeInput(String workflowInput, Node node, Map<String, String> outputs) {
        StringBuilder input = new StringBuilder();
        if (!node.input.isEmpty()) {
            input.append(node.input);
        } else {
            input.append(workflowInput);
        }
        if (!node.dependsOn.isEmpty()) {
            input.append("\n\nUpstream outputs:");
            for (String dependency : node.dependsOn) {
                input.append("\n[").append(dependency).append("] ").append(outputs.get(dependency));
            }
        }
        return input.toString();
    }

    private String lastContent(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.get(messages.size() - 1).getContent();
    }

    private Map<String, Object> mapStep(StepRecordEntity step) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", step.getId());
        response.put("stepKey", step.getStepKey());
        response.put("agentId", step.getAgentId());
        response.put("status", step.getStatus());
        response.put("sessionId", step.getSessionId());
        response.put("output", step.getOutput());
        response.put("error", step.getError());
        return response;
    }

    private Map<String, Object> response(
            WorkflowDefinitionEntity workflow,
            TraceEntity trace,
            String status,
            Map<String, String> outputs,
            List<Map<String, Object>> stepResponses,
            Map<String, Object> checkpoint) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("workflowId", workflow.getId());
        response.put("traceId", trace.getId());
        response.put("status", status);
        response.put("outputs", outputs);
        response.put("steps", stepResponses);
        response.put("checkpoint", checkpoint);
        return response;
    }

    private Map<String, Object> checkpoint(List<Node> nodes, Set<String> completed, String waitingNodeId) {
        Map<String, Object> checkpoint = new LinkedHashMap<>();
        checkpoint.put("completedNodeIds", new ArrayList<>(completed));
        checkpoint.put("waitingNodeId", waitingNodeId);
        List<String> pending = nodes.stream()
                .map(node -> node.id)
                .filter(id -> !completed.contains(id))
                .collect(Collectors.toList());
        checkpoint.put("pendingNodeIds", pending);
        return checkpoint;
    }

    private Map<String, Object> errorStep(Node node, Exception ex) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("stepKey", node.id);
        response.put("agentId", node.agentId);
        response.put("status", "FAILED");
        response.put("error", ex.getMessage());
        return response;
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) return Integer.parseInt((String) value);
        return fallback;
    }

    private String toJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (Exception ex) {
            return "{}";
        }
    }

    private static class Node {
        private String id;
        private String agentId;
        private String input;
        private String type;
        private String fallback;
        private int retry;
        private int timeoutMs = 30000;
        private final List<String> dependsOn = new ArrayList<>();

        private boolean human() {
            return "human".equalsIgnoreCase(type) || "approval".equalsIgnoreCase(type) || "human-in-loop".equalsIgnoreCase(type);
        }
    }

    private static class NodeResult {
        private final String output;
        private final StepRecordEntity step;

        private NodeResult(String output, StepRecordEntity step) {
            this.output = output;
            this.step = step;
        }
    }
}
