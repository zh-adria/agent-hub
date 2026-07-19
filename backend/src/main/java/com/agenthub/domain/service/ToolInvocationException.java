package com.agenthub.domain.service;

import com.agenthub.domain.model.ToolCall;

/**
 * Exception thrown when a tool invocation fails.
 *
 * <p>Encapsulates error classification for observability and retry decisions.</p>
 */
public class ToolInvocationException extends RuntimeException {
    public enum ErrorCategory {
        TIMEOUT,           // Tool invocation exceeded timeout
        EXECUTION_ERROR,   // Tool returned an error response
        NOT_FOUND,         // Function/tool not found
        PERMISSION_DENIED, // Caller not authorized to invoke this tool
        INVALID_INPUT,     // Arguments failed validation
        INTERRUPTED,       // Thread was interrupted during invocation
        UNKNOWN            // Uncategorized error
    }

    private final ToolCall toolCall;
    private final ErrorCategory category;

    public ToolInvocationException(ToolCall toolCall, ErrorCategory category, String message) {
        super(message);
        this.toolCall = toolCall;
        this.category = category;
    }

    public ToolInvocationException(ToolCall toolCall, ErrorCategory category, Throwable cause) {
        super(cause.getMessage() != null ? cause.getMessage() : "Tool invocation failed", cause);
        this.toolCall = toolCall;
        this.category = category;
    }

    public ToolCall getToolCall() {
        return toolCall;
    }

    public ErrorCategory getCategory() {
        return category;
    }

    public String getToolName() {
        return toolCall != null ? toolCall.getName() : "unknown";
    }
}
