package com.agenthub.client.impl;

import com.agenthub.domain.model.FunctionDefinition;
import com.agenthub.domain.port.FunctionRegistry;
import com.agenthub.domain.service.FunctionRegistryService;
import com.agenthub.domain.service.TraceService;
import com.agenthub.infra.persistence.entity.StepRecordEntity;
import com.agenthub.infra.persistence.entity.TraceEntity;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class FunctionApiImplTest {

    @Test
    void invokeFunctionRecordsSuccessfulTraceStep() {
        FunctionRegistryService functionRegistryService = mock(FunctionRegistryService.class);
        FunctionRegistry functionRegistry = mock(FunctionRegistry.class);
        TraceService traceService = mock(TraceService.class);
        FunctionApiImpl api = new FunctionApiImpl(functionRegistryService, functionRegistry, new ObjectMapper(), traceService);
        FunctionDefinition function = function("10", "lookup");
        TraceEntity trace = trace("trace-1");
        StepRecordEntity step = new StepRecordEntity();
        Map<String, Object> output = Collections.singletonMap("answer", "42");
        when(functionRegistryService.getFunction("10")).thenReturn(Optional.of(function));
        when(traceService.start(eq("function.invoke"), eq(null), eq(null), anyString())).thenReturn(trace);
        when(traceService.startStep(eq("trace-1"), eq(null), eq(null), eq("function:10"), eq(null), anyString())).thenReturn(step);
        when(functionRegistry.invoke(eq("10"), any(Map.class))).thenReturn(output);

        Object result = api.invokeFunction(10L, Collections.singletonMap("input", Collections.singletonMap("q", "life")));

        assertThat(result).isInstanceOf(Map.class);
        verify(traceService).completeStep(eq(step), anyString());
        verify(traceService).finish("trace-1", "SUCCEEDED");
    }

    @Test
    void invokeFunctionRecordsFailedTraceStep() {
        FunctionRegistryService functionRegistryService = mock(FunctionRegistryService.class);
        FunctionRegistry functionRegistry = mock(FunctionRegistry.class);
        TraceService traceService = mock(TraceService.class);
        FunctionApiImpl api = new FunctionApiImpl(functionRegistryService, functionRegistry, new ObjectMapper(), traceService);
        FunctionDefinition function = function("10", "lookup");
        TraceEntity trace = trace("trace-1");
        StepRecordEntity step = new StepRecordEntity();
        when(functionRegistryService.getFunction("10")).thenReturn(Optional.of(function));
        when(traceService.start(eq("function.invoke"), eq(null), eq(null), anyString())).thenReturn(trace);
        when(traceService.startStep(eq("trace-1"), eq(null), eq(null), eq("function:10"), eq(null), anyString())).thenReturn(step);
        when(functionRegistry.invoke(eq("10"), any(Map.class))).thenThrow(new IllegalStateException("tool down"));

        assertThatThrownBy(() -> api.invokeFunction(10L, Collections.singletonMap("q", "life")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("tool down");
        verify(traceService).failStep(eq(step), eq("IllegalStateException: tool down"));
        verify(traceService).finish("trace-1", "FAILED");
    }

    private FunctionDefinition function(String id, String name) {
        FunctionDefinition function = new FunctionDefinition();
        function.setId(id);
        function.setName(name);
        function.setEndpoint("http://example.test/tool");
        function.setMethod("POST");
        return function;
    }

    private TraceEntity trace(String id) {
        TraceEntity trace = new TraceEntity();
        trace.setId(id);
        return trace;
    }
}
