package com.agenthub.domain.service;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class TraceService {
    private final TraceJpaRepository traceRepository;
    private final StepRecordJpaRepository stepRecordRepository;

    public TraceService(TraceJpaRepository traceRepository, StepRecordJpaRepository stepRecordRepository) {
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
    }

    public TraceEntity start(String name, String sessionId, Long workflowId, String metadata) {
        TraceEntity trace = new TraceEntity();
        trace.setId(UUID.randomUUID().toString());
        trace.setTenantId(TenantContext.tenantId());
        trace.setSessionId(sessionId);
        trace.setWorkflowId(workflowId);
        trace.setName(name);
        trace.setStatus("RUNNING");
        trace.setUserId(TenantContext.userId());
        trace.setMetadata(metadata);
        trace.setStartedAt(LocalDateTime.now());
        return traceRepository.save(trace);
    }

    public StepRecordEntity startStep(String traceId, String sessionId, Long workflowId, String stepKey, String agentId, String input) {
        StepRecordEntity step = new StepRecordEntity();
        step.setTenantId(TenantContext.tenantId());
        step.setTraceId(traceId);
        step.setSessionId(sessionId);
        step.setWorkflowId(workflowId);
        step.setStepKey(stepKey);
        step.setAgentId(agentId);
        step.setInput(input);
        step.setStatus("RUNNING");
        step.setStartedAt(LocalDateTime.now());
        return stepRecordRepository.save(step);
    }

    public StepRecordEntity completeStep(StepRecordEntity step, String output) {
        step.setStatus("SUCCEEDED");
        step.setOutput(output);
        step.setEndedAt(LocalDateTime.now());
        return stepRecordRepository.save(step);
    }

    public StepRecordEntity failStep(StepRecordEntity step, String error) {
        step.setStatus("FAILED");
        step.setError(error);
        step.setEndedAt(LocalDateTime.now());
        return stepRecordRepository.save(step);
    }

    public StepRecordEntity saveStep(StepRecordEntity step) {
        return stepRecordRepository.save(step);
    }

    public TraceEntity finish(String traceId, String status) {
        TraceEntity trace = traceRepository.findByIdAndTenantId(traceId, TenantContext.tenantId())
                .orElseThrow(() -> new IllegalArgumentException("Trace not found: " + traceId));
        trace.setStatus(status);
        trace.setEndedAt(LocalDateTime.now());
        return traceRepository.save(trace);
    }

    public List<TraceEntity> listTraces() {
        return traceRepository.findByTenantIdOrderByStartedAtDesc(TenantContext.tenantId());
    }

    public List<StepRecordEntity> listSteps(String traceId) {
        return stepRecordRepository.findByTraceIdAndTenantIdOrderByStartedAtAsc(traceId, TenantContext.tenantId());
    }
}
