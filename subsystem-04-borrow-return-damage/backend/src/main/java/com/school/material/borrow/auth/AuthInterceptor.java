package com.school.material.borrow.auth;

import com.school.material.borrow.security.CurrentUser;
import com.school.material.borrow.security.CurrentUserService;
import org.springframework.beans.factory.annotation.Value;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

/** 统一令牌校验：请求头 X-Token；并校验 {@link RequireRoles} 声明的角色。 */
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
                AuthContext.set(lu);
                return true;
            }
        }
        // 登录接口与文档接口放行
        String uri = request.getRequestURI();
        if (uri.startsWith("/api/v1/borrow/auth/") || uri.contains("/swagger") || uri.contains("/v3/api-docs")) {
            return true;
        }

        LoginUser user = tokenStore.get(request.getHeader("X-Token"));
        if (user == null) {
            write(response, 401, "登录已失效，请重新登录");
            return false;
        }

        if (handler instanceof HandlerMethod hm) {
            RequireRoles need = hm.getMethodAnnotation(RequireRoles.class);
            if (need == null) need = hm.getBeanType().getAnnotation(RequireRoles.class);
            if (need != null) {
                boolean roleOk = false;
                for (String r : need.value()) {
                    if (r.equalsIgnoreCase(user.getRoleCode())) {
                        roleOk = true;
                        break;
                    }
                }
                boolean adminOk = need.allowAdmin() && user.isAdmin();
                if (!roleOk && !adminOk) {
                    write(response, 403, "当前角色（" + user.getRoleName() + "）无权执行该操作");
                    return false;
                }
            }
        }

        AuthContext.set(user);
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) {
        AuthContext.clear();
    }

    private void write(HttpServletResponse response, int code, String msg) throws Exception {
        response.setStatus(200);
        response.setContentType("application/json;charset=UTF-8");
        response.getWriter().write("{\"code\":" + code + ",\"msg\":\"" + msg + "\",\"data\":null}");
        response.getWriter().flush();
    }
}
