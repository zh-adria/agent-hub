package com.agenthub.domain.service;

import com.agenthub.domain.model.FunctionDefinition;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FunctionRegistryPortAdapterTest {
    @Test
    void invokesFunctionByNameWithRequiredSchemaValidation() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tool", exchange -> {
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            FunctionDefinition function = new FunctionDefinition();
            function.setId("1");
            function.setName("lookup");
            function.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/tool");
            function.setMethod("POST");
            function.setTimeoutMs(1000);
            function.setParameters("{\"required\":[\"q\"]}");

            FunctionRegistryService service = mock(FunctionRegistryService.class);
            when(service.discoverFunctions("lookup")).thenReturn(Collections.singletonList(function));

            FunctionRegistryPortAdapter adapter = new FunctionRegistryPortAdapter(service, new ObjectMapper());
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("q", "agent");

            Object result = adapter.invoke("lookup", args);

            assertEquals(true, ((Map<?, ?>) result).get("ok"));
        } finally {
            server.stop(0);
        }
    }

    @Test
    void rejectsMissingRequiredArgumentBeforeHttpInvocation() {
        FunctionDefinition function = new FunctionDefinition();
        function.setId("1");
        function.setName("lookup");
        function.setEndpoint("http://127.0.0.1:1/tool");
        function.setMethod("POST");
        function.setParameters("{\"parameters\":[{\"name\":\"q\",\"required\":true}]}");

        FunctionRegistryService service = mock(FunctionRegistryService.class);
        when(service.getFunction("1")).thenReturn(Optional.of(function));

        FunctionRegistryPortAdapter adapter = new FunctionRegistryPortAdapter(service, new ObjectMapper());

        assertThrows(IllegalArgumentException.class, () -> adapter.invoke("1", Collections.emptyMap()));
    }

    @Test
    void acceptsJsonStringEncodedParameterSchema() throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/tool", exchange -> {
            byte[] response = "{\"ok\":true}".getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, response.length);
            exchange.getResponseBody().write(response);
            exchange.close();
        });
        server.start();
        try {
            FunctionDefinition function = new FunctionDefinition();
            function.setId("2");
            function.setName("encoded");
            function.setEndpoint("http://127.0.0.1:" + server.getAddress().getPort() + "/tool");
            function.setMethod("GET");
            function.setTimeoutMs(1000);
            function.setParameters("\"{\\\"type\\\":\\\"object\\\"}\"");

            FunctionRegistryService service = mock(FunctionRegistryService.class);
            when(service.getFunction("2")).thenReturn(Optional.of(function));

            FunctionRegistryPortAdapter adapter = new FunctionRegistryPortAdapter(service, new ObjectMapper());

            Object result = adapter.invoke("2", Collections.emptyMap());

            assertEquals(true, ((Map<?, ?>) result).get("ok"));
        } finally {
            server.stop(0);
        }
    }
}
