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
        List<Node> ordered = topologicalSort(nodes);
        String input = stringValue(payload.get("input"), "");
        TraceEntity trace = traceService.start("workflow:" + workflow.getName(), null, workflow.getId(), toJson(payload));
        Map<String, String> outputs = new LinkedHashMap<>();
        List<Map<String, Object>> stepResponses = new ArrayList<>();
        String status = "SUCCEEDED";

        for (Node node : ordered) {
            String nodeInput = buildNodeInput(input, node, outputs);
            String sessionId = UUID.randomUUID().toString();
            StepRecordEntity step = traceService.startStep(trace.getId(), sessionId, workflow.getId(), node.id, node.agentId, nodeInput);
            try {
                String output = executeNode(node, sessionId, nodeInput);
                outputs.put(node.id, output);
                step = traceService.completeStep(step, output);
            } catch (Exception ex) {
                status = "FAILED";
                step = traceService.failStep(step, ex.getMessage());
                stepResponses.add(mapStep(step));
                break;
            }
            stepResponses.add(mapStep(step));
        }
        traceService.finish(trace.getId(), status);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("workflowId", workflow.getId());
        response.put("traceId", trace.getId());
        response.put("status", status);
        response.put("outputs", outputs);
        response.put("steps", stepResponses);
        return response;
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
            Map<String, Object> root = objectMapper.readValue(definition, new TypeReference<Map<String, Object>>() {});
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
                Object dependsOn = item.get("dependsOn");
                if (dependsOn instanceof List) {
                    for (Object dependency : (List<Object>) dependsOn) {
                        node.dependsOn.add(String.valueOf(dependency));
                    }
                }
                if (node.id.isEmpty() || node.agentId.isEmpty()) {
                    throw new IllegalArgumentException("Workflow node requires id and agentId");
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

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
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
        private final List<String> dependsOn = new ArrayList<>();
    }
}
