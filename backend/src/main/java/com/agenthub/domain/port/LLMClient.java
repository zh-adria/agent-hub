package com.agenthub.domain.port;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;

import java.util.List;
import java.util.function.Consumer;

public interface LLMClient {
    String reason(Agent agent, List<Message> context);

    String generateFinalAnswer(Agent agent, List<Message> context);

    default String streamFinalAnswer(Agent agent, List<Message> context, Consumer<String> chunkHandler) {
        String content = generateFinalAnswer(agent, context);
        if (chunkHandler != null) {
            chunkHandler.accept(content);
        }
        return content;
    }
}
