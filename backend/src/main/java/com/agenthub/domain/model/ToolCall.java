package com.agenthub.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Structured tool call from LLM response.
 *
 * <p>Supports the AgentHub structured tool call protocol where the LLM returns
 * a JSON object containing one or more tool calls. This replaces the legacy
 * {@code FUNCTION_CALL:} text-based protocol.</p>
 *
 * <h3>Protocol formats supported:</h3>
 * <pre>
 * // Single tool call
 * {"toolCall": {"name": "lookup", "arguments": {"id": "123"}}}
 *
 * // OpenAI-style
 * {"tool_calls": [{"function": {"name": "lookup", "arguments": "{\"id\":\"123\"}"}}]}
 *
 * // Anthropic-style
 * {"tool_use": {"name": "lookup", "input": {"id": "123"}}}
 *
 * // Multiple tool calls
 * {"toolCalls": [{"name": "lookup", "arguments": {"id": "123"}}, {"name": "search", "arguments": {"q": "test"}}]}
 * </pre>
 */
public class ToolCall {
    private String name;
    private Map<String, Object> arguments;
    private String rawFormat; // Which format was detected: "toolCall", "tool_calls", "tool_use", etc.
    private int index; // For multiple tool calls, the order index

    public ToolCall() {
        this.arguments = new LinkedHashMap<>();
    }

    public ToolCall(String name, Map<String, Object> arguments) {
        this();
        this.name = name;
        if (arguments != null) {
            this.arguments.putAll(arguments);
        }
    }

    public ToolCall(String name, Map<String, Object> arguments, String rawFormat, int index) {
        this(name, arguments);
        this.rawFormat = rawFormat;
        this.index = index;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, Object> getArguments() {
        return arguments;
    }

    public void setArguments(Map<String, Object> arguments) {
        this.arguments = arguments != null ? arguments : new LinkedHashMap<>();
    }

    public String getRawFormat() {
        return rawFormat;
    }

    public void setRawFormat(String rawFormat) {
        this.rawFormat = rawFormat;
    }

    public int getIndex() {
        return index;
    }

    public void setIndex(int index) {
        this.index = index;
    }

    public boolean hasArguments() {
        return arguments != null && !arguments.isEmpty();
    }

    public Object getArgument(String key) {
        return arguments != null ? arguments.get(key) : null;
    }

    public String getArgumentAsString(String key) {
        Object value = getArgument(key);
        return value != null ? String.valueOf(value) : null;
    }
}
