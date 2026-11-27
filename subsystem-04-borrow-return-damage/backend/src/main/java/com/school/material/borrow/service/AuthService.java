package com.school.material.borrow.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.borrow.auth.LoginUser;
import com.school.material.borrow.auth.TokenStore;
import com.school.material.borrow.common.BizException;
import com.school.material.borrow.common.HashUtil;
import com.school.material.borrow.entity.Dept;
import com.school.material.borrow.entity.SysUser;
import com.school.material.borrow.mapper.DeptMapper;
import com.school.material.borrow.mapper.SysUserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 登录认证（演示级）：校验 sys_user 口令（与平台管理端同一加盐摘要口径），
 * 签发内存令牌，并把角色与部门写入登录态，供接口层做角色校验与数据范围过滤。
 */
@Service
@RequiredArgsConstructor
public class AuthService {

    private final SysUserMapper userMapper;
    private final DeptMapper deptMapper;
    private final TokenStore tokenStore;

    public Map<String, Object> login(String userNo, String password) {
        if (userNo == null || userNo.isBlank() || password == null || password.isBlank())
            throw new BizException(4005, "请输入工号与密码");
        SysUser u = userMapper.selectOne(Wrappers.<SysUser>lambdaQuery().eq(SysUser::getUserNo, userNo.trim()));
        if (u == null) throw new BizException(4006, "工号或密码错误");
        if (u.getStatus() == null || u.getStatus() != 1) throw new BizException(4007, "账号已停用，请联系管理员");
        if (!HashUtil.matches(password, u.getPassword())) throw new BizException(4006, "工号或密码错误");
        return issueFor(u);
    }

    /** 统一登录后按用户 ID 换发本子系统令牌（供合并系统一次登录、多模块互通） */
    public Map<String, Object> handoff(Long userId) {
        if (userId == null) throw new BizException(4005, "缺少用户标识");
        SysUser u = userMapper.selectById(userId);
        if (u == null) throw new BizException(4006, "用户不存在");
        if (u.getStatus() == null || u.getStatus() != 1) throw new BizException(4007, "账号已停用，请联系管理员");
        return issueFor(u);
    }

    private Map<String, Object> issueFor(SysUser u) {
        LoginUser lu = new LoginUser();
        lu.setUserId(u.getUserId());
        lu.setUserNo(u.getUserNo());
        lu.setUserName(u.getUserName());
        lu.setRoleCode(u.getRoleCode());
        lu.setRoleName(roleName(u.getRoleCode()));
        lu.setDeptId(u.getDeptId());
        if (u.getDeptId() != null) {
            Dept d = deptMapper.selectById(u.getDeptId());
            if (d != null) lu.setDeptName(d.getDeptName());
        }
        String token = tokenStore.issue(lu);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("token", token);
        data.put("user", lu);
        return data;
    }

    public void logout(String token) {
        tokenStore.revoke(token);
    }

    public static String roleName(String code) {
        if (code == null) return "未知角色";
        return switch (code) {
            case "ADMIN" -> "系统管理员";
            case "DEPT_HEAD" -> "部门负责人";
            case "PURCHASER" -> "采购员";
            case "STORE_KEEPER" -> "仓库管理员";
            case "APPLICANT" -> "教职工/申请人";
            case "STUDENT" -> "学生";
            case "CHECKER" -> "验收人";
            case "STOCKTAKER" -> "盘点员";
            default -> code;
        };
    }
}
