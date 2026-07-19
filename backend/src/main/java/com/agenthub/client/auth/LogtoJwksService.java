package com.agenthub.client.auth;

import com.nimbusds.jose.JOSEException;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.KeySourceException;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKMatcher;
import com.nimbusds.jose.jwk.JWKSelector;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jose.proc.BadJOSEException;
import com.nimbusds.jose.proc.SecurityContext;
import com.nimbusds.jose.proc.SimpleSecurityContext;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.proc.DefaultJWTProcessor;
import com.nimbusds.jwt.proc.JWTProcessor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.net.URL;
import java.security.Key;
import java.text.ParseException;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Logto Cloud OIDC JWKS 公钥拉取与本地 JWT 校验服务。
 *
 * <p>使用 Nimbus JOSE+JWT 的 {@link DefaultJWTProcessor} + {@link RemoteJWKSet}
 * 完成 JWT 离线校验（RS256 签名、exp/nbf、audience）。
 *
 * <h3>校验流程</h3>
 * <ol>
 *   <li>从 Logto JWKS endpoint 初始化 {@link RemoteJWKSet}（Nimbus 内置 HTTP 缓存）</li>
 *   <li>构建 {@link DefaultJWTProcessor} 并配置 JWK key selector</li>
 *   <li>{@code processor.process(token, context)} 一步完成签名 + exp/nbf 校验</li>
 *   <li>手动补充 audience 校验（Nimbus 不内置）</li>
 *   <li>返回 {@link JWTClaimsSet}</li>
 * </ol>
 *
 * <p>全部在校验方本地完成，零 Logto 网络调用。
 */
@Service
@ConditionalOnProperty(prefix = "agenthub.identity", name = "provider", havingValue = "logto")
public class LogtoJwksService {

    private static final Logger log = LoggerFactory.getLogger(LogtoJwksService.class);

    private final RestTemplate restTemplate;
    private final LogtoIdentityProperties properties;

    /** Nimbus RemoteJWKSet 实例（线程安全，内部自带缓存和刷新） */
    private volatile RemoteJWKSet<SecurityContext> remoteJwkSet;

    public LogtoJwksService(LogtoIdentityProperties properties) {
        this(new RestTemplate(), properties);
    }

    LogtoJwksService(RestTemplate restTemplate, LogtoIdentityProperties properties) {
        this.restTemplate = restTemplate;
        this.properties = properties;
    }

    // ------------------------------------------------------------------
    // public API
    // ------------------------------------------------------------------

    /**
     * 校验并解析 JWT，返回 claims set。
     *
     * @throws IllegalArgumentException 如果 token 无效（签名错误、过期、audience 不匹配等）
     */
    public JWTClaimsSet validateAndParse(String token, String expectedAudience) {
        try {
            JWTProcessor<SecurityContext> processor = getJwtProcessor();

            // process(String, context) 一步完成：签名校验 + exp/nbf 校验
            JWTClaimsSet claims = processor.process(
                    token,
                    new SimpleSecurityContext());

            // Nimbus 不内置 audience 校验，手动补充
            if (expectedAudience != null && !expectedAudience.isBlank()) {
                if (claims.getAudience() == null
                        || claims.getAudience().stream()
                        .noneMatch(expectedAudience::equalsIgnoreCase)) {
                    throw new BadJOSEException(
                            "Audience mismatch, expected: " + expectedAudience);
                }
            }

            return claims;

        } catch (BadJOSEException ex) {
            throw new IllegalArgumentException(
                    "JWT validation failed: " + ex.getMessage(), ex);
        } catch (JOSEException | ParseException ex) {
            throw new IllegalArgumentException(
                    "Invalid JWT: " + ex.getMessage(), ex);
        }
    }

    /**
     * 主动刷新 JWKS 缓存（重建 RemoteJWKSet 实例）。
     */
    public void refresh() {
        remoteJwkSet = null;
        log.info("Logto JWKS cache cleared, will refetch on next validateAndParse call");
    }

    // ------------------------------------------------------------------
    // internal
    // ------------------------------------------------------------------

    /**
     * 构建并缓存 {@link DefaultJWTProcessor}，配置 RemoteJWKSet 作为 key source。
     */
    private synchronized JWTProcessor<SecurityContext> getJwtProcessor() {
        RemoteJWKSet<SecurityContext> rjwks = remoteJwkSet;
        if (rjwks == null) {
            try {
                String jwksUri = properties.getJwksUri();
                log.debug("Initializing RemoteJWKSet for Logto: {}", jwksUri);
                remoteJwkSet = new RemoteJWKSet<>(
                        new URL(jwksUri));
                rjwks = remoteJwkSet;
            } catch (Exception ex) {
                throw new IllegalStateException(
                        "Cannot initialize Logto JWKS client at "
                                + properties.getJwksUri(), ex);
            }
        }

        final RemoteJWKSet<SecurityContext> jwkSource = rjwks;

        DefaultJWTProcessor<SecurityContext> processor = new DefaultJWTProcessor<>();
        processor.setJWSKeySelector((header, context) -> {
            try {
                @SuppressWarnings("unchecked")
                List<? extends Key> keys = (List<? extends Key>) (List<?>) jwkSource.get(
                        new JWKSelector(JWKMatcher.forJWSHeader((JWSHeader) header)),
                        context);
                return keys;
            } catch (KeySourceException ex) {
                log.warn("Failed to resolve JWK for keyId={}: {}",
                        header.getKeyID(), ex.getMessage());
                return Collections.emptyList();
            }
        });
        return processor;
    }
}
