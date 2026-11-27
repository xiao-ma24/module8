package com.school.material.borrow.auth;

import com.school.material.borrow.common.BizException;

/** 当前请求的登录用户（由 AuthInterceptor 写入） */
public final class AuthContext {

    private static final ThreadLocal<LoginUser> HOLDER = new ThreadLocal<>();

    private AuthContext() {
    }

    public static void set(LoginUser user) {
        HOLDER.set(user);
    }

    public static LoginUser get() {
        return HOLDER.get();
    }

    /** 取当前用户，未登录抛出 4000；业务层取操作人一律走这里，避免前端伪造 userId */
    public static LoginUser require() {
        LoginUser u = HOLDER.get();
        if (u == null) throw new BizException(4000, "登录已失效，请重新登录");
        return u;
    }

    public static void clear() {
        HOLDER.remove();
    }
}
