package com.agenthub.domain.service;

public interface EmbeddingService {
    float[] embed(String text);
    String provider();
    String model();
}
