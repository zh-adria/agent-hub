package com.agenthub.domain.service;

import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Session context compression strategies.
 *
 * <p>When a session's message history grows beyond a token threshold, this service
 * compresses the context to stay within limits while preserving the most important
 * information: system instructions and recent conversation turns.</p>
 *
 * <h3>Strategies:</h3>
 * <ul>
 *   <li>{@link Strategy#SLIDING_WINDOW} — Keep last N messages, drop older ones</li>
 *   <li>{@link Strategy#TOKEN_BUDGET} — Keep messages until token budget is reached, oldest first</li>
 *   <li>{@link Strategy#SUMMARIZE} — (Future) Use LLM to summarize old messages into a system message</li>
 * </ul>
 */
@Service
public class SessionContextCompressor {

    private static final Logger log = LoggerFactory.getLogger(SessionContextCompressor.class);
    private static final int DEFAULT_MAX_MESSAGES = 20;
    private static final int DEFAULT_TOKEN_BUDGET = 4000;
    private static final int CHARS_PER_TOKEN_ESTIMATE = 4; // Rough estimate for CJK + English mixed text

    public enum Strategy {
        SLIDING_WINDOW,
        TOKEN_BUDGET,
        SUMMARIZE // Reserved for future LLM-based summarization
    }

    /**
     * Compress session context using the configured strategy.
     *
     * @param session      The session to compress
     * @param strategy     Compression strategy
     * @param maxMessages  Maximum messages to keep (for SLIDING_WINDOW)
     * @param tokenBudget  Maximum estimated tokens (for TOKEN_BUDGET)
     * @return compressed message list
     */
    public List<Message> compress(Session session, Strategy strategy, int maxMessages, int tokenBudget) {
        List<Message> messages = session.getMessages();
        if (messages == null || messages.isEmpty()) {
            return new ArrayList<>();
        }

        return switch (strategy) {
            case SLIDING_WINDOW -> slidingWindow(messages, maxMessages);
            case TOKEN_BUDGET -> tokenBudget(messages, tokenBudget);
            case SUMMARIZE -> summarize(messages, maxMessages); // Falls back to sliding window for now
        };
    }

    /**
     * Compress session context with default settings.
     */
    public List<Message> compress(Session session) {
        return compress(session, Strategy.SLIDING_WINDOW, DEFAULT_MAX_MESSAGES, DEFAULT_TOKEN_BUDGET);
    }

    /**
     * Sliding window: keep the most recent N messages.
     * Always preserves the first message if it looks like a system prompt.
     */
    List<Message> slidingWindow(List<Message> messages, int maxMessages) {
        if (messages.size() <= maxMessages) {
            return new ArrayList<>(messages);
        }

        List<Message> result = new ArrayList<>();

        // Preserve the first message if it's a system message (typically the system prompt)
        if (!messages.isEmpty() && Message.MessageRole.SYSTEM.equals(messages.getFirst().getRole())) {
            result.add(messages.getFirst());
        }

        // Take the most recent messages
        int start = Math.max(result.size(), messages.size() - maxMessages + result.size());
        for (int i = start; i < messages.size(); i++) {
            result.add(messages.get(i));
        }

        log.debug("Sliding window compression: {} -> {} messages", messages.size(), result.size());
        return result;
    }

    /**
     * Token budget: keep messages until the estimated token count is reached.
     * Most recent messages are prioritized.
     */
    List<Message> tokenBudget(List<Message> messages, int tokenBudget) {
        if (messages.isEmpty()) {
            return new ArrayList<>();
        }

        List<Message> result = new ArrayList<>();
        int estimatedTokens = 0;

        // Always include the first system message if present
        if (Message.MessageRole.SYSTEM.equals(messages.getFirst().getRole())) {
            Message systemMsg = messages.getFirst();
            result.add(systemMsg);
            estimatedTokens += estimateTokens(systemMsg);
        }

        // Walk backwards through messages, adding most recent first
        for (int i = messages.size() - 1; i >= 0; i--) {
            Message msg = messages.get(i);

            // Skip if already added (system message at index 0)
            if (result.contains(msg)) {
                continue;
            }

            int msgTokens = estimateTokens(msg);
            if (estimatedTokens + msgTokens > tokenBudget && !result.isEmpty()) {
                break;
            }

            estimatedTokens += msgTokens;
            result.add(0, msg); // Insert at beginning to maintain chronological order
        }

        log.debug("Token budget compression: {} -> {} messages, ~{} tokens",
                messages.size(), result.size(), estimatedTokens);
        return result;
    }

    /**
     * Summarize: (placeholder) falls back to sliding window.
     * Future implementation will use LLM to generate a summary of old messages.
     */
    List<Message> summarize(List<Message> messages, int maxMessages) {
        log.debug("Summarize strategy not yet implemented, falling back to sliding window");
        return slidingWindow(messages, maxMessages);
    }

    /**
     * Estimate token count for a message (rough heuristic: ~4 chars per token for mixed CJK/English).
     */
    int estimateTokens(Message message) {
        if (message == null || message.getContent() == null) {
            return 0;
        }
        return Math.max(1, message.getContent().length() / CHARS_PER_TOKEN_ESTIMATE);
    }

    /**
     * Estimate total tokens for a list of messages.
     */
    int estimateTotalTokens(List<Message> messages) {
        if (messages == null) {
            return 0;
        }
        return messages.stream().mapToInt(this::estimateTokens).sum();
    }

    /**
     * Build a compression metadata map for observability.
     */
    Map<String, Object> compressionStats(List<Message> original, List<Message> compressed) {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("originalCount", original != null ? original.size() : 0);
        stats.put("compressedCount", compressed != null ? compressed.size() : 0);
        stats.put("originalTokens", estimateTotalTokens(original));
        stats.put("compressedTokens", estimateTotalTokens(compressed));
        stats.put("compressionRatio", original != null && !original.isEmpty()
                ? (double) compressed.size() / original.size()
                : 0.0);
        return stats;
    }
}
