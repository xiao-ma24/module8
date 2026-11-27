package com.school.material.security;

/**
 * 统一身份入口（接口）：业务代码只依赖本接口，DBA 集成时替换实现类即可。
 * 契约与《给各组的接入通知》§2.1 保持一致。
 */
public interface CurrentUserService {

    /** 当前用户；未登录返回 null（是否拒绝由调用方决定） */
    CurrentUser currentOrNull();

    /** 当前用户；未登录抛出 401 */
    CurrentUser current();
}
