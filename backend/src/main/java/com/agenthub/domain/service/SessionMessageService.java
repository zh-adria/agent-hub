package com.agenthub.domain.service;

import com.agenthub.domain.model.Agent;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.model.Session;
import com.agenthub.domain.repository.AgentRepository;
import com.agenthub.domain.repository.SessionRepository;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
public class SessionMessageService {
    private final SessionRepository sessionRepository;
    private final AgentRepository agentRepository;
    private final ReActEngine reActEngine;
    private final TraceService traceService;

    public SessionMessageService(
            SessionRepository sessionRepository,
            AgentRepository agentRepository,
            ReActEngine reActEngine,
            TraceService traceService) {
        this.sessionRepository = sessionRepository;
        this.agentRepository = agentRepository;
        this.reActEngine = reActEngine;
        this.traceService = traceService;
    }

    public Message send(String sessionId, Message userMessage) {
        Session session = sessionRepository.findById(sessionId)
                .orElseThrow(() -> new IllegalArgumentException("Session not found: " + sessionId));
        List<Message> existing = session.getMessages() != null ? new ArrayList<>(session.getMessages()) : new ArrayList<>();
        session.setMessages(existing);
        TraceEntity trace = traceService.start("session:" + sessionId, sessionId, null, null);
        StepRecordEntity step = traceService.startStep(trace.getId(), sessionId, null, "react-loop", session.getAgentId(), userMessage.getContent());

        List<Message> generated;
        try {
            Agent agent = agentRepository.findById(session.getAgentId())
                    .orElseThrow(() -> new IllegalArgumentException("Agent not found: " + session.getAgentId()));
            generated = reActEngine.executeReActLoop(agent, session, userMessage);
            traceService.completeStep(step, lastContent(generated));
            traceService.finish(trace.getId(), "SUCCEEDED");
        } catch (Exception ex) {
            generated = new ArrayList<>();
            generated.add(userMessage);
            Message failure = new Message();
            failure.setId(UUID.randomUUID().toString());
            failure.setSessionId(sessionId);
            failure.setRole(Message.MessageRole.ASSISTANT);
            failure.setContent("Agent execution failed: " + ex.getMessage());
            failure.setCreatedAt(LocalDateTime.now());
            generated.add(failure);
            traceService.failStep(step, ex.getMessage());
            traceService.finish(trace.getId(), "FAILED");
        }
        existing.addAll(generated);
        session.setMessages(existing);
        Session saved = sessionRepository.save(session);
        List<Message> messages = saved.getMessages();
        return messages != null && !messages.isEmpty() ? messages.get(messages.size() - 1) : userMessage;
    }

    public Message newUserMessage(String sessionId, String content) {
        Message message = new Message();
        message.setId(UUID.randomUUID().toString());
        message.setSessionId(sessionId);
        message.setRole(Message.MessageRole.USER);
        message.setContent(content != null ? content : "");
        message.setCreatedAt(LocalDateTime.now());
        return message;
    }

    private String lastContent(List<Message> messages) {
        if (messages == null || messages.isEmpty()) {
            return "";
        }
        return messages.get(messages.size() - 1).getContent();
    }
}
