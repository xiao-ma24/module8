package com.school.material.stocktake.security;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 接口级角色校验，配合 {@link IdentityInterceptor} 使用。
 * 取值见 docs/delivery/身份权限矩阵.md，如 @RequireRole({"STORE_KEEPER", "ADMIN"})。
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface RequireRole {
    String[] value();
}
