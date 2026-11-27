package com.school.material.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 角色（团队 sys_role 表）
 */
@Data
@TableName("mat_sys_role")
public class SysRole {

    @TableId(value = "role_id", type = IdType.AUTO)
    private Long id;

    /** 角色编码（唯一），与 sys_user.role_code 对应 */
    @TableField("role_code")
    private String roleNo;

    /** 角色名称 */
    private String roleName;

    /** 1 启用 / 0 停用 */
    @TableField("role_status")
    private Integer status;

    /** 排序号 */
    @TableField("sort_no")
    private Integer sortNo;
}
