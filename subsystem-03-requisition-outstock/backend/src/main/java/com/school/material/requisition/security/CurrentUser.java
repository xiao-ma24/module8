package com.school.material.requisition.security;

/**
 * 统一身份载体（DBA 集成契约）。
 *
 * @param userId     用户主键，对应 sys_user.user_id
 * @param role       角色编码，见 docs/delivery/身份权限矩阵.md
 * @param businessId 业务归属标识（部门/仓库等），可为空
 */
public record CurrentUser(String userId, String role, String businessId) {
}
