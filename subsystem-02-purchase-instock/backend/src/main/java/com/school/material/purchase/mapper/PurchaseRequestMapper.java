package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.PurchaseRequest;
import org.apache.ibatis.annotations.*;

public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {

    @Select("""
        SELECT pr.*, u.user_name AS applicant_name, d.dept_name,
               b.subject_name AS budget_subject_name
        FROM purchase_request pr
        LEFT JOIN mat_sys_user u ON u.user_id = pr.applicant_id
        LEFT JOIN mat_dept d ON d.org_id = pr.dept_id
        LEFT JOIN budget b ON b.budget_no = pr.budget_no
        WHERE pr.del_flag = 0
        ORDER BY pr.pr_id DESC""")
    java.util.List<PurchaseRequest> selectListWithNames();

    @Select("""
        SELECT pr.*, u.user_name AS applicant_name, d.dept_name,
               b.subject_name AS budget_subject_name
        FROM purchase_request pr
        LEFT JOIN mat_sys_user u ON u.user_id = pr.applicant_id
        LEFT JOIN mat_dept d ON d.org_id = pr.dept_id
        LEFT JOIN budget b ON b.budget_no = pr.budget_no
        WHERE pr.pr_id = #{id}""")
    PurchaseRequest selectDetail(@Param("id") Long id);
}
