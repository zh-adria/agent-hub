package com.agenthub.domain.repository;

import com.agenthub.domain.model.FunctionDefinition;

import java.util.List;
import java.util.Optional;

public interface FunctionRepository {
    boolean existsById(String id);

    FunctionDefinition save(FunctionDefinition function);

    Optional<FunctionDefinition> findById(String id);

    List<FunctionDefinition> findAll();

    void deleteById(String id);

    List<FunctionDefinition> findByNameContainingOrDescriptionContaining(String nameKeyword, String descriptionKeyword);
}
