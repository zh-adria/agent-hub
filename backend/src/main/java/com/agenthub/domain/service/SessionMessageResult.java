package com.agenthub.domain.service;

import com.agenthub.domain.model.Message;

public class SessionMessageResult {
    private final Message message;
    private final String traceId;
    private final Long stepRecordId;
    private final String status;

    public SessionMessageResult(Message message, String traceId, Long stepRecordId, String status) {
        this.message = message;
        this.traceId = traceId;
        this.stepRecordId = stepRecordId;
        this.status = status;
    }

    public Message getMessage() {
        return message;
    }

    public String getTraceId() {
        return traceId;
    }

    public Long getStepRecordId() {
        return stepRecordId;
    }

    public String getStatus() {
        return status;
    }
}
