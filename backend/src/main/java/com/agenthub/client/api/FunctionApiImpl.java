package com.agenthub.client.api;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.service.FunctionRegistryService;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
public class FunctionApiImpl implements FunctionApi {
    private final FunctionRegistryService functionRegistryService;
    
    public FunctionApiImpl(FunctionRegistryService functionRegistryService) {
        this.functionRegistryService = functionRegistryService;
    }
    
    @Override
    @PostMapping
    public FunctionDefinition createFunction(@RequestBody FunctionDefinition function) {
        return functionRegistryService.registerFunction(function);
    }
    
    @Override
    @GetMapping
    public List<FunctionDefinition> listFunctions() {
        return functionRegistryService.discoverFunctions();
    }
    
    @Override
    @GetMapping("/{id}")
    public FunctionDefinition getFunction(@PathVariable String id) {
        return functionRegistryService.getFunction(id);
    }
    
    @Override
    @PutMapping("/{id}")
    public FunctionDefinition updateFunction(@PathVariable String id, @RequestBody FunctionDefinition function) {
        return functionRegistryService.updateFunction(id, function);
    }
    
    @Override
    @DeleteMapping("/{id}")
    public void deleteFunction(@PathVariable String id) {
        functionRegistryService.deleteFunction(id);
    }
    
    @Override
    @PostMapping("/{id}/invoke")
    public Object invokeFunction(@PathVariable String id, @RequestBody String arguments) {
        return functionRegistryService.invokeFunction(id, arguments);
    }
}
