package com.agenthub.client.tokenrouter;

public class TokenRouterClientException extends RuntimeException {
    public TokenRouterClientException(String message) {
        super(message);
    }

    public TokenRouterClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
