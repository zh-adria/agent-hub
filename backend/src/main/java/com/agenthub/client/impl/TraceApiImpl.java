package com.agenthub.client.impl;

import com.agenthub.domain.service.TraceService;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/traces")
public class TraceApiImpl {
    private final TraceService traceService;

    public TraceApiImpl(TraceService traceService) {
        this.traceService = traceService;
    }

    @GetMapping
    public List<Map<String, Object>> listTraces() {
        return traceService.listTraces().stream().map(this::mapTrace).collect(Collectors.toList());
    }

    @GetMapping("/{traceId}/steps")
    public List<Map<String, Object>> listSteps(@PathVariable String traceId) {
        return traceService.listSteps(traceId).stream().map(this::mapStep).collect(Collectors.toList());
    }

    private Map<String, Object> mapTrace(TraceEntity trace) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", trace.getId());
        response.put("sessionId", trace.getSessionId());
        response.put("workflowId", trace.getWorkflowId());
        response.put("name", trace.getName());
        response.put("status", trace.getStatus());
        response.put("userId", trace.getUserId());
        response.put("startedAt", trace.getStartedAt());
        response.put("endedAt", trace.getEndedAt());
        return response;
    }

    private Map<String, Object> mapStep(StepRecordEntity step) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", step.getId());
        response.put("traceId", step.getTraceId());
        response.put("sessionId", step.getSessionId());
        response.put("workflowId", step.getWorkflowId());
        response.put("stepKey", step.getStepKey());
        response.put("agentId", step.getAgentId());
        response.put("status", step.getStatus());
        response.put("input", step.getInput());
        response.put("output", step.getOutput());
        response.put("error", step.getError());
        response.put("startedAt", step.getStartedAt());
        response.put("endedAt", step.getEndedAt());
        return response;
    }
}
