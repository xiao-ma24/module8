package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.auth.LoginUser;
import com.school.material.admin.auth.TokenStore;
import com.school.material.admin.common.BizException;
import com.school.material.admin.common.HashUtil;
import com.school.material.admin.entity.*;
import com.school.material.admin.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** 登录认证（演示级：内存令牌 + SHA-256 密码 + 失败锁定） */
@Service
@RequiredArgsConstructor
public class AuthService {

    private static final int MAX_RETRY = 5;

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysMenuMapper menuMapper;
    private final SysLoginLogMapper loginLogMapper;
    private final TokenStore tokenStore;
    private final org.springframework.jdbc.core.JdbcTemplate jdbc;
    private final AuditService audit;
    private final UserService userService;

    private final Map<String, Integer> failCount = new ConcurrentHashMap<>();

    public Map<String, Object> login(String userNo, String password, String ip, String device) {
        if (userNo == null || userNo.isBlank() || password == null || password.isBlank()) {
            throw new BizException(1001, "请输入工号与密码");
        }
        int failed = failCount.getOrDefault(userNo, 0);
        if (failed >= MAX_RETRY) {
            recordLogin(userNo, null, ip, device, 0, "连续失败 " + MAX_RETRY + " 次，账号已锁定");
            throw new BizException(1002, "连续失败 " + MAX_RETRY + " 次，账号已锁定，请联系管理员或稍后重试");
        }

        User user = userMapper.selectOne(Wrappers.<User>lambdaQuery().eq(User::getUserNo, userNo.trim()));
        if (user == null) {
            failCount.merge(userNo, 1, Integer::sum);
            recordLogin(userNo, null, ip, device, 0, "账号不存在");
            throw new BizException(1003, "工号或密码错误");
        }
        if (user.getUserStatus() == null || user.getUserStatus() != 1) {
            String why = user.getUserStatus() != null && user.getUserStatus() == 2
                    ? "账号待启用，请联系管理员完成部门与角色分配后启用"
                    : "该账号已停用，请联系管理员";
            recordLogin(userNo, user.getUserName(), ip, device, 0, why);
            throw new BizException(1004, why);
        }
        String hash = user.getPwdHash();
        if (!HashUtil.matches(password, hash)) {
            failCount.merge(userNo, 1, Integer::sum);
            recordLogin(userNo, user.getUserName(), ip, device, 0, "密码错误");
            throw new BizException(1003, "工号或密码错误");
        }
        failCount.remove(userNo);

        LoginUser lu = buildLoginUser(user);
        String token = tokenStore.issue(lu);

        User upd = new User();
        upd.setUserId(user.getUserId());
        upd.setLastLoginAt(LocalDateTime.now());
        // 历史无盐摘要登录成功后自动升级为加盐摘要
        if (!HashUtil.isSalted(hash)) upd.setPwdHash(HashUtil.hash(password));
        userMapper.updateById(upd);
        recordLogin(userNo, user.getUserName(), ip, device, 1, "登录成功");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", lu);
        data.put("menus", menuTree(user.getUserId()));
        data.put("needChangePwd", user.getPwdNeedChange() != null && user.getPwdNeedChange() == 1);
        data.put("perms", menuMapper.selectByUser(user.getUserId()).stream()
                .map(SysMenu::getPermCode).filter(Objects::nonNull).filter(s -> !s.isBlank())
                .collect(Collectors.toList()));
        return data;
    }

    /** 注册页下拉选项：启用状态的部门与可自助申请的岗位（排除 ADMIN） */
    public Map<String, Object> registerOptions() {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("depts", jdbc.queryForList(
                "SELECT org_id AS deptId, dept_name AS deptName FROM mat_dept WHERE dept_status = 1 ORDER BY sort_no, org_id"));
        data.put("roles", jdbc.queryForList(
                "SELECT role_code AS roleCode, role_name AS roleName FROM mat_sys_role " +
                        "WHERE role_status = 1 AND role_code <> 'ADMIN' ORDER BY sort_no"));
        data.put("userTypes", List.of(
                Map.of("value", 1, "label", "教职工"),
                Map.of("value", 2, "label", "学生"),
                Map.of("value", 3, "label", "外部人员")));
        data.put("teachers", userService.teacherOptions(null));
        data.put("minPwdLength", minPwdLength());
        return data;
    }

    /** 自助注册：工号/手机号/邮箱查重 → 自动建档（待启用）→ 绑定申请岗位 → 留痕，待管理员启用 */
    @Transactional
    public Map<String, Object> register(Map<String, String> body) {
        String userNo = trim(body.get("userNo"));
        String userName = trim(body.get("userName"));
        String phone = trim(body.get("phone"));
        String email = trim(body.get("email"));
        String deptIdStr = trim(body.get("deptId"));
        String roleCode = trim(body.get("applyRole"));
        String userTypeStr = trim(body.get("userType"));
        String pwd = body.get("password");
        String confirm = body.get("confirmPassword");
        String remark = trim(body.get("remark"));

        if (userNo == null) throw new BizException("请输入工号/学号");
        if (userName == null) throw new BizException("请输入姓名");
        if (phone == null) throw new BizException("请输入手机号");
        if (!phone.matches("^1\\d{10}$")) throw new BizException("手机号格式不正确，应为 11 位手机号");
        if (email != null && !email.matches("^[\\w.+-]+@[\\w-]+(\\.[\\w-]+)+$")) {
            throw new BizException("邮箱格式不正确");
        }
        if (deptIdStr == null) throw new BizException("请选择所属部门");
        if (roleCode == null) throw new BizException("请选择申请岗位");
        if ("ADMIN".equalsIgnoreCase(roleCode)) {
            throw new BizException("管理员账号由平台统一分配，不支持自助注册");
        }
        int min = minPwdLength();
        if (pwd == null || pwd.length() < min) throw new BizException("密码长度不能少于 " + min + " 位");
        if (!pwd.equals(confirm)) throw new BizException("两次输入的密码不一致");

        int userType = 1;
        if (userTypeStr != null) {
            try {
                userType = Integer.parseInt(userTypeStr);
            } catch (NumberFormatException ignore) {
            }
        }

        String deptId;
        try {
            deptId = String.format("%06d", Long.parseLong(deptIdStr));
        } catch (NumberFormatException e) {
            throw new BizException("所属部门不正确");
        }
        if (deptMapper.selectById(deptId) == null) throw new BizException("所属部门不存在");

        boolean teacherSource = userType == 1;
        if (teacherSource) {
            // 教职工工号必须来自师资系统（sys_teacher），姓名/部门以师资系统为准
            Map<String, Object> t = userService.teacher(userNo);
            if (t == null) {
                throw new BizException("工号 " + userNo + " 不在师资系统名单中，请从师资系统选择工号");
            }
            userName = String.valueOf(t.get("teacher_name"));
            if (t.get("dept_id") != null) deptId = String.valueOf(t.get("dept_id"));
        }

        List<String> conflicts = new ArrayList<>();
        if (countBy("person_no", userNo) > 0) conflicts.add("工号 " + userNo + " 已注册，请直接登录或联系管理员");
        if (countBy("phone", phone) > 0) conflicts.add("手机号 " + phone + " 已绑定其他账号");
        if (email != null && countBy("email", email) > 0) conflicts.add("邮箱 " + email + " 已绑定其他账号");
        if (!conflicts.isEmpty()) throw new BizException(String.join("；", conflicts));

        List<Map<String, Object>> roles = jdbc.queryForList(
                "SELECT role_id, role_name FROM mat_sys_role WHERE role_code = ? AND role_status = 1", roleCode);
        if (roles.isEmpty()) throw new BizException("申请岗位不存在或已停用");
        Object roleId = roles.get(0).get("role_id");
        String roleName = String.valueOf(roles.get(0).get("role_name"));

        User u = new User();
        u.setUserNo(userNo);
        u.setUserName(userName);
        u.setDeptId(deptId);
        u.setRoleCode(roleCode);
        u.setUserStatus(2);              // 待启用：管理员启用前不可登录
        u.setUserType(userType);
        u.setPhone(phone);
        u.setEmail(email);
        u.setHireDate(LocalDate.now());
        u.setPwdHash(HashUtil.hash(pwd));
        u.setPwdNeedChange(0);           // 密码由本人设置，无需再强制改密
        u.setSourceType(teacherSource ? "TEACHER" : "SELF");   // 教职工取自师资系统
        if (teacherSource) u.setSourceSyncAt(LocalDateTime.now());
        u.setCreatedBy(userName);
        u.setCreatedAt(LocalDateTime.now());
        u.setRemark("自助注册申请，申请岗位=" + roleName
                + (remark == null ? "" : "；说明=" + remark));
        userMapper.insert(u);

        jdbc.update("INSERT IGNORE INTO mat_sys_user_role(user_id, role_id, granted_by, granted_at) VALUES (?,?,?,?)",
                u.getUserId(), roleId, u.getUserId(), LocalDateTime.now());
        audit.write("用户注册", "自助注册", userNo + " " + userName,
                "部门=" + deptId + "，申请岗位=" + roleName + "，状态=待启用，来源=" + u.getSourceType() + "，等待管理员启用");

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("userId", u.getUserId());
        data.put("userNo", userNo);
        data.put("userName", userName);
        data.put("deptId", deptId);
        data.put("roleName", roleName);
        data.put("status", 2);
        data.put("statusText", "待启用");
        return data;
    }

    /** 修改本人密码：校验旧密码与最小长度（取系统参数 sys.pwd.minLength） */
    public void changePassword(Long userId, String oldPwd, String newPwd) {
        if (userId == null) throw new BizException(401, "登录已失效，请重新登录");
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException("用户不存在");
        if (oldPwd == null || !HashUtil.matches(oldPwd, u.getPwdHash())) {
            throw new BizException(1005, "原密码不正确");
        }
        int min = minPwdLength();
        if (newPwd == null || newPwd.length() < min) throw new BizException("新密码长度不能少于 " + min + " 位");
        if (newPwd.equals(oldPwd)) throw new BizException("新密码不能与原密码相同");

        User upd = new User();
        upd.setUserId(userId);
        upd.setPwdHash(HashUtil.hash(newPwd));
        upd.setPwdNeedChange(0);
        upd.setPwdUpdatedAt(LocalDateTime.now());
        userMapper.updateById(upd);
        audit.write("账号安全", "修改密码", u.getUserNo() + " " + u.getUserName(), "本人修改登录密码");
    }

    public Map<String, Object> me(String token) {
        LoginUser lu = tokenStore.get(token);
        if (lu == null) throw new BizException(401, "登录已失效，请重新登录");
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("user", lu);
        data.put("menus", menuTree(lu.getUserId()));
        return data;
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    private LoginUser buildLoginUser(User u) {
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getUserId());
        lu.setUserNo(u.getUserNo());
        lu.setUserName(u.getUserName());
        lu.setRoleCode(u.getRoleCode());
        lu.setDeptId(u.getDeptId());
        if (u.getDeptId() != null) {
            Dept d = deptMapper.selectById(u.getDeptId());
            if (d != null) lu.setDeptName(d.getDeptName());
        }
        SysRole role = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, u.getRoleCode()));
        lu.setRoleName(role == null ? u.getRoleCode() : role.getRoleName());
        List<String> roles = jdbc.queryForList(
                "SELECT r.role_code FROM mat_sys_user_role ur JOIN mat_sys_role r ON ur.role_id = r.role_id WHERE ur.user_id = ?",
                String.class, u.getUserId());
        if (roles.isEmpty() && u.getRoleCode() != null) roles = List.of(u.getRoleCode());
        lu.setRoles(roles);
        return lu;
    }

    private List<SysMenu> menuTree(Long userId) {
        List<SysMenu> all = menuMapper.selectByUser(userId);
        Map<Long, SysMenu> map = all.stream().collect(Collectors.toMap(SysMenu::getMenuId, m -> m, (a, b) -> a, LinkedHashMap::new));
        List<SysMenu> roots = new ArrayList<>();
        for (SysMenu m : all) {
            SysMenu parent = m.getParentId() == null ? null : map.get(m.getParentId());
            if (parent == null) roots.add(m);
            else parent.getChildren().add(m);
        }
        return roots;
    }

    private void recordLogin(String userNo, String userName, String ip, String device, int result, String msg) {
        SysLoginLog log = new SysLoginLog();
        log.setUserNo(userNo);
        log.setUserName(userName);
        log.setIp(ip);
        log.setDevice(device);
        log.setResult(result);
        log.setMessage(msg);
        log.setCreatedAt(LocalDateTime.now());
        loginLogMapper.insert(log);
    }

    /** 密码最小长度：取系统参数 sys.pwd.minLength，缺省 6 */
    private int minPwdLength() {
        try {
            List<String> v = jdbc.queryForList(
                    "SELECT config_value FROM sys_config WHERE config_key = 'sys.pwd.minLength'", String.class);
            if (!v.isEmpty()) return Integer.parseInt(v.get(0));
        } catch (Exception ignore) {
        }
        return 6;
    }

    /** 注册查重：按字段统计未删除账号数量 */
    private long countBy(String column, String value) {
        Long n = jdbc.queryForObject(
                "SELECT COUNT(*) FROM mat_sys_user WHERE " + column + " = ? AND del_flag = 0", Long.class, value);
        return n == null ? 0 : n;
    }

    private String trim(String s) {
        return s == null || s.isBlank() ? null : s.trim();
    }
}
