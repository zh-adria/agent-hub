package com.agenthub.client.tokenrouter;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.function.Consumer;

@Service
public class HttpTokenRouterClient implements TokenRouterClient {
    private final RestTemplate restTemplate;
    private final TokenRouterProperties properties;
    private final TokenRouterResponseMapper responseMapper;
    private final ObjectMapper objectMapper;

    @Autowired
    public HttpTokenRouterClient(TokenRouterProperties properties, ObjectMapper objectMapper) {
        this(new RestTemplate(), properties, new TokenRouterResponseMapper(objectMapper), objectMapper);
    }

    public HttpTokenRouterClient(
            RestTemplate restTemplate,
            TokenRouterProperties properties,
            TokenRouterResponseMapper responseMapper,
            ObjectMapper objectMapper
    ) {
        this.restTemplate = restTemplate;
        this.properties = properties;
        this.responseMapper = responseMapper;
        this.objectMapper = objectMapper;
    }

    @Override
    public TokenRouterChatResponse complete(TokenRouterChatRequest request) {
        request.setStream(false);
        try {
            ResponseEntity<Map<String, Object>> response = restTemplate.postForEntity(
                    properties.completionUrl(),
                    request,
                    mapType()
            );
            Map<String, Object> body = response.getBody();
            if (body == null) {
                throw new TokenRouterClientException("Token Router returned empty response");
            }
            return responseMapper.fromMap(body);
        } catch (HttpStatusCodeException ex) {
            throw mapHttpException(ex);
        }
    }

    @Override
    public TokenRouterChatResponse streamComplete(TokenRouterChatRequest request, Consumer<String> chunkHandler) {
        request.setStream(true);
        try {
            return restTemplate.execute(
                    properties.streamCompletionUrl(),
                    org.springframework.http.HttpMethod.POST,
                    callback -> {
                        callback.getHeaders().setContentType(org.springframework.http.MediaType.APPLICATION_JSON);
                        objectMapper.writeValue(callback.getBody(), request);
                    },
                    response -> {
                        if (response.getStatusCode().isError()) {
                            HttpStatus status = response.getStatusCode();
                            String body = StreamUtils.copyToString(response.getBody(), StandardCharsets.UTF_8);
                            throw mapStatus(status, body);
                        }
                        TokenRouterChatResponse finalResponse = new TokenRouterChatResponse();
                        try (BufferedReader reader = new BufferedReader(new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (StringUtils.hasText(line)) {
                                    chunkHandler.accept(line);
                                }
                            }
                        }
                        return finalResponse;
                    }
            );
        } catch (HttpStatusCodeException ex) {
            throw mapHttpException(ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Class<Map<String, Object>> mapType() {
        return (Class<Map<String, Object>>) (Class<?>) Map.class;
    }

    private TokenRouterClientException mapHttpException(HttpStatusCodeException ex) {
        return mapStatus(ex.getStatusCode(), ex.getResponseBodyAsString());
    }

    private TokenRouterClientException mapStatus(HttpStatus status, String body) {
        String message = StringUtils.hasText(body) ? body : status.toString();
        if (status == HttpStatus.FORBIDDEN || status == HttpStatus.TOO_MANY_REQUESTS) {
            return new TokenRouterPolicyDeniedException(message);
        }
        return new TokenRouterClientException("Token Router request failed: " + message);
    }

}
