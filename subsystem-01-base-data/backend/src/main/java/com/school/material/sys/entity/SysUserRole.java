package com.school.material.sys.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 用户角色关联（团队 sys_user_role 表，联合主键 user_id + role_id，无自增 id）
 */
@Data
@TableName("mat_sys_user_role")
public class SysUserRole {

    private Long userId;

    private Long roleId;

    /** 授权人 user_id；系统/无登录态记 0 */
    private Long grantedBy;

    private LocalDateTime grantedAt;
}
