package com.school.material.requisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import com.baomidou.mybatisplus.annotation.TableField;

/** 平台用户（sys_user，只读）：领用申请人、审批人、库管员均通过 user_id 关联本表。 */
@Data
@TableName("mat_sys_user")
public class SysUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long userId;
    @TableField("person_no")
    private String userNo;
    private String userName;
    private Integer userType;
    @TableField("org_id")
    private String deptId;
    private String roleCode;
    private String title;
    private String phone;
    private String email;
    private LocalDate hireDate;
    private String password;
    @TableField("user_status")
    private Integer status;
    private LocalDateTime lastLoginAt;
    @TableLogic
    private Integer delFlag;

    /** 部门年度领用额度（关联查询用，不落库） */
    @com.baomidou.mybatisplus.annotation.TableField(exist = false)
    private BigDecimal annualQuota;
}
