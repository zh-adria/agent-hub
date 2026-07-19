package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Spring AI MCP adapter bridge.
 *
 * <p>When Spring AI MCP dependency is added, this component will import MCP tools
 * and convert them into AgentHub {@link FunctionDefinition} records.</p>
 *
 * <p>Current implementation is a placeholder that logs when Spring AI MCP is not on the classpath.</p>
 */
@Component
public class SpringAiMcpAdapter {

    private static final org.slf4j.Logger log = org.slf4j.LoggerFactory.getLogger(SpringAiMcpAdapter.class);

    /**
     * Import MCP tools from a remote MCP server endpoint.
     *
     * @param serverUrl  MCP server URL (e.g. https://mcp.example.com/sse)
     * @param tenantId   tenant scope
     * @param ownerId    owner user
     * @return list of imported FunctionDefinitions
     */
    public List<FunctionDefinition> importMcpTools(String serverUrl, String tenantId, String ownerId) {
        log.warn("Spring AI MCP not yet on classpath. MCP import from {} skipped. Add spring-ai-mcp dependency to enable.", serverUrl);
        return java.util.Collections.emptyList();
    }

    /**
     * Check if Spring AI MCP is available on the classpath.
     */
    public boolean isAvailable() {
        try {
            Class.forName("org.springframework.ai.mcp.McpClient");
            return true;
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    /**
     * Convert an MCP tool schema to AgentHub FunctionDefinition fields.
     *
     * <p>This method performs the schema mapping logic independent of the actual MCP client,
     * so it can be tested without Spring AI on the classpath.</p>
     */
    public FunctionDefinition mapMcpToolToFunction(Map<String, Object> mcpTool, String ownerId) {
        FunctionDefinition function = new FunctionDefinition();
        function.setName((String) mcpTool.getOrDefault("name", "unknown"));
        function.setDescription((String) mcpTool.getOrDefault("description", ""));
        function.setImplementation("mcp");
        function.setOwnerId(ownerId);

        Object inputSchema = mcpTool.get("inputSchema");
        if (inputSchema instanceof Map) {
            try {
                com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
                function.setParameters(mapper.writeValueAsString(inputSchema));
            } catch (Exception ex) {
                function.setParameters("{\"type\":\"object\"}");
            }
        } else {
            function.setParameters("{\"type\":\"object\"}");
        }

        if (mcpTool.containsKey("timeoutMs")) {
            Object timeout = mcpTool.get("timeoutMs");
            if (timeout instanceof Number) {
                function.setTimeoutMs(((Number) timeout).intValue());
            }
        }

        return function;
    }
}
