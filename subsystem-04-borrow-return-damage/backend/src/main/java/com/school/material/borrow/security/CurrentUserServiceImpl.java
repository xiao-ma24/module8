package com.school.material.borrow.security;

import com.school.material.borrow.auth.AuthContext;
import com.school.material.borrow.auth.LoginUser;
import com.school.material.borrow.common.BizException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 统一身份入口：业务代码获取“当前登录用户”必须调用本类，禁止各自解析请求。
 *
 * <p>当前实现同时兼容两种来源：
 * <ol>
 *   <li>DBA 统一登录下发：身份请求头 / Cookie（接入时按 DBA 规范调整属性名）；</li>
 *   <li>课程演示登录态：原有 AuthContext（内存令牌）。</li>
 * </ol>
 * DBA 正式接入时只需替换本类实现（改为校验统一登录令牌或调用 SDK），
 * 业务代码与 {@link CurrentUser} 契约保持不变。
 */
@Service
public class CurrentUserServiceImpl implements CurrentUserService {

    @Value("${auth.identity-cookie-name:DBA_TOKEN}")
    private String cookieName;

    @Value("${auth.identity-user-header:X-DBA-USER-ID}")
    private String userHeader;

    @Value("${auth.identity-role-header:X-DBA-ROLE}")
    private String roleHeader;

    @Value("${auth.identity-business-header:X-DBA-BUSINESS-ID}")
    private String businessHeader;

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.jdbc.core.JdbcTemplate jdbc;

    /** 当前用户；未登录返回 null（是否拒绝由接口按需决定） */
    @Override
    public CurrentUser currentOrNull() {
        LoginUser demo = AuthContext.get();
        if (demo != null && demo.getUserId() != null) {
            return new CurrentUser(String.valueOf(demo.getUserId()), demo.getRoleCode(),
                    demo.getDeptId() == null ? null : String.valueOf(demo.getDeptId()));
        }
        HttpServletRequest req = request();
        if (req == null) return null;
        String userId = firstNonBlank(req.getHeader(userHeader), cookieValue(req, cookieName));
        if (userId == null) return null;
        String role = trimToNull(req.getHeader(roleHeader));
        // DBA 约定（2026-11-27）：部门不从 X-DBA-BUSINESS-ID 取，改为按工号查本组 mat_sys_user.org_id；
        // 请求头仅作过渡期兼容，验收后关闭。
        String orgId = orgIdOf(userId);
        if (orgId == null) {
            orgId = trimToNull(req.getHeader(businessHeader));
        }
        return new CurrentUser(userId, role, orgId);
    }

    /** 当前用户；未登录抛出 401 业务异常 */
    @Override
    public CurrentUser current() {
        CurrentUser user = currentOrNull();
        if (user == null) throw new BizException(401, "未登录或登录已失效");
        return user;
    }

    private static HttpServletRequest request() {
        if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attrs) {
            return attrs.getRequest();
        }
        return null;
    }

    private static String cookieValue(HttpServletRequest req, String name) {
        if (req.getCookies() == null) return null;
        for (Cookie c : req.getCookies()) {
            if (name.equals(c.getName())) return c.getValue();
        }
        return null;
    }

    private static String firstNonBlank(String a, String b) {
        String x = trimToNull(a);
        return x != null ? x : trimToNull(b);
    }

    private static String trimToNull(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }


    /** 按工号（X-DBA-USER-ID）查本组用户表，取部门 org_id */
    private String orgIdOf(String personNo) {
        if (personNo == null || jdbc == null) return null;
        try {
            java.util.List<String> rows = jdbc.queryForList(
                    "SELECT org_id FROM mat_sys_user WHERE person_no = ? AND del_flag = 0 LIMIT 1",
                    String.class, personNo);
            return rows.isEmpty() ? null : trimToNull(rows.get(0));
        } catch (Exception ignore) {
            return null;
        }
    }

}
