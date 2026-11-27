package com.school.material.admin.auth;

import com.school.material.admin.security.CurrentUser;
import com.school.material.admin.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.nio.charset.StandardCharsets;

/** 统一令牌校验：请求头 X-Token，未登录返回 401 的 JSON 结构 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final TokenStore tokenStore;

    private final CurrentUserService currentUserService;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    @Value("${auth.enforce:false}")
    private boolean authEnforce;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) return true;
        // DBA 统一登录已生效：身份与权限交给 IdentityInterceptor（HTTP 401/403）
        if (authEnforce) {
            CurrentUser cu = currentUserService.currentOrNull();
            if (cu != null) {
                LoginUser lu = new LoginUser();
                Long uid = null;
                String uname = cu.userId();
                String deptId = cu.businessId();
                String deptName = null;
                try { uid = Long.valueOf(cu.userId()); } catch (NumberFormatException ignored) { }
                if (uid == null) {
                    try {
                        java.util.Map<String, Object> row = jdbc.queryForMap(
                                "SELECT u.user_id, u.user_name, u.org_id, d.dept_name FROM mat_sys_user u "
                                        + "LEFT JOIN mat_dept d ON d.org_id = u.org_id "
                                        + "WHERE u.person_no = ? AND u.del_flag = 0 LIMIT 1",
                                cu.userId());
                        uid = ((Number) row.get("user_id")).longValue();
                        uname = String.valueOf(row.get("user_name"));
                        Object org = row.get("org_id");
                        if ((deptId == null || deptId.isBlank()) && org != null) deptId = String.valueOf(org);
                        Object dn = row.get("dept_name");
                        if (dn != null) deptName = String.valueOf(dn);
                    } catch (Exception ignore) { }
                }
                lu.setUserId(uid);
                lu.setUserNo(cu.userId());
                lu.setUserName(uname);
                lu.setDeptId(deptId);
                lu.setDeptName(deptName);
                lu.setRoleCode(cu.role());
                lu.setRoleName(cu.role());
                lu.setExpireAt(Long.MAX_VALUE);
                lu.setRoles(java.util.List.of(cu.role()));
                AuthContext.set(lu);
                return true;
            }
        }
        String token = request.getHeader("X-Token");
        LoginUser user = tokenStore.get(token);
        if (user == null) {
            response.setStatus(200);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"msg\":\"登录已失效，请重新登录\",\"data\":null}");
            response.getWriter().flush();
            return false;
        }
        AuthContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    public static String body(Object o) {
        return new String(String.valueOf(o).getBytes(StandardCharsets.UTF_8), StandardCharsets.UTF_8);
    }
}
