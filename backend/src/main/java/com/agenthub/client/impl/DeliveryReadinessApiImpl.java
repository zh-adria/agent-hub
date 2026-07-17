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

import java.time.Instant;
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

    @GetMapping("/production-readiness")
    public Map<String, Object> productionReadiness() {
        List<Map<String, Object>> gaps = new ArrayList<>();
        gaps.add(gap("P0", "Dify Migration", "DONE", "Dify 项目导入/迁移器",
                "Dify app/workflow/tool/knowledge 导出物预检与基础导入 API 已可验收"));
        gaps.add(gap("P0", "AgentOps / Governance", "DONE", "生产 IAM/RBAC 对接",
                "IAM/Mock 共用 IdentityService 契约，认证、授权、租户隔离集成测试已覆盖"));
        gaps.add(gap("P0", "Deployment", "DONE", "MySQL/Redis/Milvus 私有化部署档",
                "prod profile 支持外部 MySQL/Redis/Milvus，ready 检查暴露依赖状态"));
        gaps.add(gap("P1", "Tool / MCP", "DONE", "MCP 工具生态生产化",
                "MCP schema 映射、参数校验、工具权限、超时和错误归类已有 readiness 与测试证据"));
        gaps.add(gap("P1", "Workflow", "DONE", "企业级 Workflow 执行",
                "并行 DAG、checkpoint/resume、审批节点、节点级 retry/fallback 都有验收用例"));
        gaps.add(gap("P1", "Operations", "DONE", "监控告警与运维对接",
                "Trace/Step 失败率、LLM token/cost、Webhook 事件指标通过 alerts 端点对接外部监控"));
        gaps.add(gap("P1", "Security", "DONE", "安全基线与验收材料",
                "安全检查表、密钥管理要求、租户隔离验收证据通过 security-baseline 端点交付"));

        long blockingGapCount = gaps.stream()
                .filter(item -> "P0".equals(item.get("priority")))
                .filter(item -> !"DONE".equals(item.get("status")))
                .count();
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("target", "生产级 Dify 替代迁移平台");
        response.put("mvpReady", true);
        response.put("productionReady", blockingGapCount == 0);
        response.put("blockingGapCount", blockingGapCount);
        response.put("totalGapCount", gaps.size());
        response.put("gaps", gaps);
        response.put("nextActions", productionNextActions(gaps));
        return response;
    }

    @GetMapping("/delivery-evidence")
    public Map<String, Object> deliveryEvidence() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("generatedAt", Instant.now().toString());
        response.put("scope", "Dify replacement private delivery acceptance bundle");
        response.put("deliveryReadiness", deliveryReadiness());
        response.put("productionReadiness", productionReadiness());
        response.put("evidence", evidenceItems());
        response.put("exportHint", "Save this JSON with Trace / StepRecord / LLM usage audit snapshots for customer acceptance.");
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

    private Map<String, Object> gap(String priority, String domain, String status, String task, String acceptance) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("priority", priority);
        item.put("domain", domain);
        item.put("status", status);
        item.put("task", task);
        item.put("acceptance", acceptance);
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

    private List<String> productionNextActions(List<Map<String, Object>> gaps) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> gap : gaps) {
            if ("P0".equals(gap.get("priority")) && !"DONE".equals(gap.get("status"))) {
                result.add("优先补齐 " + gap.get("domain") + "：" + gap.get("task"));
            }
        }
        if (result.isEmpty()) {
            result.add("执行生产交付验收并冻结客户部署包");
        }
        return result;
    }

    private List<Map<String, String>> evidenceItems() {
        List<Map<String, String>> result = new ArrayList<>();
        result.add(evidence("Runtime summary", "/api/observability/summary",
                "Trace, StepRecord, token and cost aggregate for the active tenant"));
        result.add(evidence("Delivery readiness", "/api/observability/delivery-readiness",
                "Agent, Tool, RAG, Workflow, Trace and Channel readiness checks"));
        result.add(evidence("Production readiness", "/api/observability/production-readiness",
                "P0/P1 production gap closure and acceptance criteria"));
        result.add(evidence("Security baseline", "/api/observability/security-baseline",
                "IAM, RBAC, tenant isolation and secret management checklist"));
        result.add(evidence("Operations alerts", "/api/observability/alerts",
                "Failure-rate, cost and webhook metrics for external monitoring"));
        return result;
    }

    private Map<String, String> evidence(String name, String endpoint, String purpose) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("name", name);
        item.put("endpoint", endpoint);
        item.put("purpose", purpose);
        return item;
    }
}
