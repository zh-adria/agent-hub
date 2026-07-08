package com.agenthub.client.api;

import com.agenthub.domain.model.Session;
import com.agenthub.domain.model.Message;
import com.agenthub.domain.service.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/sessions")
public class SessionApiImpl implements SessionApi {
    
    @Autowired
    private SessionService sessionService;
    
    @Override
    public Session createSession(@RequestBody Session session) {
        return sessionService.createSession(session);
    }
    
    @Override
    public Session getSession(@PathVariable String id) {
        return sessionService.getSession(id);
    }
    
    @Override
    public List<Session> getAllSessions() {
        return sessionService.getAllSessions();
    }
    
    @Override
    public void deleteSession(@PathVariable String id) {
        sessionService.deleteSession(id);
    }
    
    @Override
    public Message sendMessage(@PathVariable String id, @RequestBody Message message) {
        return sessionService.sendMessage(id, message);
    }
    
    @Override
    public List<Message> getMessages(@PathVariable String id) {
        return sessionService.getMessages(id);
    }
}
