package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.repository.FunctionRepository;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class FunctionRegistryServiceImpl implements FunctionRegistryService {
    
    private final FunctionRepository functionRepository;
    
    public FunctionRegistryServiceImpl(FunctionRepository functionRepository) {
        this.functionRepository = functionRepository;
    }
    
    @Override
    public FunctionDefinition registerFunction(FunctionDefinition function) {
        // 权限校验
        validateFunctionPermission(function);
        
        // 检查函数是否已存在
        if (functionRepository.existsById(function.getId())) {
            throw new IllegalArgumentException("Function already exists: " + function.getId());
        }
        
        return functionRepository.save(function);
    }
    
    @Override
    public Optional<FunctionDefinition> getFunction(String functionId) {
        return functionRepository.findById(functionId);
    }
    
    @Override
    public List<FunctionDefinition> getAllFunctions() {
        return functionRepository.findAll();
    }
    
    @Override
    public FunctionDefinition updateFunction(String functionId, FunctionDefinition updatedFunction) {
        FunctionDefinition existing = functionRepository.findById(functionId)
            .orElseThrow(() -> new IllegalArgumentException("Function not found: " + functionId));
        
        // 更新字段
        existing.setName(updatedFunction.getName());
        existing.setDescription(updatedFunction.getDescription());
        existing.setParameters(updatedFunction.getParameters());
        existing.setImplementation(updatedFunction.getImplementation());
        
        return functionRepository.save(existing);
    }
    
    @Override
    public void deleteFunction(String functionId) {
        if (!functionRepository.existsById(functionId)) {
            throw new IllegalArgumentException("Function not found: " + functionId);
        }
        functionRepository.deleteById(functionId);
    }
    
    @Override
    public List<FunctionDefinition> discoverFunctions(String keyword) {
        return functionRepository.findByNameContainingOrDescriptionContaining(keyword, keyword);
    }
    
    private void validateFunctionPermission(FunctionDefinition function) {
        // 权限校验逻辑
        if (function.getOwnerId() == null || function.getOwnerId().isEmpty()) {
            throw new IllegalArgumentException("Function owner ID is required");
        }
    }
}
