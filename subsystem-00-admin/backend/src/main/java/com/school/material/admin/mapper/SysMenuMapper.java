package com.school.material.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.admin.entity.SysMenu;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface SysMenuMapper extends BaseMapper<SysMenu> {

    /** 当前用户可访问的菜单（按角色关联） */
    @Select("""
        SELECT DISTINCT m.* FROM sys_menu m
        JOIN sys_role_menu rm ON rm.menu_id = m.menu_id
        JOIN mat_sys_role r ON r.role_id = rm.role_id
        JOIN mat_sys_user_role ur ON ur.role_id = r.role_id
        WHERE ur.user_id = #{userId} AND m.menu_status = 1 AND r.role_status = 1
        ORDER BY m.sort_no""")
    List<SysMenu> selectByUser(@Param("userId") Long userId);

    @Select("SELECT menu_id FROM sys_role_menu WHERE role_id = #{roleId}")
    List<Long> selectMenuIdsByRole(@Param("roleId") Long roleId);
}
