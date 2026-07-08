package com.agenthub.client.impl;

import com.agenthub.client.api.LLMClient;
import org.springframework.stereotype.Service;

@Service
public class LLMClientImpl implements LLMClient {
    @Override
    public String generate(String prompt, String model) {
        return "Mock response for: " + prompt;
    }
    
    @Override
    public String generateChat(String messagesJson, String model) {
        return "Mock chat response";
    }
    
    @Override
    public boolean validateModel(String model) {
        return model != null && !model.isEmpty();
    }
}
