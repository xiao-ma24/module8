package com.school.material.admin.auth;

import lombok.Data;

import java.util.List;

/** 登录态用户（演示用，存于内存 TokenStore） */
@Data
public class LoginUser {
    private Long userId;
    private String userNo;
    private String userName;
    private String roleCode;
    private String roleName;
    private List<String> roles;   // 全部角色编码（一人可多角色）
    private String deptId;
    private String deptName;
    private long expireAt;
}
