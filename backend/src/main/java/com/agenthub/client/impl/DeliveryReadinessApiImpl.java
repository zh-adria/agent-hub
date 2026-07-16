package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.AgentJpaRepository;
import com.agenthub.infra.persistence.repository.BotBindingJpaRepository;
import com.agenthub.infra.persistence.repository.FunctionDefinitionJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.StepRecordJpaRepository;
import com.agenthub.infra.persistence.repository.TraceJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/observability")
public class DeliveryReadinessApiImpl {
    private final AgentJpaRepository agentRepository;
    private final FunctionDefinitionJpaRepository functionRepository;
    private final KnowledgeBaseJpaRepository knowledgeBaseRepository;
    private final WorkflowDefinitionJpaRepository workflowRepository;
    private final TraceJpaRepository traceRepository;
    private final StepRecordJpaRepository stepRecordRepository;
    private final BotBindingJpaRepository botBindingRepository;

    public DeliveryReadinessApiImpl(
            AgentJpaRepository agentRepository,
            FunctionDefinitionJpaRepository functionRepository,
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            WorkflowDefinitionJpaRepository workflowRepository,
            TraceJpaRepository traceRepository,
            StepRecordJpaRepository stepRecordRepository,
            BotBindingJpaRepository botBindingRepository) {
        this.agentRepository = agentRepository;
        this.functionRepository = functionRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.workflowRepository = workflowRepository;
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.botBindingRepository = botBindingRepository;
    }

    @GetMapping("/delivery-readiness")
    public Map<String, Object> deliveryReadiness() {
        Long tenantId = TenantContext.tenantId();
        List<Map<String, Object>> checks = new ArrayList<>();
        checks.add(check("Agent Runtime", agentRepository.findByTenantId(tenantId).size(), "Agent 可创建并按租户隔离"));
        checks.add(check("Tool / MCP", functionRepository.findByTenantId(tenantId).size(), "工具可注册、发现和调用"));
        checks.add(check("Knowledge / RAG", knowledgeBaseRepository.findByTenantId(tenantId).size(), "知识库可创建并检索"));
        checks.add(check("Workflow", workflowRepository.findByTenantId(tenantId).size(), "工作流定义可迁移和执行"));
        checks.add(check("Trace Audit", traceRepository.findByTenantIdOrderByStartedAtDesc(tenantId).size(), "执行链路可追溯"));
        checks.add(check("Step Records", stepRecordRepository.countByTenantId(tenantId), "步骤级输入输出和错误可审计"));
        checks.add(check("Enterprise Channels", botBindingRepository.findByTenantId(tenantId).size(), "企业通道可绑定 Agent"));

        long readyCount = checks.stream().filter(item -> Boolean.TRUE.equals(item.get("ready"))).count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("scenario", "Dify replacement private delivery");
        response.put("readyCount", readyCount);
        response.put("totalCount", checks.size());
        response.put("readinessScore", checks.isEmpty() ? 0 : Math.round((readyCount * 100.0 / checks.size())));
        response.put("checks", checks);
        response.put("nextActions", nextActions(checks));
        return response;
    }

    private Map<String, Object> check(String domain, long count, String evidence) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("domain", domain);
        item.put("ready", count > 0);
        item.put("count", count);
        item.put("evidence", evidence);
        return item;
    }

    private List<String> nextActions(List<Map<String, Object>> checks) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> check : checks) {
            if (!Boolean.TRUE.equals(check.get("ready"))) {
                result.add("补齐 " + check.get("domain") + " 演示数据和验收用例");
            }
        }
        if (result.isEmpty()) {
            result.add("执行端到端迁移演示并导出 Trace / StepRecord / audit 证据");
        }
        return result;
    }
}
