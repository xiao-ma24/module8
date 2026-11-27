package com.school.material.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台用户（团队 sys_user 表，教师 / 学生 / 管理员统一账号）
 */
@Data
@TableName("mat_sys_user")
public class SysUser {

    @TableId(value = "user_id", type = IdType.AUTO)
    private Long id;

    /** 工号 / 登录账号（唯一） */
    @TableField("person_no")
    private String userNo;

    /** 姓名 */
    private String userName;

    /** 密码，建档默认 123456 */
    @JsonIgnore
    private String password;

    /** 用户类型：1 教师 / 2 学生 / 3 管理员 */
    private Integer userType;

    /** 部门 id */
    @TableField("org_id")
    private String deptId;

    private String phone;

    /** 状态：0 停用 / 1 启用 */
    @TableField("user_status")
    private Integer status;

    /** 主角色编码，对应 sys_role.role_code */
    private String roleCode;

    private String remark;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    @JsonIgnore
    @TableField("del_flag")
    @TableLogic(value = "0", delval = "1")
    private Integer deleted;
}
