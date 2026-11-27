package com.school.material.sys.service;

import com.school.material.sys.entity.SysRole;

import java.util.List;

public interface SysRoleService {

    /**
     * 角色列表，可按状态筛选。
     */
    List<SysRole> list(Integer status);
}
