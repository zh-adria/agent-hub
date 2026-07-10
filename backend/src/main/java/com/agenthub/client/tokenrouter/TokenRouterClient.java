package com.agenthub.client.tokenrouter;

import java.util.function.Consumer;

public interface TokenRouterClient {
    TokenRouterChatResponse complete(TokenRouterChatRequest request);

    TokenRouterChatResponse streamComplete(TokenRouterChatRequest request, Consumer<String> chunkHandler);
}
