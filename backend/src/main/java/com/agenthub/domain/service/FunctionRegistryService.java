package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;

import java.util.List;
import java.util.Optional;

public interface FunctionRegistryService {
    FunctionDefinition registerFunction(FunctionDefinition function);

    Optional<FunctionDefinition> getFunction(String functionId);

    List<FunctionDefinition> getAllFunctions();

    FunctionDefinition updateFunction(String functionId, FunctionDefinition updatedFunction);

    void deleteFunction(String functionId);

    List<FunctionDefinition> discoverFunctions(String keyword);
}
