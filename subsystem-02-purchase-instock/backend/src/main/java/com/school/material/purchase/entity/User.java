package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 用户表（sys_user）— 韦森宁负责，本系统只读使用（申请人/审批人/验收人/库管员） */
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
    private String roleCode;   // APPLICANT / DEPT_HEAD / STORE_KEEPER / PURCHASER / ADMIN
    @TableField("user_status")
    private Integer userStatus;
    private LocalDateTime createdAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String deptName;
}
