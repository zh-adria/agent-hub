package com.agenthub.client.auth;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 集成配置属性。
 *
 * <p>激活方式：设置 {@code agenthub.identity.provider=sa-token}。
 *
 * <h3>Sa-Token 核心概念</h3>
 * <ul>
 *   <li>{@code tokenName} — Cookie/Header 中 token 的参数名，默认 {@code satoken}</li>
 *   <li>{@code timeout} — 登录凭证有效时间（秒），默认 2 小时</li>
 *   <li>{@code activeTimeout} — 活跃续期超时（秒），0 表示不启用</li>
 *   <li>{@code isReadCookie} / {@code isReadHeader} — 是否从 Cookie/Header 读取 token</li>
 *   <li>{@code jwtSecretKey} — JWT 模式下的签名密钥（启用 JWT 时需要）</li>
 * </ul>
 *
 * <p>配置示例：
 * <pre>
 * agenthub:
 *   identity:
 *     provider: sa-token
 *     sa-token:
 *       token-name: satoken
 *       timeout: 7200
 *       is-read-header: true
 *       is-concurrent: true
 *       jwt-secret-key: your-secret-key
 * </pre>
 */
@Component
@ConfigurationProperties(prefix = "agenthub.identity.sa-token")
public class SaTokenIdentityProperties {

    /**
     * Token 名称（Cookie/Header key），默认 {@code satoken}
     */
    private String tokenName = "satoken";

    /**
     * 登录凭证有效期（秒），默认 2 小时
     */
    private long timeout = 7200;

    /**
     * 活跃续期超时（秒），0 表示不启用
     */
    private long activeTimeout = 0;

    /**
     * 是否允许同时登录（多端），默认 true
     */
    private boolean isConcurrent = true;

    /**
     * 是否从 Cookie 读取 token
     */
    private boolean isReadCookie = false;

    /**
     * 是否从 Header 读取 token
     */
    private boolean isReadHeader = true;

    /**
     * 是否输出操作日志
     */
    private boolean isLog = false;

    /**
     * JWT 模式密钥（启用 JWT 时必需）
     * 留空则使用 Sa-Token 默认 Session 模式
     */
    private String jwtSecretKey = "";

    /**
     * 是否启用 JWT 模式（而非 Session 模式）
     */
    private boolean jwtEnabled = false;

    /**
     * 权限认证函数名称（对应 StpInterface.getPermissionList）
     * 留空使用 Sa-Token 默认实现
     */
    private String permissionFunctions = "";

    /**
     * 权限数据源名称（对应 StpInterface.getPermissionData）
     */
    private String permissionDataName = "";

    public SaTokenIdentityProperties() {
    }

    // ------------------------------------------------------------------
    // getters / setters
    // ------------------------------------------------------------------

    public String getTokenName() {
        return tokenName;
    }

    public void setTokenName(String tokenName) {
        this.tokenName = tokenName;
    }

    public long getTimeout() {
        return timeout;
    }

    public void setTimeout(long timeout) {
        this.timeout = timeout;
    }

    public long getActiveTimeout() {
        return activeTimeout;
    }

    public void setActiveTimeout(long activeTimeout) {
        this.activeTimeout = activeTimeout;
    }

    public boolean isConcurrent() {
        return isConcurrent;
    }

    public void setConcurrent(boolean concurrent) {
        isConcurrent = concurrent;
    }

    public boolean isReadCookie() {
        return isReadCookie;
    }

    public void setReadCookie(boolean readCookie) {
        isReadCookie = readCookie;
    }

    public boolean isReadHeader() {
        return isReadHeader;
    }

    public void setReadHeader(boolean readHeader) {
        isReadHeader = readHeader;
    }

    public boolean isLog() {
        return isLog;
    }

    public void setLog(boolean log) {
        isLog = log;
    }

    public String getJwtSecretKey() {
        return jwtSecretKey;
    }

    public void setJwtSecretKey(String jwtSecretKey) {
        this.jwtSecretKey = jwtSecretKey;
    }

    public boolean isJwtEnabled() {
        return jwtEnabled;
    }

    public void setJwtEnabled(boolean jwtEnabled) {
        this.jwtEnabled = jwtEnabled;
    }

    public String getPermissionFunctions() {
        return permissionFunctions;
    }

    public void setPermissionFunctions(String permissionFunctions) {
        this.permissionFunctions = permissionFunctions;
    }

    public String getPermissionDataName() {
        return permissionDataName;
    }

    public void setPermissionDataName(String permissionDataName) {
        this.permissionDataName = permissionDataName;
    }
}
