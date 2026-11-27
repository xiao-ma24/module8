package com.school.material.borrow.auth;

import lombok.Data;

/** 登录态用户（演示级，存于内存 TokenStore） */
@Data
public class LoginUser {
    private Long userId;
    private String userNo;
    private String userName;
    private String roleCode;
    private String roleName;
    private String deptId;
    private String deptName;
    private long expireAt;

    /** 是否具备指定角色（超级管理员视为具备全部角色） */
    public boolean hasRole(String... roles) {
        if (roleCode == null) return false;
        for (String r : roles) {
            if (r.equalsIgnoreCase(roleCode)) return true;
            if ("ADMIN".equalsIgnoreCase(roleCode)) return true;
        }
        return false;
    }

    /** 是否为只读管理员（平台管理端维护者，不参与业务动作） */
    public boolean isAdmin() {
        return "ADMIN".equalsIgnoreCase(roleCode);
    }
}
