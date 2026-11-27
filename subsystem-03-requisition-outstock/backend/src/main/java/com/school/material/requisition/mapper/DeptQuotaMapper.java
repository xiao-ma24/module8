package com.school.material.requisition.mapper;

import com.school.material.requisition.entity.DeptQuota;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;

/**
 * 部门月度领用额度 Mapper（material_dept_quota，报告表2-4）。
 * 额度由平台管理端核定，本子系统只在出库时累加 used_amount。
 */
@Mapper
public interface DeptQuotaMapper {

    /** 取某部门某年月的额度行；不存在返回 null（表示本月未核定额度） */
    @Select("""
        SELECT q.dept_id, q.quota_period, q.month_quota, q.used_amount, q.approve_by, q.update_time,
               d.dept_name
        FROM material_dept_quota q
        LEFT JOIN mat_dept d ON d.org_id = q.dept_id
        WHERE q.dept_id = #{deptId} AND q.quota_period = #{period}""")
    DeptQuota selectOne(@Param("deptId") String deptId, @Param("period") String period);

    /**
     * 出库时累加本月已用额度：带额度前置条件，超出月度额度则更新 0 行（由调用方回滚整单）。
     */
    @Update("""
        UPDATE material_dept_quota
        SET used_amount = used_amount + #{amount}
        WHERE dept_id = #{deptId} AND quota_period = #{period}
          AND used_amount + #{amount} <= month_quota""")
    int addUsedAmount(@Param("deptId") String deptId,
                      @Param("period") String period,
                      @Param("amount") BigDecimal amount);

    /** 单据关闭或核销回退时冲减已用额度 */
    @Update("""
        UPDATE material_dept_quota
        SET used_amount = GREATEST(used_amount - #{amount}, 0)
        WHERE dept_id = #{deptId} AND quota_period = #{period}""")
    int subUsedAmount(@Param("deptId") String deptId,
                      @Param("period") String period,
                      @Param("amount") BigDecimal amount);
}
