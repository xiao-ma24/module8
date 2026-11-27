package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/** 角色表 */
@Data
@TableName("mat_sys_role")
public class SysRole {
    @TableId(type = IdType.AUTO)
    private Long roleId;
    private String roleCode;
    private String roleName;
    /** 数据范围：全校 / 部门 / 本人（数据字典 data_scope varchar(30)） */
    private String dataScope;
    @TableField("role_status")
    private Integer roleStatus;
    private Integer sortNo;
    private String remark;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @TableField(exist = false)
    private Integer userCount;
    @TableField(exist = false)
    private List<Long> menuIds;
}
