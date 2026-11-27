package com.school.material.sys.service;

import com.school.material.common.PageResult;
import com.school.material.sys.dto.AssignRoleReq;
import com.school.material.sys.dto.UserRegisterReq;
import com.school.material.sys.dto.UserReq;
import com.school.material.sys.dto.UserVO;

public interface SysUserService {

    /**
     * 分页查询用户，返回部门、角色、状态信息。
     */
    PageResult<UserVO> page(Integer pageNum, Integer pageSize, String keyword, String deptId, Integer status);

    UserVO getById(Long id);

    /**
     * 新教师建档：默认密码 123456，状态置为待启用（0）。
     */
    UserVO register(UserRegisterReq req);

    Long create(UserReq req);

    void update(Long id, UserReq req);

    void delete(Long id);

    /**
     * 分配角色：覆盖原有角色，成功后用户状态置为启用（1）。
     */
    UserVO assignRoles(AssignRoleReq req);
}
