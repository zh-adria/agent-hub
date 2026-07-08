package com.agenthub.client.api;

public interface LLMClient {
    String generate(String prompt, String model);
    String generateChat(String messagesJson, String model);
    boolean validateModel(String model);
}
