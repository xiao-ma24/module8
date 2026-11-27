package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.Dept;
import com.school.material.admin.entity.SysAuditLog;
import com.school.material.admin.entity.SysRole;
import com.school.material.admin.entity.User;
import com.school.material.admin.mapper.DeptMapper;
import com.school.material.admin.mapper.SysAuditLogMapper;
import com.school.material.admin.mapper.SysRoleMapper;
import com.school.material.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/** 管理员个人中心 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final SysRoleMapper roleMapper;
    private final SysAuditLogMapper auditMapper;
    private final JdbcTemplate jdbc;

    public Map<String, Object> profile(Long userId) {
        User user = userMapper.selectById(userId == null ? 5L : userId);
        if (user == null) throw new BizException("用户不存在");
        final User u = user;

        Map<String, Object> info = new LinkedHashMap<>();
        info.put("userId", u.getUserId());
        info.put("userNo", u.getUserNo());
        info.put("userName", u.getUserName());
        info.put("roleCode", u.getRoleCode());
        info.put("roleName", roleName(u.getRoleCode()));
        info.put("deptId", u.getDeptId());
        Dept dept = u.getDeptId() == null ? null : deptMapper.selectById(u.getDeptId());
        info.put("deptName", dept == null ? "—" : dept.getDeptName());
        info.put("title", u.getTitle());
        info.put("phone", u.getPhone());
        info.put("email", u.getEmail());
        info.put("userStatus", u.getUserStatus());
        info.put("userType", u.getUserType());
        info.put("lastLoginAt", u.getLastLoginAt());
        info.put("createdAt", u.getCreatedAt());

        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("myOps", count("SELECT COUNT(*) FROM mat_operation_log WHERE operator_id = ?", u.getUserId()));
        stats.put("todayOps", count("SELECT COUNT(*) FROM mat_operation_log WHERE operator_id = ? AND DATE(operate_time) = CURDATE()", u.getUserId()));
        stats.put("weekOps", count("SELECT COUNT(*) FROM mat_operation_log WHERE operator_id = ? AND operate_time >= NOW() - INTERVAL 7 DAY", u.getUserId()));
        stats.put("dictItems", count("SELECT COUNT(*) FROM sys_dict_item", null));
        stats.put("configs", count("SELECT COUNT(*) FROM sys_config", null));

        List<Map<String, Object>> todos = new ArrayList<>();
        int noRole = count("SELECT COUNT(*) FROM mat_sys_user u WHERE u.del_flag = 0 AND (u.role_code IS NULL OR u.role_code = '')", null);
        if (noRole > 0) todos.add(todo("未分配角色", noRole + " 名用户尚未分配角色，无法登录子系统", "/users", "warning"));
        int noPhone = count("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0 AND (phone IS NULL OR phone = '')", null);
        if (noPhone > 0) todos.add(todo("联系方式缺失", noPhone + " 名用户未填写手机号", "/users", "info"));
        int failLogin = count("SELECT COUNT(*) FROM sys_login_log WHERE result = 0 AND created_at >= NOW() - INTERVAL 7 DAY", null);
        if (failLogin > 0) todos.add(todo("登录异常", "近 7 天有 " + failLogin + " 次登录失败，建议核查", "/login-logs", "danger"));
        int disabled = count("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0 AND user_status = 0", null);
        if (disabled > 0) todos.add(todo("停用账号", disabled + " 个账号处于停用状态", "/users", "info"));

        List<SysAuditLog> logs = auditMapper.selectList(Wrappers.<SysAuditLog>lambdaQuery()
                .eq(SysAuditLog::getUserId, u.getUserId())
                .orderByDesc(SysAuditLog::getCreatedAt).last("LIMIT 10"));

        List<Map<String, Object>> roles = userMapper.selectList(Wrappers.<User>lambdaQuery()
                        .eq(User::getUserStatus, 1).orderByAsc(User::getUserNo))
                .stream().map(x -> {
                    Map<String, Object> m = new LinkedHashMap<>();
                    m.put("userId", x.getUserId());
                    m.put("userName", x.getUserName());
                    m.put("roleName", roleName(x.getRoleCode()));
                    return m;
                }).toList();

        Map<String, Object> res = new LinkedHashMap<>();
        res.put("info", info);
        res.put("stats", stats);
        res.put("todos", todos);
        res.put("logs", logs);
        res.put("roles", roles);
        return res;
    }

    private Map<String, Object> todo(String type, String desc, String path, String level) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("type", type);
        m.put("desc", desc);
        m.put("path", path);
        m.put("level", level);
        return m;
    }

    private String roleName(String code) {
        if (code == null) return "未分配";
        SysRole r = roleMapper.selectOne(Wrappers.<SysRole>lambdaQuery().eq(SysRole::getRoleCode, code));
        return r == null ? code : r.getRoleName();
    }

    private int count(String sql, Object arg) {
        try {
            Integer v = arg == null ? jdbc.queryForObject(sql, Integer.class) : jdbc.queryForObject(sql, Integer.class, arg);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
}
