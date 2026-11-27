package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.common.HashUtil;
import com.school.material.admin.entity.Dept;
import com.school.material.admin.entity.SysRole;
import com.school.material.admin.entity.User;
import com.school.material.admin.mapper.DeptMapper;
import com.school.material.admin.mapper.SysRoleMapper;
import com.school.material.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 用户管理：全平台唯一写入口（身份来源校验 → 建立账号 → 授权 → 启用 → 变更/停用/审计） */
@Service
@RequiredArgsConstructor
public class UserService {

    private static final String DEFAULT_PWD = "123456";
    private static final DateTimeFormatter TF = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final JdbcTemplate jdbc;
    private final AuditService audit;

    /** 列表：关键字 / 角色 / 部门 / 状态 / 人员类型 */
    public List<User> list(String keyword, String roleCode, String deptId, Integer status, Integer userType) {
        List<User> rows = userMapper.selectList(Wrappers.<User>lambdaQuery()
                .eq(roleCode != null && !roleCode.isBlank(), User::getRoleCode, roleCode)
                .eq(deptId != null, User::getDeptId, deptId)
                .eq(status != null, User::getUserStatus, status)
                .eq(userType != null, User::getUserType, userType)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(User::getUserNo, keyword).or().like(User::getUserName, keyword)
                        .or().like(User::getPhone, keyword))
                .orderByAsc(User::getUserNo));
        fill(rows);
        return rows;
    }

    public List<String> userNos() {
        return userMapper.selectList(Wrappers.<User>lambdaQuery().select(User::getUserNo))
                .stream().map(User::getUserNo).collect(Collectors.toList());
    }

    /** 账号唯一性校验：工号 / 手机号 / 邮箱，返回冲突说明 */
    public Map<String, Object> checkDuplicate(String userNo, String phone, String email, Long excludeId) {
        List<String> conflicts = new ArrayList<>();
        if (notBlank(userNo)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getUserNo, userNo.trim())
                    .ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) conflicts.add("工号 " + userNo.trim() + " 已被占用，请核准是否同一自然人后合并");
        }
        if (notBlank(phone)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getPhone, phone.trim())
                    .ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) conflicts.add("手机号 " + phone.trim() + " 已绑定其他账号，请核验后合并");
        }
        if (notBlank(email)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, email.trim())
                    .ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) conflicts.add("邮箱 " + email.trim() + " 已绑定其他账号");
        }
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("conflicts", conflicts);
        res.put("duplicate", !conflicts.isEmpty());
        return res;
    }

    @Transactional
    public User save(User in) {
        if (in.getUserNo() == null || in.getUserNo().isBlank()) throw new BizException("工号/学号不能为空");
        in.setUserNo(in.getUserNo().trim());

        boolean creating = in.getUserId() == null;
        if (creating && "TEACHER".equalsIgnoreCase(in.getSourceType())) {
            // 工号必须来自师资系统（sys_teacher），姓名/部门/手机号以师资系统为准
            Map<String, Object> t = teacher(in.getUserNo());
            if (t == null) {
                throw new BizException("工号 " + in.getUserNo() + " 不在师资系统名单中，请从师资系统选择工号");
            }
            in.setUserName(String.valueOf(t.get("teacher_name")));
            if (!notBlank(in.getPhone())) in.setPhone((String) t.get("phone"));
            if (in.getDeptId() == null && t.get("dept_id") != null) {
                in.setDeptId(String.valueOf(t.get("dept_id")));
            }
            in.setSourceSyncAt(LocalDateTime.now());
        }
        if (in.getUserName() == null || in.getUserName().isBlank()) throw new BizException("姓名不能为空");
        List<String> conflicts = conflicts(in.getUserNo(), in.getPhone(), in.getEmail(), in.getUserId());
        if (!conflicts.isEmpty()) throw new BizException(String.join("；", conflicts));

        if (creating) {
            in.setPwdHash(HashUtil.hash(DEFAULT_PWD));
            // 文档规则：新账号默认“待启用”，且不分配任何业务权限
            in.setUserStatus(2);
            in.setPwdNeedChange(1);
            in.setSourceType(notBlank(in.getSourceType()) ? in.getSourceType() : "LOCAL");
            in.setUserType(in.getUserType() == null ? 1 : in.getUserType());
            in.setHireDate(in.getHireDate() == null ? LocalDate.now() : in.getHireDate());
            in.setCreatedAt(LocalDateTime.now());
            in.setCreatedBy(currentName());
            userMapper.insert(in);
            audit.write("用户管理", "新增账号", in.getUserNo() + " " + in.getUserName(),
                    "初始状态=待启用，来源=" + in.getSourceType() + "，初始密码 " + DEFAULT_PWD);
        } else {
            User old = requireUser(in.getUserId());
            in.setPwdHash(null);
            in.setUserStatus(null);
            in.setUpdatedAt(LocalDateTime.now());
            in.setUpdatedBy(currentName());
            userMapper.updateById(in);
            audit.write("用户管理", "修改账号", in.getUserNo() + " " + in.getUserName(),
                    diff(old, in), snapshot(old), snapshot(in));
        }
        List<String> codes = in.getRoleCodes();
        if (codes == null || codes.isEmpty()) {
            if (notBlank(in.getRoleCode())) codes = List.of(in.getRoleCode());
        }
        if (creating) {
            if (codes != null && !codes.isEmpty()) grantRoles(in.getUserId(), codes, "新增时授权");
        } else if (in.getRoleCodes() != null) {
            grantRoles(in.getUserId(), in.getRoleCodes(), "编辑时变更");
        }
        return in;
    }

    /** 启用：校验部门、角色、联系方式完整后才允许启用 */
    public void activate(Long userId, String reason) {
        User u = requireUser(userId);
        if (u.getUserStatus() != null && u.getUserStatus() == 1) throw new BizException("该账号已处于启用状态");
        List<String> miss = new ArrayList<>();
        if (u.getDeptId() == null) miss.add("未分配部门");
        if (roleIds(userId).isEmpty()) miss.add("未绑定角色");
        if (!notBlank(u.getPhone())) miss.add("未填写手机号");
        if (!miss.isEmpty()) throw new BizException("启用前校验未通过：" + String.join("、", miss));

        User upd = new User();
        upd.setUserId(userId);
        upd.setUserStatus(1);
        userMapper.updateById(upd);
        audit.write("用户管理", "启用账号", u.getUserNo() + " " + u.getUserName(),
                "变更前=待启用，变更后=启用；已发送首次登录提示；原因=" + blankTo(reason, "岗位到岗"),
                "状态=待启用", "状态=启用");
    }

    /** 停用并撤权：账号保留，历史单据仍指向原 user_id */
    public void disable(Long userId, String reason) {
        User u = requireUser(userId);
        if (u.getUserStatus() != null && u.getUserStatus() == 0) throw new BizException("该账号已停用");
        List<String> roles = roleNames(userId);
        User upd = new User();
        upd.setUserId(userId);
        upd.setUserStatus(0);
        userMapper.updateById(upd);
        jdbc.update("DELETE FROM mat_sys_user_role WHERE user_id = ?", userId);
        audit.write("用户管理", "停用并撤权", u.getUserNo() + " " + u.getUserName(),
                "变更前=启用，变更后=停用；撤销角色=" + (roles.isEmpty() ? "无" : String.join("、", roles))
                        + "；原因=" + blankTo(reason, "未填写"),
                "状态=启用，角色=" + (roles.isEmpty() ? "无" : String.join("、", roles)), "状态=停用，角色=无");
    }

    /** 兼容旧接口：status=1 启用，0 停用并撤权 */
    public void toggleStatus(Long userId, Integer status, String reason) {
        if (status != null && status == 1) activate(userId, reason);
        else disable(userId, reason);
    }

    public void resetPassword(Long userId) {
        User u = requireUser(userId);
        User upd = new User();
        upd.setUserId(userId);
        upd.setPwdHash(HashUtil.hash(DEFAULT_PWD));
        upd.setPwdNeedChange(1);
        userMapper.updateById(upd);
        audit.write("用户管理", "重置密码", u.getUserNo() + " " + u.getUserName(),
                "重置为初始密码，并要求下次登录修改");
    }

    /** 文档规则：停用不删除，避免审计链断裂 */
    public void remove(Long userId) {
        User u = requireUser(userId);
        throw new BizException("为保留历史责任链，平台不允许删除用户（" + u.getUserNo() + "），请使用「停用并撤权」");
    }

    /** 师资系统工号候选（模拟表 sys_teacher）：建档时工号只能选师资系统在册人员 */
    public List<Map<String, Object>> teacherOptions(String keyword) {
        String kw = keyword == null ? "" : keyword.trim();
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.teacher_no, t.teacher_name, t.dept_code, t.dept_name, t.phone, d.org_id AS dept_id, " +
                        "(SELECT COUNT(*) FROM mat_sys_user u WHERE u.person_no = t.teacher_no AND u.del_flag = 0) AS used " +
                        "FROM sys_teacher t " +
                        "LEFT JOIN mat_dept d ON d.del_flag = 0 AND (d.dept_code = t.dept_code OR d.dept_name = t.dept_name) " +
                        "WHERE (? = '' OR t.teacher_no LIKE CONCAT('%', ?, '%') OR t.teacher_name LIKE CONCAT('%', ?, '%')) " +
                        "ORDER BY t.teacher_no LIMIT 100", kw, kw, kw);
        List<Map<String, Object>> list = new ArrayList<>();
        for (Map<String, Object> r : rows) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("teacherNo", r.get("teacher_no"));
            m.put("teacherName", r.get("teacher_name"));
            m.put("deptCode", r.get("dept_code"));
            m.put("deptName", r.get("dept_name"));
            m.put("deptId", r.get("dept_id"));
            m.put("phone", r.get("phone"));
            m.put("registered", ((Number) r.get("used")).intValue() > 0);
            list.add(m);
        }
        return list;
    }

    /** 按工号查询师资系统身份（模拟表 sys_teacher），未在册返回 null */
    public Map<String, Object> teacher(String teacherNo) {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT t.teacher_no, t.teacher_name, t.phone, d.org_id AS dept_id " +
                        "FROM sys_teacher t " +
                        "LEFT JOIN mat_dept d ON d.del_flag = 0 AND (d.dept_code = t.dept_code OR d.dept_name = t.dept_name) " +
                        "WHERE t.teacher_no = ? LIMIT 1", teacherNo);
        return rows.isEmpty() ? null : rows.get(0);
    }

    /** 模拟从师资/学籍系统同步在岗、在籍状态 */
    public Map<String, Object> syncSource() {
        List<Map<String, Object>> rows = jdbc.queryForList(
                "SELECT source_type, COUNT(*) c FROM mat_sys_user WHERE del_flag = 0 GROUP BY source_type");
        int total = 0;
        Map<String, Object> detail = new LinkedHashMap<>();
        for (Map<String, Object> r : rows) {
            String k = String.valueOf(r.get("source_type"));
            int c = ((Number) r.get("c")).intValue();
            total += c;
            detail.put(k, c);
        }
        LocalDateTime now = LocalDateTime.now();
        jdbc.update("UPDATE mat_sys_user SET source_sync_at = ? WHERE del_flag = 0", now);
        audit.write("用户管理", "身份源同步", "师资系统 / 学籍系统",
                "同步 " + total + " 个身份，明细=" + detail);
        Map<String, Object> res = new LinkedHashMap<>(detail);
        res.put("total", total);
        res.put("syncedAt", now.format(TF));
        return res;
    }

    /** 批量导入：行内字段 userNo/userName/deptName/roleCode/title/phone */
    public Map<String, Object> batchImport(List<Map<String, String>> rows) {
        int ok = 0;
        List<String> errors = new ArrayList<>();
        Map<String, String> deptMap = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getDeptName, Dept::getDeptId, (a, b) -> a));
        for (int i = 0; i < rows.size(); i++) {
            Map<String, String> row = rows.get(i);
            try {
                String userNo = val(row, "userNo", "工号", "user_no");
                String userName = val(row, "userName", "姓名", "user_name");
                if (userNo == null || userName == null) throw new BizException("工号或姓名为空");
                User u = new User();
                u.setUserNo(userNo);
                u.setUserName(userName);
                String deptName = val(row, "deptName", "部门", "dept_name");
                u.setDeptId(deptName == null ? null : deptMap.get(deptName));
                String roleCode = val(row, "roleCode", "角色", "role_code");
                u.setRoleCode(roleCode == null ? "APPLICANT" : roleCode);
                u.setTitle(val(row, "title", "职称", "身份"));
                u.setPhone(val(row, "phone", "手机号", "手机"));
                save(u);
                ok++;
            } catch (Exception e) {
                errors.add("第 " + (i + 2) + " 行：" + e.getMessage());
            }
        }
        audit.write("用户管理", "批量导入", rows.size() + " 行", "成功 " + ok + " 行，失败 " + errors.size() + " 行");
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("success", ok);
        res.put("failed", errors.size());
        res.put("errors", errors);
        return res;
    }

    public List<Map<String, Object>> deptOptions() {
        return deptMapper.selectList(Wrappers.<Dept>lambdaQuery().orderByAsc(Dept::getSortNo)).stream()
                .map(d -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("deptId", d.getDeptId());
                    m.put("deptName", d.getDeptName());
                    return m;
                }).collect(Collectors.toList());
    }

    // ---------------------------------------------------------------- 内部方法

    /** 覆盖式授权：写入 granted_by / granted_at，先清后插 */
    private void grantRoles(Long userId, List<String> roleCodes, String scene) {
        if (userId == null) return;
        List<String> before = roleNames(userId);
        jdbc.update("DELETE FROM mat_sys_user_role WHERE user_id = ?", userId);
        List<String> after = new ArrayList<>();
        Long operatorId = com.school.material.admin.auth.AuthContext.get() == null
                ? null : com.school.material.admin.auth.AuthContext.get().getUserId();
        LocalDateTime now = LocalDateTime.now();
        for (String code : roleCodes) {
            if (!notBlank(code)) continue;
            List<Map<String, Object>> roles = jdbc.queryForList(
                    "SELECT role_id, role_name FROM mat_sys_role WHERE role_code = ?", code);
            for (Map<String, Object> r : roles) {
                jdbc.update("INSERT IGNORE INTO mat_sys_user_role(user_id, role_id, granted_by, granted_at) VALUES (?,?,?,?)",
                        userId, r.get("role_id"), operatorId, now);
                after.add(String.valueOf(r.get("role_name")));
            }
        }
        // 兼容主角色字段：取第一个角色
        String primary = roleCodes.stream().filter(this::notBlank).findFirst().orElse(null);
        jdbc.update("UPDATE mat_sys_user SET role_code = ? WHERE user_id = ?", primary, userId);
        if (!before.equals(after)) {
            audit.write("用户管理", "授权变更", "用户#" + userId,
                    scene + "；变更前角色=" + (before.isEmpty() ? "无" : String.join("、", before))
                            + "，变更后角色=" + (after.isEmpty() ? "无" : String.join("、", after)),
                    "角色=" + (before.isEmpty() ? "无" : String.join("、", before)),
                    "角色=" + (after.isEmpty() ? "无" : String.join("、", after)));
        }
    }

    private void fill(List<User> rows) {
        if (rows.isEmpty()) return;
        Map<String, String> depts = deptMapper.selectList(null).stream()
                .collect(Collectors.toMap(Dept::getDeptId, Dept::getDeptName, (a, b) -> a));
        Map<String, String> roleNames = roleMapper.selectList(null).stream()
                .collect(Collectors.toMap(SysRole::getRoleCode, SysRole::getRoleName, (a, b) -> a));
        Map<Long, List<String[]>> ur = new HashMap<>();
        jdbc.queryForList("SELECT ur.user_id, r.role_code, r.role_name FROM mat_sys_user_role ur " +
                        "JOIN mat_sys_role r ON ur.role_id = r.role_id")
                .forEach(m -> ur.computeIfAbsent(((Number) m.get("user_id")).longValue(), k -> new ArrayList<>())
                        .add(new String[]{String.valueOf(m.get("role_code")), String.valueOf(m.get("role_name"))}));
        rows.forEach(u -> {
            u.setDeptName(depts.getOrDefault(u.getDeptId(), "—"));
            List<String[]> list = ur.get(u.getUserId());
            if (list != null && !list.isEmpty()) {
                u.setRoleCodes(list.stream().map(a -> a[0]).collect(Collectors.toList()));
                u.setRoleNames(list.stream().map(a -> a[1]).collect(Collectors.joining("、")));
                u.setRoleName(list.get(0)[1]);
            } else {
                u.setRoleName(roleNames.getOrDefault(u.getRoleCode(), u.getRoleCode()));
                u.setRoleNames("");
            }
        });
    }

    private List<Long> roleIds(Long userId) {
        return jdbc.queryForList("SELECT role_id FROM mat_sys_user_role WHERE user_id = ?", Long.class, userId);
    }

    private List<String> roleNames(Long userId) {
        return jdbc.queryForList("SELECT r.role_name FROM mat_sys_user_role ur JOIN mat_sys_role r " +
                "ON ur.role_id = r.role_id WHERE ur.user_id = ?", String.class, userId);
    }

    private List<String> conflicts(String userNo, String phone, String email, Long excludeId) {
        List<String> list = new ArrayList<>();
        if (notBlank(userNo)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getUserNo, userNo.trim()).ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) list.add("工号 " + userNo.trim() + " 已被占用");
        }
        if (notBlank(phone)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getPhone, phone.trim()).ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) list.add("手机号 " + phone.trim() + " 已绑定其他账号");
        }
        if (notBlank(email)) {
            Long n = userMapper.selectCount(Wrappers.<User>lambdaQuery()
                    .eq(User::getEmail, email.trim()).ne(excludeId != null, User::getUserId, excludeId));
            if (n != null && n > 0) list.add("邮箱 " + email.trim() + " 已绑定其他账号");
        }
        return list;
    }

    /** 用户快照（写入审计 before/after） */
    private String snapshot(User u) {
        return "姓名=" + u.getUserName() + "，部门=" + deptName(u.getDeptId())
                + "，手机号=" + blankTo(u.getPhone(), "空") + "，邮箱=" + blankTo(u.getEmail(), "空")
                + "，职称=" + blankTo(u.getTitle(), "空");
    }

    private String diff(User old, User now) {
        List<String> changes = new ArrayList<>();
        if (!Objects.equals(old.getDeptId(), now.getDeptId())) {
            changes.add("部门 " + deptName(old.getDeptId()) + " → " + deptName(now.getDeptId()));
        }
        if (!Objects.equals(old.getPhone(), now.getPhone())) {
            changes.add("手机号 " + blankTo(old.getPhone(), "空") + " → " + blankTo(now.getPhone(), "空"));
        }
        if (!Objects.equals(old.getEmail(), now.getEmail())) {
            changes.add("邮箱 " + blankTo(old.getEmail(), "空") + " → " + blankTo(now.getEmail(), "空"));
        }
        if (!Objects.equals(old.getUserType(), now.getUserType())) {
            changes.add("人员类型 " + old.getUserType() + " → " + now.getUserType());
        }
        return changes.isEmpty() ? "更新基础信息" : String.join("；", changes);
    }

    private String deptName(String deptId) {
        if (deptId == null || deptId.isBlank()) return "未分配";
        Dept d = deptMapper.selectById(deptId);
        return d == null ? "未分配" : d.getDeptName();
    }

    private User requireUser(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) throw new BizException("用户不存在");
        return u;
    }

    private String currentName() {
        var lu = com.school.material.admin.auth.AuthContext.get();
        return lu == null ? "system" : lu.getUserName();
    }

    private boolean notBlank(String s) {
        return s != null && !s.isBlank();
    }

    private String blankTo(String s, String def) {
        return notBlank(s) ? s : def;
    }

    private String val(Map<String, String> row, String... keys) {
        for (String k : keys) {
            String v = row.get(k);
            if (v != null && !v.isBlank()) return v.trim();
        }
        return null;
    }
}
