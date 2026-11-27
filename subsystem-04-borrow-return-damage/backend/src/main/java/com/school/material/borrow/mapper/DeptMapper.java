package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.Dept;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

/** 部门 Mapper：只写 used_amount（领用出库累加），其余字段由平台管理端维护。 */
@Mapper
public interface DeptMapper extends BaseMapper<Dept> {

    /** 出库时累加部门已用额度（带额度前置条件，超过年度额度则更新 0 行） */
    @Update("""
        UPDATE mat_dept SET used_amount = used_amount + #{amount}
        WHERE org_id = #{deptId} AND del_flag = 0
          AND used_amount + #{amount} <= annual_quota""")
    int addUsedAmount(@Param("deptId") String deptId, @Param("amount") java.math.BigDecimal amount);

    /** 驳回/关闭时回退已用额度 */
    @Update("""
        UPDATE mat_dept SET used_amount = GREATEST(used_amount - #{amount}, 0)
        WHERE org_id = #{deptId} AND del_flag = 0""")
    int subUsedAmount(@Param("deptId") String deptId, @Param("amount") java.math.BigDecimal amount);
}
