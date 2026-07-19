package com.agenthub.client.auth;

import cn.dev33.satoken.stp.StpInterface;
import org.springframework.stereotype.Component;

import java.util.*;

/**
 * Sa-Token 权限数据源实现。
 *
 * <p>实现 {@link StpInterface} 接口，为 Sa-Token 提供权限和角色数据。
 *
 * <p>权限来源：
 * <ol>
 *   <li>优先从 Sa-Token Session 中读取（登录时写入）</li>
 *   <li>Session 无数据时，返回空集合</li>
 * </ol>
 *
 * <p>如需从数据库加载权限，可在此处注入 Repository 查询。
 */
@Component
public class SaTokenPermissionProvider implements StpInterface {

    /**
     * 获取权限列表（对应 AgentHub 的 permissions）。
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 权限列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        // 权限数据已在登录时写入 Sa-Token Session
        // Sa-Token 会自动从 Session 中读取，此处返回空即可
        return Collections.emptyList();
    }

    /**
     * 获取角色列表（对应 AgentHub 的 roles）。
     *
     * @param loginId   登录用户 ID
     * @param loginType 登录类型
     * @return 角色列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        // 角色数据已在登录时写入 Sa-Token Session
        return Collections.emptyList();
    }
}
