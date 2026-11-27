package com.school.material.borrow.auth;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口角色约束：标注在 Controller 方法或类上，由 {@link AuthInterceptor} 统一校验。
 * 不标注表示「登录即可访问」；标注后要求当前用户具备其中任一角色。
 * ADMIN 仅通过 {@link #allowAdmin()} 显式放开，避免管理员越权执行业务动作。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RequireRoles {

    /** 允许的角色编码（满足任一即可） */
    String[] value();

    /** 是否允许只读管理员访问（默认不允许，管理员不参与业务动作） */
    boolean allowAdmin() default false;
}
