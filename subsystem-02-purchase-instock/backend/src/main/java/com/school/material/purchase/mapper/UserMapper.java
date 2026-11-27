package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.User;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface UserMapper extends BaseMapper<User> {

    @Select("""
        SELECT u.*, u.user_status AS user_status, d.dept_name FROM mat_sys_user u
        LEFT JOIN mat_dept d ON d.org_id = u.org_id
        WHERE u.del_flag = 0 ORDER BY u.user_id""")
    List<User> selectListWithDept();

    /** 是否具备库管员（STORE_KEEPER）角色：主角色或多角色授权均认可（文档 warehouse.keeper_id 约束） */
    @Select("""
        SELECT COUNT(*) FROM mat_sys_user u
        WHERE u.user_id = #{userId} AND u.del_flag = 0
          AND (u.role_code = 'STORE_KEEPER'
               OR EXISTS (SELECT 1 FROM mat_sys_user_role ur
                          JOIN mat_sys_role r ON r.role_id = ur.role_id
                          WHERE ur.user_id = u.user_id AND r.role_code = 'STORE_KEEPER'))""")
    int countStoreKeeper(Long userId);

    /** 用户的主角色与多角色授权编码（用于岗位权限校验） */
    @Select("""
        SELECT u.role_code FROM mat_sys_user u
        WHERE u.user_id = #{userId} AND u.del_flag = 0
        UNION
        SELECT r.role_code FROM mat_sys_user_role ur
        JOIN mat_sys_role r ON r.role_id = ur.role_id
        WHERE ur.user_id = #{userId}""")
    List<String> selectRoleCodes(Long userId);

    /** 取用户归属部门（org_id），用于申请人未传部门时兜底 */
    @Select("SELECT org_id FROM mat_sys_user WHERE user_id = #{userId} AND del_flag = 0")
    String selectDeptId(Long userId);
}
