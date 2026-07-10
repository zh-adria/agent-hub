package com.agenthub.domain.repository;

import com.agenthub.domain.model.Session;

import java.util.List;
import java.util.Optional;

public interface SessionRepository {
    boolean existsById(String id);
    Session save(Session session);
    Optional<Session> findById(String id);
    List<Session> findAll();
    void deleteById(String id);
}
