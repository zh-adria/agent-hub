package com.agenthub.client.auth;

public final class AuthContext {
    private static final ThreadLocal<AuthenticatedPrincipal> PRINCIPAL = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(AuthenticatedPrincipal principal) {
        PRINCIPAL.set(principal);
    }

    public static AuthenticatedPrincipal principal() {
        return PRINCIPAL.get();
    }

    public static void clear() {
        PRINCIPAL.remove();
    }
}
