package com.school.material.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Arrays;

/**
 * 统一鉴权拦截器：DBA 接入后把 auth.enforce 置 true 生效。
 * 未登录 -> HTTP 401；角色不匹配 -> HTTP 403。
 * 默认关闭，避免破坏各组本地演示登录（X-Token / mockUserId）。
 */
@Component
@RequiredArgsConstructor
public class IdentityInterceptor implements HandlerInterceptor {

    private final CurrentUserService currentUserService;

    @Value("${auth.enforce:false}")
    private boolean enforce;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if (!enforce) return true;
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;

        String uri = request.getRequestURI();
        if (uri.contains("/auth/") || uri.contains("/swagger") || uri.contains("/v3/api-docs")
                || uri.contains("/error") || uri.contains("/actuator")) {
            return true;
        }

        CurrentUser user = currentUserService.currentOrNull();
        if (user == null) {
            write(response, 401, "未登录或登录已失效");
            return false;
        }

        if (handler instanceof HandlerMethod hm) {
            RequireRole need = hm.getMethodAnnotation(RequireRole.class);
            if (need == null) need = hm.getBeanType().getAnnotation(RequireRole.class);
            if (need != null && Arrays.stream(need.value()).noneMatch(r -> r.equalsIgnoreCase(user.role()))) {
                write(response, 403, "当前角色无权执行该操作");
                return false;
            }
        }
        return true;
    }

    private void write(HttpServletResponse response, int status, String msg) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + status + ",\"msg\":\"" + msg + "\",\"data\":null}");
        response.getWriter().flush();
    }
}
