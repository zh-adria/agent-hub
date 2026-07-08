package com.agenthub.client.llm;

import com.agenthub.domain.model.LLMRequest;
import com.agenthub.domain.model.LLMResponse;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * LLM 客户端抽象
 * 统一 OpenAI/Anthropic/Azure 接口
 */
@Service
public class LLMClient {

    /**
     * 发送消息到 LLM
     */
    public LLMResponse sendMessage(LLMRequest request) {
        // TODO: 实现具体 LLM 调用逻辑
        return new LLMResponse();
    }

    /**
     * 批量发送消息（用于 Function Calling 场景）
     */
    public List<LLMResponse> sendMessages(List<LLMRequest> requests) {
        // TODO: 实现批量调用
        return List.of();
    }

    /**
     * 流式发送消息
     */
    public void sendMessageStream(LLMRequest request, StreamHandler handler) {
        // TODO: 实现流式调用
    }

    @FunctionalInterface
    public interface StreamHandler {
        void onChunk(String chunk);
        void onComplete(LLMResponse response);
        void onError(Throwable error);
    }
}
