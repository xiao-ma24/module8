package com.school.material.admin.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.admin.entity.DeptQuota;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface DeptQuotaMapper extends BaseMapper<DeptQuota> {

    /** 按年月列出全部启用部门；额度来自财务预算表 budget（年度额度 ÷ 12），只读 */
    @Select("""
        <script>
        SELECT d.org_id AS dept_id, #{period} AS quota_period,
               IFNULL(q.month_quota, 0.00) AS month_quota,
               IFNULL(q.used_amount, 0.00) AS used_amount,
               q.approve_by, q.update_time,
               b.balance AS annual_budget,
               d.dept_name, d.dept_code, d.sort_no AS dept_sort_no,
               u.user_name AS approve_name
        FROM mat_dept d
        LEFT JOIN budget b ON b.dept_id = d.org_id AND b.del_flag = 0
        LEFT JOIN material_dept_quota q ON q.dept_id = d.org_id AND q.quota_period = #{period}
        LEFT JOIN mat_sys_user u ON u.user_id = q.approve_by
        WHERE d.del_flag = 0
        <if test="keyword != null and keyword != ''">
          AND (d.dept_name LIKE CONCAT('%', #{keyword}, '%') OR d.dept_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY d.sort_no, d.org_id
        </script>""")
    List<DeptQuota> selectWithDept(@Param("period") String period, @Param("keyword") String keyword);

    /** 用财务预算表中的部门年度额度刷新月度额度（年度 ÷ 12），已用额度保持不变 */
    @Insert("""
        INSERT INTO material_dept_quota (dept_id, quota_period, month_quota, used_amount, update_time)
        SELECT b.dept_id, #{period}, ROUND(b.balance / 12, 2), 0, NOW()
        FROM budget b
        JOIN mat_dept d ON d.org_id = b.dept_id AND d.del_flag = 0
        WHERE b.dept_id IS NOT NULL AND b.del_flag = 0
        ON DUPLICATE KEY UPDATE
            month_quota = VALUES(month_quota),
            update_time = NOW()""")
    int syncFromBudget(@Param("period") String period);
}
