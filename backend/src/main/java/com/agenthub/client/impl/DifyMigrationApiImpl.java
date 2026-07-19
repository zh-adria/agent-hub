package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.service.FunctionRegistryService;
import com.agenthub.infra.persistence.entity.DifyMigrationResultEntity;
import com.agenthub.infra.persistence.entity.KnowledgeBaseEntity;
import com.agenthub.infra.persistence.entity.RagDocumentEntity;
import com.agenthub.infra.persistence.entity.WorkflowDefinitionEntity;
import com.agenthub.infra.persistence.repository.DifyMigrationResultJpaRepository;
import com.agenthub.infra.persistence.repository.KnowledgeBaseJpaRepository;
import com.agenthub.infra.persistence.repository.RagDocumentJpaRepository;
import com.agenthub.infra.persistence.repository.WorkflowDefinitionJpaRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/migrations/dify")
public class DifyMigrationApiImpl {
    private final AgentRepository agentRepository;
    private final FunctionRegistryService functionRegistryService;
    private final WorkflowDefinitionJpaRepository workflowRepository;
    private final KnowledgeBaseJpaRepository knowledgeBaseRepository;
    private final RagDocumentJpaRepository documentRepository;
    private final DifyMigrationResultJpaRepository resultRepository;
    private final ObjectMapper objectMapper;

    public DifyMigrationApiImpl(
            AgentRepository agentRepository,
            FunctionRegistryService functionRegistryService,
            WorkflowDefinitionJpaRepository workflowRepository,
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            RagDocumentJpaRepository documentRepository,
            DifyMigrationResultJpaRepository resultRepository,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.functionRegistryService = functionRegistryService;
        this.workflowRepository = workflowRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.documentRepository = documentRepository;
        this.resultRepository = resultRepository;
        this.objectMapper = objectMapper;
    }

    @PostMapping("/preflight")
    public Map<String, Object> preflight(@RequestBody Map<String, Object> payload) {
        return preflightReport(payload);
    }

    @PostMapping("/import")
    public Map<String, Object> importExport(@RequestBody Map<String, Object> payload) {
        Map<String, Object> preflight = preflightReport(payload);
        if (!((List<?>) preflight.get("blockers")).isEmpty()) {
            throw new IllegalArgumentException("Dify export has blocking migration issues");
        }

        List<Map<String, Object>> importedFunctions = importTools(tools(payload));
        List<String> functionIds = ids(importedFunctions);
        List<Map<String, Object>> importedAgents = importApps(apps(payload), functionIds);
        List<Map<String, Object>> importedWorkflows = importWorkflows(workflows(payload));
        Map<String, List<Map<String, Object>>> importedKnowledge = importKnowledgeBases(knowledgeBases(payload));

        // Persist import results for visualization
        saveMigrationResults(payload, preflight, importedFunctions, importedAgents, importedWorkflows, importedKnowledge);

        Map<String, Object> imported = new LinkedHashMap<>();
        imported.put("agents", importedAgents);
        imported.put("functions", importedFunctions);
        imported.put("workflows", importedWorkflows);
        imported.put("knowledgeBases", importedKnowledge.get("knowledgeBases"));
        imported.put("documents", importedKnowledge.get("documents"));

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("source", "dify");
        response.put("preflight", preflight);
        response.put("imported", imported);
        return response;
    }

    @GetMapping("/results")
    public List<Map<String, Object>> listResults() {
        long tenantId = TenantContext.tenantId();
        return resultRepository.findByTenantIdOrderByCreatedAtDesc(tenantId).stream()
                .map(this::mapResult).toList();
    }

    private void saveMigrationResults(
            Map<String, Object> payload,
            Map<String, Object> preflight,
            List<Map<String, Object>> importedFunctions,
            List<Map<String, Object>> importedAgents,
            List<Map<String, Object>> importedWorkflows,
            Map<String, List<Map<String, Object>>> importedKnowledge) {
        long tenantId = TenantContext.tenantId();
        Object mappingsObj = preflight.get("mappings");
        if (!(mappingsObj instanceof List<?> rawMappings)) {
            return;
        }
        List<Map<String, Object>> mappings = new ArrayList<>();
        for (Object item : rawMappings) {
            if (item instanceof Map) {
                @SuppressWarnings("unchecked")
                Map<String, Object> map = (Map<String, Object>) item;
                mappings.add(map);
            }
        }
        if (mappings.isEmpty()) {
            return;
        }

        if (mappings != null) {
            for (Map<String, Object> mapping : mappings) {
                DifyMigrationResultEntity entity = new DifyMigrationResultEntity();
                entity.setTenantId(tenantId);
                entity.setSourceName(text(mapping.get("sourceName")));
                entity.setStatus("READY".equals(mapping.get("status")) ? "SUCCEEDED" : "FAILED");
                entity.setTargetType(text(mapping.get("targetType")));
                entity.setSourceType("dify");
                entity.setMappingDetail(json(mapping));
                if (!"READY".equals(mapping.get("status"))) {
                    entity.setErrorMessage("Missing name or blocked by preflight check");
                }
                resultRepository.save(entity);
            }
        }

        // Save agent-level results
        for (Map<String, Object> agent : importedAgents) {
            DifyMigrationResultEntity entity = new DifyMigrationResultEntity();
            entity.setTenantId(tenantId);
            entity.setSourceName(text(agent.get("name")));
            entity.setStatus("SUCCEEDED");
            entity.setTargetType("Agent");
            entity.setSourceType("dify-app");
            entity.setMappingDetail(json(agent));
            resultRepository.save(entity);
        }

        // Save function-level results
        for (Map<String, Object> func : importedFunctions) {
            DifyMigrationResultEntity entity = new DifyMigrationResultEntity();
            entity.setTenantId(tenantId);
            entity.setSourceName(text(func.get("name")));
            entity.setStatus("SUCCEEDED");
            entity.setTargetType("FunctionDefinition");
            entity.setSourceType("dify-tool");
            entity.setMappingDetail(json(func));
            resultRepository.save(entity);
        }

        // Save workflow-level results
        for (Map<String, Object> wf : importedWorkflows) {
            DifyMigrationResultEntity entity = new DifyMigrationResultEntity();
            entity.setTenantId(tenantId);
            entity.setSourceName(text(wf.get("name")));
            entity.setStatus("SUCCEEDED");
            entity.setTargetType("WorkflowDefinition");
            entity.setSourceType("dify-workflow");
            entity.setMappingDetail(json(wf));
            resultRepository.save(entity);
        }
    }

    private Map<String, Object> preflightReport(Map<String, Object> payload) {
        List<Map<String, Object>> apps = apps(payload);
        List<Map<String, Object>> workflows = workflows(payload);
        List<Map<String, Object>> tools = tools(payload);
        List<Map<String, Object>> knowledgeBases = knowledgeBases(payload);
        int documentCount = documentCount(knowledgeBases);

        List<String> blockers = new ArrayList<>();
        List<Map<String, String>> risks = new ArrayList<>();
        List<Map<String, String>> compatibilityIssues = new ArrayList<>();

        if (apps.isEmpty() && workflows.isEmpty() && tools.isEmpty() && knowledgeBases.isEmpty()) {
            blockers.add("未发现 Dify app/workflow/tool/knowledge 导出内容");
        }
        for (Map<String, Object> tool : tools) {
            String name = nameOf(tool);
            if (blank(name)) {
                blockers.add("存在缺少 name 的 tool，无法迁移到 FunctionDefinition");
            } else if (blank(text(tool.get("endpoint"))) && blank(text(tool.get("url"))) && blank(text(tool.get("server_url")))) {
                risks.add(risk("MEDIUM", "tool." + name, "缺少 endpoint/url/server_url，导入后需手动配置"));
            }
            if (blank(text(tool.get("input_schema"))) && blank(text(tool.get("inputSchema"))) && blank(text(tool.get("parameters")))) {
                risks.add(risk("LOW", "tool." + name, "缺少 input schema，FunctionDefinition parameters 将为默认值"));
            }
        }

        // Knowledge base permissions check
        for (Map<String, Object> kb : knowledgeBases) {
            String kbName = nameOf(kb);
            if (kb.containsKey("permissions") || kb.containsKey("accessTags")) {
                // Has permission info, good
            } else {
                risks.add(risk("MEDIUM", "knowledgeBase." + (kbName != null ? kbName : "unknown"),
                        "知识库缺少 permissions/accessTags，导入后将继承默认权限策略"));
            }
            List<Map<String, Object>> docs = documents(kb);
            for (Map<String, Object> doc : docs) {
                if (blank(text(doc.get("content"))) && blank(text(doc.get("contentHash")))) {
                    compatibilityIssues.add(compat("document." + text(doc.get("title")),
                            "文档缺少 content 或 contentHash，需重新上传或分块"));
                }
            }
        }

        // Workflow node compatibility
        for (Map<String, Object> wf : workflows) {
            String wfName = nameOf(wf);
            List<Map<String, Object>> nodes = maps(wf.get("nodes"));
            if (nodes.isEmpty()) {
                nodes = maps(wf.get("graph"));
            }
            for (Map<String, Object> node : nodes) {
                String nodeType = text(node.get("type"));
                if (nodeType == null) {
                    nodeType = text(node.get("nodeType"));
                }
                if (nodeType == null) {
                    compatibilityIssues.add(compat("workflow." + (wfName != null ? wfName : "unknown"),
                            "工作流节点缺少 type 字段，AgentHub 将按默认 Agent 节点处理"));
                }
                String agentRef = text(node.get("agentId"));
                if (agentRef == null) {
                    agentRef = text(node.get("appId"));
                }
                if (agentRef == null && ("agent".equalsIgnoreCase(nodeType) || "llm".equalsIgnoreCase(nodeType))) {
                    compatibilityIssues.add(compat("workflow." + (wfName != null ? wfName : "unknown") + ".node",
                            "LLM/Agent 节点缺少 agentId/appId 引用，导入后需绑定 Agent"));
                }
            }
        }

        List<String> warnings = new ArrayList<>();
        if (!workflows.isEmpty()) {
            warnings.add("Dify workflow 会作为 AgentHub workflow definition 原始蓝图导入，复杂节点语义需后续验收");
        }
        if (!knowledgeBases.isEmpty() && documentCount == 0) {
            warnings.add("发现知识库但未发现 documents，导入后需要补充文档或分块");
        }
        if (!tools.isEmpty() && apps.isEmpty()) {
            warnings.add("发现工具但未发现 app，工具导入后需手动绑定到 Agent");
        }

        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("apps", apps.size());
        summary.put("workflows", workflows.size());
        summary.put("tools", tools.size());
        summary.put("knowledgeBases", knowledgeBases.size());
        summary.put("documents", documentCount);
        summary.put("riskCount", risks.size());
        summary.put("compatibilityIssueCount", compatibilityIssues.size());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("tenantId", TenantContext.externalTenantId());
        response.put("source", "dify");
        response.put("ready", blockers.isEmpty());
        response.put("summary", summary);
        response.put("blockers", blockers);
        response.put("warnings", warnings);
        response.put("risks", risks);
        response.put("compatibilityIssues", compatibilityIssues);
        response.put("mappings", mappings(apps, workflows, tools, knowledgeBases));
        return response;
    }

    private Map<String, String> risk(String level, String target, String description) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("level", level);
        item.put("target", target);
        item.put("description", description);
        return item;
    }

    private Map<String, String> compat(String target, String issue) {
        Map<String, String> item = new LinkedHashMap<>();
        item.put("target", target);
        item.put("issue", issue);
        return item;
    }

    private List<Map<String, Object>> importTools(List<Map<String, Object>> tools) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> tool : tools) {
            FunctionDefinition function = new FunctionDefinition();
            function.setName(nameOf(tool));
            function.setDescription(text(firstNonNull(tool.get("description"), tool.get("label"))));
            function.setEndpoint(text(firstNonNull(tool.get("endpoint"), firstNonNull(tool.get("url"), tool.get("server_url")))));
            function.setMethod(text(firstNonNull(tool.get("method"), "POST")));
            function.setParameters(json(firstNonNull(tool.get("input_schema"), firstNonNull(tool.get("inputSchema"), tool.get("parameters")))));
            function.setImplementation("dify-tool");
            function.setTimeoutMs(intValue(tool.get("timeoutMs"), 30000));
            function.setRetryPolicy(text(tool.get("retryPolicy")));
            function.setCircuitBreakerPolicy(text(tool.get("circuitBreakerPolicy")));
            function.setFallbackResponse(text(tool.get("fallbackResponse")));
            function.setOwnerId(TenantContext.userId());
            result.add(mapFunction(functionRegistryService.registerFunction(function)));
        }
        return result;
    }

    private List<Map<String, Object>> importApps(List<Map<String, Object>> apps, List<String> functionIds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : apps) {
            Agent agent = new Agent();
            agent.setName(nameOf(item));
            agent.setDescription(text(firstNonNull(item.get("description"), item.get("mode"))));
            agent.setPrompt(text(firstNonNull(item.get("prompt"), firstNonNull(item.get("system_prompt"), item.get("instructions")))));
            agent.setProvider(text(firstNonNull(item.get("provider"), "dify")));
            agent.setModel(text(firstNonNull(item.get("model"), "gpt-4o-mini")));
            agent.setTemperature(doubleValue(item.get("temperature"), 0.7d));
            agent.setMaxTokens(intValue(item.get("maxTokens"), 2048));
            agent.setFunctionIds(json(functionIds));
            result.add(mapAgent(agentRepository.save(agent)));
        }
        return result;
    }

    private List<Map<String, Object>> importWorkflows(List<Map<String, Object>> workflows) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : workflows) {
            WorkflowDefinitionEntity entity = new WorkflowDefinitionEntity();
            entity.setTenantId(TenantContext.tenantId());
            entity.setName(nameOf(item));
            entity.setDescription(text(firstNonNull(item.get("description"), "Dify workflow import")));
            entity.setDefinition(json(wrapSource(item)));
            entity.setStatus(1);
            entity.setCreatedBy(TenantContext.userId());
            entity.setUpdatedBy(TenantContext.userId());
            result.add(mapWorkflow(workflowRepository.save(entity)));
        }
        return result;
    }

    private Map<String, List<Map<String, Object>>> importKnowledgeBases(List<Map<String, Object>> knowledgeBases) {
        List<Map<String, Object>> importedKnowledgeBases = new ArrayList<>();
        List<Map<String, Object>> importedDocuments = new ArrayList<>();
        for (Map<String, Object> item : knowledgeBases) {
            KnowledgeBaseEntity knowledgeBase = new KnowledgeBaseEntity();
            knowledgeBase.setTenantId(TenantContext.tenantId());
            knowledgeBase.setName(nameOf(item));
            knowledgeBase.setDescription(text(firstNonNull(item.get("description"), item.get("provider"))));
            knowledgeBase.setEmbeddingProvider(text(firstNonNull(item.get("embeddingProvider"), "llm-gateway")));
            knowledgeBase.setEmbeddingModel(text(firstNonNull(item.get("embeddingModel"), "text-embedding")));
            knowledgeBase.setChunkSize(intValue(item.get("chunkSize"), 800));
            knowledgeBase.setChunkOverlap(intValue(item.get("chunkOverlap"), 120));
            knowledgeBase.setMetadata(json(wrapSource(item)));
            knowledgeBase.setStatus(1);
            knowledgeBase.setCreatedBy(TenantContext.userId());
            knowledgeBase.setUpdatedBy(TenantContext.userId());
            KnowledgeBaseEntity savedKnowledgeBase = knowledgeBaseRepository.save(knowledgeBase);
            importedKnowledgeBases.add(mapKnowledgeBase(savedKnowledgeBase));

            for (Map<String, Object> document : documents(item)) {
                RagDocumentEntity entity = new RagDocumentEntity();
                entity.setTenantId(TenantContext.tenantId());
                entity.setKnowledgeBaseId(savedKnowledgeBase.getId());
                entity.setTitle(text(firstNonNull(document.get("title"), firstNonNull(document.get("name"), "Dify document"))));
                entity.setSourceUri(text(firstNonNull(document.get("sourceUri"), firstNonNull(document.get("source_url"), document.get("id")))));
                entity.setMimeType(text(firstNonNull(document.get("mimeType"), "text/plain")));
                entity.setContentHash(text(document.get("contentHash")));
                entity.setMetadata(json(wrapSource(document)));
                entity.setStatus(1);
                entity.setCreatedBy(TenantContext.userId());
                entity.setUpdatedBy(TenantContext.userId());
                importedDocuments.add(mapDocument(documentRepository.save(entity)));
            }
        }
        Map<String, List<Map<String, Object>>> result = new LinkedHashMap<>();
        result.put("knowledgeBases", importedKnowledgeBases);
        result.put("documents", importedDocuments);
        return result;
    }

    private Map<String, Object> mappings(
            List<Map<String, Object>> apps,
            List<Map<String, Object>> workflows,
            List<Map<String, Object>> tools,
            List<Map<String, Object>> knowledgeBases) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("agents", preview(apps, "Agent"));
        result.put("functions", preview(tools, "FunctionDefinition"));
        result.put("workflows", preview(workflows, "WorkflowDefinition"));
        result.put("knowledgeBases", preview(knowledgeBases, "KnowledgeBase"));
        return result;
    }

    private List<Map<String, Object>> preview(List<Map<String, Object>> items, String targetType) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Map<String, Object> mapped = new LinkedHashMap<>();
            mapped.put("sourceName", nameOf(item));
            mapped.put("targetType", targetType);
            mapped.put("status", blank(nameOf(item)) ? "BLOCKED" : "READY");
            result.add(mapped);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> apps(Map<String, Object> payload) {
        List<Map<String, Object>> result = maps(payload.get("apps"));
        Object app = payload.get("app");
        if (app instanceof Map) {
            result.add((Map<String, Object>) app);
        }
        if (result.isEmpty() && (payload.containsKey("name") || payload.containsKey("mode"))) {
            result.add(payload);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> workflows(Map<String, Object> payload) {
        List<Map<String, Object>> result = maps(payload.get("workflows"));
        Object workflow = payload.get("workflow");
        if (workflow instanceof Map) {
            result.add((Map<String, Object>) workflow);
        }
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> tools(Map<String, Object> payload) {
        List<Map<String, Object>> result = maps(payload.get("tools"));
        Object app = payload.get("app");
        if (app instanceof Map) {
            result.addAll(maps(((Map<String, Object>) app).get("tools")));
        }
        return result;
    }

    private List<Map<String, Object>> knowledgeBases(Map<String, Object> payload) {
        List<Map<String, Object>> result = maps(payload.get("knowledgeBases"));
        result.addAll(maps(payload.get("knowledge")));
        result.addAll(maps(payload.get("datasets")));
        return result;
    }

    private int documentCount(List<Map<String, Object>> knowledgeBases) {
        int count = 0;
        for (Map<String, Object> knowledgeBase : knowledgeBases) {
            count += documents(knowledgeBase).size();
        }
        return count;
    }

    private List<Map<String, Object>> documents(Map<String, Object> knowledgeBase) {
        List<Map<String, Object>> result = maps(knowledgeBase.get("documents"));
        result.addAll(maps(knowledgeBase.get("docs")));
        return result;
    }

    @SuppressWarnings("unchecked")
    private List<Map<String, Object>> maps(Object value) {
        if (value instanceof List) {
            List<Map<String, Object>> result = new ArrayList<>();
            for (Object item : (List<?>) value) {
                if (item instanceof Map) {
                    result.add((Map<String, Object>) item);
                }
            }
            return result;
        }
        if (value instanceof Map) {
            return new ArrayList<>(Collections.singletonList((Map<String, Object>) value));
        }
        return new ArrayList<>();
    }

    private String nameOf(Map<String, Object> item) {
        return text(firstNonNull(item.get("name"), firstNonNull(item.get("label"), item.get("title"))));
    }

    private Map<String, Object> wrapSource(Map<String, Object> source) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("source", "dify");
        result.put("raw", source);
        return result;
    }

    private List<String> ids(List<Map<String, Object>> items) {
        List<String> result = new ArrayList<>();
        for (Map<String, Object> item : items) {
            Object id = item.get("id");
            if (id != null) {
                result.add(String.valueOf(id));
            }
        }
        return result;
    }

    private Map<String, Object> mapAgent(Agent agent) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", agent.getId());
        result.put("name", agent.getName());
        result.put("model", agent.getModel());
        result.put("functionIds", readJson(agent.getFunctionIds()));
        return result;
    }

    private Map<String, Object> mapFunction(FunctionDefinition function) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", function.getId());
        result.put("name", function.getName());
        result.put("endpoint", function.getEndpoint());
        result.put("method", function.getMethod());
        result.put("implementation", function.getImplementation());
        result.put("timeoutMs", function.getTimeoutMs());
        result.put("retryPolicy", function.getRetryPolicy());
        result.put("circuitBreakerPolicy", function.getCircuitBreakerPolicy());
        result.put("fallbackResponse", function.getFallbackResponse());
        return result;
    }

    private Map<String, Object> mapWorkflow(WorkflowDefinitionEntity workflow) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", workflow.getId());
        result.put("name", workflow.getName());
        result.put("status", workflow.getStatus());
        return result;
    }

    private Map<String, Object> mapKnowledgeBase(KnowledgeBaseEntity knowledgeBase) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", knowledgeBase.getId());
        result.put("name", knowledgeBase.getName());
        result.put("embeddingProvider", knowledgeBase.getEmbeddingProvider());
        result.put("embeddingModel", knowledgeBase.getEmbeddingModel());
        return result;
    }

    private Map<String, Object> mapDocument(RagDocumentEntity document) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", document.getId());
        result.put("knowledgeBaseId", document.getKnowledgeBaseId());
        result.put("title", document.getTitle());
        result.put("sourceUri", document.getSourceUri());
        return result;
    }

    private Object firstNonNull(Object value, Object fallback) {
        return value != null ? value : fallback;
    }

    private String text(Object value) {
        return value != null ? String.valueOf(value) : null;
    }

    private boolean blank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private Integer intValue(Object value, Integer fallback) {
        if (value instanceof Number) return ((Number) value).intValue();
        if (value instanceof String && !((String) value).isEmpty()) return Integer.parseInt((String) value);
        return fallback;
    }

    private Double doubleValue(Object value, Double fallback) {
        if (value instanceof Number) return ((Number) value).doubleValue();
        if (value instanceof String && !((String) value).isEmpty()) return Double.parseDouble((String) value);
        return fallback;
    }

    private String json(Object value) {
        try {
            Object payload = value;
            if (payload == null) {
                payload = new LinkedHashMap<String, Object>();
            }
            if (payload instanceof Collection) {
                payload = new ArrayList<>((Collection<?>) payload);
            }
            return objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException ex) {
            throw new IllegalArgumentException("Invalid Dify migration payload", ex);
        }
    }

    private Object readJson(String value) {
        if (value == null || value.trim().isEmpty()) {
            return Collections.emptyList();
        }
        try {
            return objectMapper.readValue(value, Object.class);
        } catch (Exception ex) {
            return value;
        }
    }

    private Map<String, Object> mapResult(DifyMigrationResultEntity entity) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("id", entity.getId());
        result.put("sourceName", entity.getSourceName());
        result.put("status", entity.getStatus());
        result.put("targetType", entity.getTargetType());
        result.put("sourceType", entity.getSourceType());
        result.put("errorMessage", entity.getErrorMessage());
        result.put("createdAt", entity.getCreatedAt());
        try {
            result.put("mappingDetail", objectMapper.readValue(entity.getMappingDetail(), Object.class));
        } catch (Exception ex) {
            result.put("mappingDetail", entity.getMappingDetail());
        }
        return result;
    }
}
