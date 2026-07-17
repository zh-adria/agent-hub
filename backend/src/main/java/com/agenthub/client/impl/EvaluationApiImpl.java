package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.exception.ResourceNotFoundException;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.domain.service.SessionMessageService;
import com.agenthub.infra.persistence.entity.EvaluationCaseResultEntity;
import com.agenthub.infra.persistence.entity.EvaluationRunEntity;
import com.agenthub.infra.persistence.repository.EvaluationCaseResultJpaRepository;
import com.agenthub.infra.persistence.repository.EvaluationRunJpaRepository;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/evaluations")
public class EvaluationApiImpl {
    private final EvaluationRunJpaRepository runRepository;
    private final EvaluationCaseResultJpaRepository caseResultRepository;
    private final AgentRepository agentRepository;
    private final SessionRepository sessionRepository;
    private final SessionMessageService sessionMessageService;

    public EvaluationApiImpl(
            EvaluationRunJpaRepository runRepository,
            EvaluationCaseResultJpaRepository caseResultRepository,
            AgentRepository agentRepository,
            SessionRepository sessionRepository,
            SessionMessageService sessionMessageService) {
        this.runRepository = runRepository;
        this.caseResultRepository = caseResultRepository;
        this.agentRepository = agentRepository;
        this.sessionRepository = sessionRepository;
        this.sessionMessageService = sessionMessageService;
    }

    @PostMapping("/runs")
    public Map<String, Object> run(@RequestBody Map<String, Object> payload) {
        String agentId = String.valueOf(payload.get("agentId"));
        agentRepository.findById(agentId).orElseThrow(() -> new ResourceNotFoundException("Agent not found: " + agentId));
        List<Map<String, Object>> cases = cases(payload);
        EvaluationRunEntity run = new EvaluationRunEntity();
        run.setTenantId(TenantContext.tenantId());
        run.setName(stringValue(payload.get("name"), "evaluation"));
        run.setAgentId(agentId);
        run.setStatus("RUNNING");
        run.setTotalCases(cases.size());
        run.setPassedCases(0);
        run.setScore(0.0);
        run.setCreatedBy(TenantContext.userId());
        run = runRepository.save(run);

        int passed = 0;
        for (int i = 0; i < cases.size(); i++) {
            EvaluationCaseResultEntity result = executeCase(run.getId(), agentId, i, cases.get(i));
            if (Boolean.TRUE.equals(result.getPassed())) {
                passed++;
            }
            caseResultRepository.save(result);
        }
        run.setPassedCases(passed);
        run.setScore(cases.isEmpty() ? 1.0 : (double) passed / cases.size());
        run.setStatus("SUCCEEDED");
        run = runRepository.save(run);
        return mapRun(run, true);
    }

    @GetMapping("/runs")
    public List<Map<String, Object>> listRuns() {
        return runRepository.findByTenantIdOrderByCreatedAtDesc(TenantContext.tenantId()).stream()
                .map(run -> mapRun(run, false))
                .collect(Collectors.toList());
    }

    @GetMapping("/runs/{runId}")
    public Map<String, Object> getRun(@PathVariable Long runId) {
        EvaluationRunEntity run = runRepository.findByIdAndTenantId(runId, TenantContext.tenantId())
                .orElseThrow(() -> new ResourceNotFoundException("Evaluation run not found: " + runId));
        return mapRun(run, true);
    }

    @GetMapping("/metrics")
    public List<Map<String, Object>> metrics() {
        List<Map<String, Object>> result = new ArrayList<>();
        for (EvaluationMetric metric : metricPlugins()) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("name", metric.name());
            item.put("description", metric.description());
            result.add(item);
        }
        return result;
    }

    private EvaluationCaseResultEntity executeCase(Long runId, String agentId, int index, Map<String, Object> item) {
        String input = stringValue(item.get("input"), "");
        String expected = stringValue(item.get("expected"), "");
        EvaluationCaseResultEntity result = new EvaluationCaseResultEntity();
        result.setTenantId(TenantContext.tenantId());
        result.setRunId(runId);
        result.setCaseKey(stringValue(item.get("id"), "case-" + (index + 1)));
        result.setInput(input);
        result.setExpected(expected);
        try {
            String sessionId = UUID.randomUUID().toString();
            Session session = new Session();
            session.setId(sessionId);
            session.setAgentId(agentId);
            session.setUserId(TenantContext.userId());
            session.setMessages(new ArrayList<>());
            session.setStatus(Session.SessionStatus.ACTIVE);
            sessionRepository.save(session);
            Message response = sessionMessageService.send(sessionId, sessionMessageService.newUserMessage(sessionId, input));
            result.setActual(response.getContent());
            result.setPassed(evaluate(item, expected, response.getContent()));
        } catch (Exception ex) {
            result.setActual("");
            result.setPassed(false);
            result.setError(ex.getMessage());
        }
        return result;
    }

    private Map<String, Object> mapRun(EvaluationRunEntity run, boolean includeCases) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", run.getId());
        response.put("name", run.getName());
        response.put("agentId", run.getAgentId());
        response.put("status", run.getStatus());
        response.put("totalCases", run.getTotalCases());
        response.put("passedCases", run.getPassedCases());
        response.put("score", run.getScore());
        response.put("createdAt", run.getCreatedAt());
        if (includeCases) {
            response.put("cases", caseResultRepository.findByRunIdAndTenantId(run.getId(), TenantContext.tenantId()).stream()
                    .map(this::mapCase)
                    .collect(Collectors.toList()));
        }
        return response;
    }

    private Map<String, Object> mapCase(EvaluationCaseResultEntity item) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", item.getId());
        response.put("caseKey", item.getCaseKey());
        response.put("input", item.getInput());
        response.put("expected", item.getExpected());
        response.put("actual", item.getActual());
        response.put("passed", item.getPassed());
        response.put("error", item.getError());
        return response;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> cases(Map<String, Object> payload) {
        Object cases = payload.get("cases");
        if (cases instanceof List) {
            return (List<Map<String, Object>>) cases;
        }
        return Collections.emptyList();
    }

    private String stringValue(Object value, String fallback) {
        return value != null ? String.valueOf(value) : fallback;
    }

    private boolean evaluate(Map<String, Object> item, String expected, String actual) {
        List<String> metricNames = metricNames(item);
        if (metricNames.isEmpty()) {
            metricNames.add("contains");
        }
        for (String metricName : metricNames) {
            if (!metric(metricName).evaluate(expected, actual)) {
                return false;
            }
        }
        return true;
    }

    @SuppressWarnings("unchecked")
    private List<String> metricNames(Map<String, Object> item) {
        Object value = item.get("metrics");
        List<String> result = new ArrayList<>();
        if (value instanceof List) {
            for (Object metric : (List<Object>) value) {
                if (metric != null && !String.valueOf(metric).trim().isEmpty()) {
                    result.add(String.valueOf(metric));
                }
            }
        } else if (value instanceof String && !((String) value).trim().isEmpty()) {
            result.add((String) value);
        }
        return result;
    }

    private EvaluationMetric metric(String name) {
        for (EvaluationMetric metric : metricPlugins()) {
            if (metric.name().equalsIgnoreCase(name)) {
                return metric;
            }
        }
        throw new IllegalArgumentException("Unsupported evaluation metric: " + name);
    }

    private List<EvaluationMetric> metricPlugins() {
        List<EvaluationMetric> result = new ArrayList<>();
        result.add(new ContainsMetric());
        result.add(new ExactMetric());
        result.add(new RegexMetric());
        return result;
    }

    private interface EvaluationMetric {
        String name();
        String description();
        boolean evaluate(String expected, String actual);
    }

    private static class ContainsMetric implements EvaluationMetric {
        public String name() { return "contains"; }
        public String description() { return "Actual answer contains expected text"; }
        public boolean evaluate(String expected, String actual) {
            return expected == null || expected.isEmpty() || (actual != null && actual.contains(expected));
        }
    }

    private static class ExactMetric implements EvaluationMetric {
        public String name() { return "exact"; }
        public String description() { return "Actual answer exactly equals expected text"; }
        public boolean evaluate(String expected, String actual) {
            return expected == null || expected.isEmpty() || expected.equals(actual);
        }
    }

    private static class RegexMetric implements EvaluationMetric {
        public String name() { return "regex"; }
        public String description() { return "Actual answer matches expected regular expression"; }
        public boolean evaluate(String expected, String actual) {
            return expected == null || expected.isEmpty() || (actual != null && Pattern.compile(expected).matcher(actual).find());
        }
    }
}
