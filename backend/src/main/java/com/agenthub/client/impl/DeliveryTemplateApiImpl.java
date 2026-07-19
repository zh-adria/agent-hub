package com.agenthub.client.impl;

import com.agenthub.domain.context.TenantContext;
import com.agenthub.infra.persistence.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.io.ByteArrayOutputStream;

@RestController
@RequestMapping("/api/delivery/templates")
public class DeliveryTemplateApiImpl {
    private final AgentJpaRepository agentRepository;
    private final FunctionDefinitionJpaRepository functionRepository;
    private final KnowledgeBaseJpaRepository knowledgeBaseRepository;
    private final WorkflowDefinitionJpaRepository workflowRepository;
    private final TraceJpaRepository traceRepository;
    private final StepRecordJpaRepository stepRecordRepository;
    private final BotBindingJpaRepository botBindingRepository;
    private final ObjectMapper objectMapper;

    public DeliveryTemplateApiImpl(
            AgentJpaRepository agentRepository,
            FunctionDefinitionJpaRepository functionRepository,
            KnowledgeBaseJpaRepository knowledgeBaseRepository,
            WorkflowDefinitionJpaRepository workflowRepository,
            TraceJpaRepository traceRepository,
            StepRecordJpaRepository stepRecordRepository,
            BotBindingJpaRepository botBindingRepository,
            ObjectMapper objectMapper) {
        this.agentRepository = agentRepository;
        this.functionRepository = functionRepository;
        this.knowledgeBaseRepository = knowledgeBaseRepository;
        this.workflowRepository = workflowRepository;
        this.traceRepository = traceRepository;
        this.stepRecordRepository = stepRecordRepository;
        this.botBindingRepository = botBindingRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public ResponseEntity<byte[]> getTemplate(
            @RequestParam(defaultValue = "standard") String edition,
            @RequestParam(defaultValue = "json") String format) {

        Map<String, Object> template = buildTemplate(edition);
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "-");
        String fileName = "agenthub-delivery-" + edition + "-" + timestamp;

        try {
            if ("zip".equalsIgnoreCase(format)) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                try (java.util.zip.ZipOutputStream zos = new java.util.zip.ZipOutputStream(baos, java.nio.charset.StandardCharsets.UTF_8)) {
                    addZipEntry(zos, "delivery-plan.json", template);
                    addZipEntry(zos, "deployment-checklist.json", buildDeploymentChecklist(edition));
                    addZipEntry(zos, "acceptance-criteria.json", buildAcceptanceCriteria(edition));
                    addZipEntry(zos, "operations-runbook.json", buildOperationsRunbook(edition));
                }
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".zip\"");
                headers.setContentType(MediaType.valueOf("application/zip"));
                return new ResponseEntity<>(baos.toByteArray(), headers, HttpStatus.OK);
            }

            String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(template);
            HttpHeaders headers = new HttpHeaders();
            headers.set(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + fileName + ".json\"");
            headers.setContentType(MediaType.APPLICATION_JSON);
            return new ResponseEntity<>(json.getBytes(java.nio.charset.StandardCharsets.UTF_8), headers, HttpStatus.OK);
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to generate delivery template", ex);
        }
    }

    @GetMapping("/list")
    public List<Map<String, Object>> listTemplates() {
        return java.util.Arrays.asList(
                summary("light", "轻量版", "单业务线、单知识库、少量工具接入"),
                summary("standard", "标准版", "Dify 替代迁移主推场景，完整功能"),
                summary("fullstack", "全栈信创版", "金融/政务/制造等高合规场景，国产化适配")
        );
    }

    private Map<String, Object> buildTemplate(String edition) {
        Map<String, Object> template = new LinkedHashMap<>();
        template.put("edition", edition);
        template.put("generatedAt", Instant.now().toString());
        template.put("tenantId", TenantContext.externalTenantId());
        template.put("description", description(edition));
        template.put("scope", scope(edition));
        template.put("deployment", deploymentSteps(edition));
        template.put("features", features(edition));
        template.put("estimatedEffort", effort(edition));
        return template;
    }

    private Map<String, Object> buildDeploymentChecklist(String edition) {
        Map<String, Object> checklist = new LinkedHashMap<>();
        checklist.put("edition", edition);
        checklist.put("items", checklistItems(edition));
        return checklist;
    }

    private Map<String, Object> buildAcceptanceCriteria(String edition) {
        Map<String, Object> criteria = new LinkedHashMap<>();
        criteria.put("edition", edition);
        criteria.put("functional", functionalCriteria(edition));
        criteria.put("nonFunctional", nonFunctionalCriteria(edition));
        criteria.put("security", securityCriteria(edition));
        return criteria;
    }

    private Map<String, Object> buildOperationsRunbook(String edition) {
        Map<String, Object> runbook = new LinkedHashMap<>();
        runbook.put("edition", edition);
        runbook.put("startStop", startStopSteps(edition));
        runbook.put("monitoring", monitoringSteps(edition));
        runbook.put("backup", backupSteps(edition));
        runbook.put("troubleshooting", java.util.Arrays.asList(
                "Backend 启动失败：检查 JDK 21 路径和 MySQL 连接",
                "前端白屏：检查 VITE_BACKEND_URL 环境变量",
                "LLM 调用超时：检查 Token Router 网关地址和网络",
                "向量检索无结果：检查 Milvus 连接和文档分块状态"
        ));
        return runbook;
    }

    private void addZipEntry(java.util.zip.ZipOutputStream zos, String name, Object value) throws Exception {
        zos.putNextEntry(new java.util.zip.ZipEntry(name));
        String json = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
        zos.write(json.getBytes(java.nio.charset.StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private Map<String, Object> summary(String id, String label, String desc) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("id", id);
        m.put("label", label);
        m.put("description", desc);
        return m;
    }

    private String description(String edition) {
        return switch (edition) {
            case "light" -> "适合单业务线、单知识库、少量工具接入的轻量部署。";
            case "fullstack" -> "适合金融、政务、制造等高合规场景，包含国产化适配和更严格的租户隔离策略。";
            default -> "适合 Dify 替代迁移主推场景，完整功能覆盖。";
        };
    }

    private List<String> scope(String edition) {
        List<String> s = new ArrayList<>();
        s.add("Agent / Function / Session 主流程部署");
        if (!"light".equals(edition)) {
            s.add("MCP tool 生态与 Function Registry");
        }
        if (!"light".equals(edition)) {
            s.add("RAG 知识库与 Hybrid search");
        }
        if (!"light".equals(edition)) {
            s.add("Multi-Agent Workflow DAG 执行");
        }
        s.add("基础 Trace 与 LLM usage audit");
        s.add("租户隔离与 RBAC");
        if ("fullstack".equals(edition)) {
            s.add("国产化 OS / 数据库 / NPU 适配评估");
            s.add("更严格的租户隔离策略");
            s.add("运维、日志、监控、告警对接");
            s.add("安全基线与验收材料");
        }
        if ("standard".equals(edition)) {
            s.add("Dify Agent / Workflow / Tool / Knowledge 迁移");
            s.add("企业 IAM / RBAC 对接契约");
            s.add("Milvus / Redis / MySQL 私有化部署");
        }
        return s;
    }

    private List<Map<String, Object>> deploymentSteps(String edition) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step(1, "环境检查", "确认 JDK 21、MySQL 8.0、Redis 7 已安装"));
        steps.add(step(2, "配置", "编辑 application-prod.yml，填入数据库密码、LLM 网关地址、Milvus 地址"));
        steps.add(step(3, "数据库初始化", "执行 schema 初始化脚本"));
        if (!"light".equals(edition)) {
            steps.add(step(4, "IAM 对接", "配置外部认证或启用 Mock 登录"));
        }
        steps.add(step(5, "启动服务", "java -jar agent-hub-backend.jar --spring.profiles.active=prod"));
        steps.add(step(6, "前端部署", "npm run build && 部署 dist/ 到 Nginx"));
        if ("fullstack".equals(edition)) {
            steps.add(step(7, "国产化适配", "验证国产 OS、数据库、NPU 驱动兼容性"));
        }
        steps.add(step(8, "健康检查", "访问 /health/ready 确认所有依赖就绪"));
        return steps;
    }

    private List<Map<String, Object>> features(String edition) {
        List<Map<String, Object>> features = new ArrayList<>();
        features.add(feature("Agent Runtime", "Agent 生命周期管理、ReAct 执行、WebSocket 流式会话"));
        features.add(feature("Tool / MCP", "Function Registry、MCP tool 导入、工具调用编排"));
        if (!"light".equals(edition)) {
            features.add(feature("Knowledge / RAG", "知识库管理、Hybrid search、Rerank"));
            features.add(feature("Workflow", "Multi-Agent DAG 定义与执行"));
        }
        features.add(feature("AgentOps", "Trace、StepRecord、LLM usage audit、Observability"));
        features.add(feature("Enterprise Channels", "Bot 绑定、飞书/企微/webhook adapter"));
        if ("fullstack".equals(edition)) {
            features.add(feature("信创合规", "国产化适配、安全基线、密钥管理"));
        }
        return features;
    }

    private Map<String, Object> effort(String edition) {
        Map<String, Object> effort = new LinkedHashMap<>();
        switch (edition) {
            case "light" -> {
                effort.put("deployment", "1 天");
                effort.put("configuration", "半天");
                effort.put("acceptance", "1 天");
            }
            case "fullstack" -> {
                effort.put("deployment", "3-5 天");
                effort.put("configuration", "2-3 天");
                effort.put("acceptance", "3-5 天");
            }
            default -> {
                effort.put("deployment", "2-3 天");
                effort.put("configuration", "1-2 天");
                effort.put("acceptance", "2-3 天");
            }
        }
        return effort;
    }

    private List<Map<String, Object>> checklistItems(String edition) {
        List<Map<String, Object>> items = new ArrayList<>();
        items.add(checkItem("INFRA-01", "JDK 21 已安装", "基础", true));
        items.add(checkItem("INFRA-02", "MySQL 8.0 已安装并可连接", "基础", true));
        items.add(checkItem("INFRA-03", "Redis 7 已安装并可连接", "基础", !edition.equals("light")));
        items.add(checkItem("INFRA-04", "Milvus 或本地向量存储可用", "基础", !edition.equals("light")));
        items.add(checkItem("INFRA-05", "LLM 网关地址配置正确", "基础", true));
        items.add(checkItem("CONF-01", "application-prod.yml 已配置", "配置", true));
        items.add(checkItem("CONF-02", "环境变量已设置", "配置", "fullstack".equals(edition)));
        items.add(checkItem("CONF-03", "IAM/Mock 登录已测试", "配置", !edition.equals("light")));
        items.add(checkItem("DATA-01", "数据库 schema 已初始化", "数据", true));
        items.add(checkItem("DATA-02", "演示数据已生成", "数据", true));
        items.add(checkItem("FUNC-01", "Agent 可创建和对话", "功能", true));
        items.add(checkItem("FUNC-02", "Function 可注册和调用", "功能", true));
        items.add(checkItem("FUNC-03", "RAG 知识库可检索", "功能", !edition.equals("light")));
        items.add(checkItem("FUNC-04", "Workflow 可执行", "功能", !edition.equals("light")));
        items.add(checkItem("FUNC-05", "Trace 链路可查询", "功能", true));
        items.add(checkItem("SEC-01", "RBAC 权限测试通过", "安全", true));
        items.add(checkItem("SEC-02", "跨租户隔离测试通过", "安全", true));
        items.add(checkItem("SEC-03", "安全基线检查通过", "安全", "fullstack".equals(edition)));
        return items;
    }

    private List<String> functionalCriteria(String edition) {
        List<String> criteria = new ArrayList<>();
        criteria.add("Agent 可创建、编辑、部署，对话返回正常响应");
        criteria.add("Function 可注册、发现、调用，参数校验生效");
        criteria.add("Session 可创建，消息历史可查询");
        criteria.add("Trace 和 StepRecord 记录完整");
        if (!edition.equals("light")) {
            criteria.add("RAG 检索结果准确，Hybrid search 生效");
            criteria.add("Workflow DAG 执行成功，节点状态正确");
        }
        if ("standard".equals(edition)) {
            criteria.add("Dify 导出物可成功导入并映射");
        }
        return criteria;
    }

    private List<String> nonFunctionalCriteria(String edition) {
        List<String> criteria = new ArrayList<>();
        criteria.add("核心接口 p99 < 2s");
        criteria.add("系统可用性 99.9%");
        criteria.add("支持 100+ 并发 Agent 会话");
        if (!edition.equals("light")) {
            criteria.add("Milvus 检索 p99 < 500ms");
            criteria.add("Workflow 执行超时控制生效");
        }
        return criteria;
    }

    private List<String> securityCriteria(String edition) {
        List<String> criteria = new ArrayList<>();
        criteria.add("未认证请求返回 401");
        criteria.add("跨租户资源访问返回 404");
        criteria.add("敏感操作需要对应权限");
        criteria.add("LLM 调用记录写入 audit");
        if ("fullstack".equals(edition)) {
            criteria.add("安全基线检查全部通过");
            criteria.add("密钥通过环境变量注入");
            criteria.add("国产化组件兼容性验证通过");
        }
        return criteria;
    }

    private List<Map<String, Object>> startStopSteps(String edition) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(step("START-1", "启动 Redis", "redis-server"));
        steps.add(step("START-2", "启动 MySQL", "mysqld"));
        if (!edition.equals("light")) {
            steps.add(step("START-3", "启动 Milvus", "milvus run standalone"));
        }
        steps.add(step("START-4", "启动后端", "java -jar agent-hub-backend.jar --spring.profiles.active=prod"));
        steps.add(step("START-5", "启动前端", "npm run preview 或部署到 Nginx"));
        steps.add(step("STOP-1", "停止前端 Nginx", "nginx -s stop"));
        steps.add(step("STOP-2", "停止后端", "kill %pid%"));
        steps.add(step("STOP-3", "停止 Milvus", "milvus stop"));
        return steps;
    }

    private List<Map<String, Object>> monitoringSteps(String edition) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(monitor("健康检查", "GET /health/ready", "200 OK"));
        steps.add(monitor("Trace 查询", "GET /api/traces", "返回最近执行记录"));
        steps.add(monitor("LLM 用量", "GET /api/audit/llm-usage/summary", "返回 token/cost 汇总"));
        steps.add(monitor("告警指标", "GET /api/observability/alerts", "返回失败率、成本等指标"));
        if ("fullstack".equals(edition)) {
            steps.add(monitor("日志采集", "配置日志输出到文件或外部 ELK", "日志结构化输出"));
            steps.add(monitor("性能监控", "对接 Prometheus + Grafana", "JVM、数据库、HTTP 指标"));
        }
        return steps;
    }

    private List<Map<String, Object>> backupSteps(String edition) {
        List<Map<String, Object>> steps = new ArrayList<>();
        steps.add(backup("MySQL 数据", "mysqldump --all-databases > backup.sql", "每日"));
        steps.add(backup("配置文件", "备份 application-prod.yml 和 env vars", "变更时"));
        if (!edition.equals("light")) {
            steps.add(backup("Milvus 数据", "milvus export collection", "每周"));
        }
        return steps;
    }

    private Map<String, Object> step(int num, String name, String desc) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("step", num);
        s.put("name", name);
        s.put("description", desc);
        return s;
    }

    private Map<String, Object> step(String id, String name, String desc) {
        Map<String, Object> s = new LinkedHashMap<>();
        s.put("id", id);
        s.put("name", name);
        s.put("command", desc);
        return s;
    }

    private Map<String, Object> feature(String name, String desc) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("name", name);
        f.put("description", desc);
        return f;
    }

    private Map<String, Object> checkItem(String id, String name, String category, boolean required) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("id", id);
        item.put("name", name);
        item.put("category", category);
        item.put("required", required);
        return item;
    }

    private Map<String, Object> monitor(String name, String endpoint, String expectation) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("name", name);
        m.put("endpoint", endpoint);
        m.put("expectation", expectation);
        return m;
    }

    private Map<String, Object> backup(String type, String command, String frequency) {
        Map<String, Object> b = new LinkedHashMap<>();
        b.put("type", type);
        b.put("command", command);
        b.put("frequency", frequency);
        return b;
    }
}
