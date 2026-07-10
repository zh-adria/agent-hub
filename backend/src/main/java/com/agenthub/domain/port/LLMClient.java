package com.agenthub.domain.port;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;

import java.util.List;

public interface LLMClient {
    String reason(Agent agent, List<Message> context);

    String generateFinalAnswer(Agent agent, List<Message> context);
}
