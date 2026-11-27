package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 用户表（全平台共用主数据，本端唯一可写入口） */
@Data
@TableName("mat_sys_user")
public class User {
    @TableId(type = IdType.AUTO)
    private Long userId;
    @TableField("person_no")
    private String userNo;
    private String userName;
    @TableField("org_id")
    private String deptId;
    private String roleCode;
    @TableField("user_status")
    private Integer userStatus;
    private Integer userType;      // 1教职工 2学生 3外部人员
    private String title;
    private String phone;
    private String email;
    private LocalDate hireDate;
    @JsonIgnore
    @TableField("password")
    private String pwdHash;
    private Integer pwdNeedChange;   // 1 首次登录需改密
    private LocalDateTime pwdUpdatedAt;
    private String sourceType;       // LOCAL 补录 / TEACHER 师资 / STUDENT 学籍
    private LocalDateTime sourceSyncAt;
    private LocalDateTime lastLoginAt;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String roleName;
    @TableField(exist = false)
    private String roleNames;                 // 多个角色名，逗号分隔
    @TableField(exist = false)
    private List<String> roleCodes;           // 提交/回显多个角色
}
