package com.school.material.requisition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.requisition.entity.ReqOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 领用单 Mapper：列表带物资/申请人/审批信息联查，按角色数据范围过滤在 Service 层组装条件。 */
@Mapper
public interface ReqOrderMapper extends BaseMapper<ReqOrder> {

    @Select("""
        <script>
        SELECT r.req_id, r.req_no, r.applicant_id, r.org_id, r.material_id, r.qty_apply, r.qty_approve,
               r.amount, r.req_status, r.created_at, r.updated_at,
               u.user_name AS applicant_name, u.person_no AS applicant_no, d.dept_name,
               m.material_name, m.material_code, m.unit, m.ref_price, m.material_type, m.is_controlled,
               (SELECT IFNULL(SUM(s.qty_available), 0) FROM mat_stock s
                  JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
                 WHERE s.material_id = r.material_id AND s.del_flag = 0 AND w.warehouse_type = 1
                   AND (s.expire_date IS NULL OR s.expire_date &gt;= CURDATE())) AS available_qty,
               o.out_no
        FROM mat_req_order r
        LEFT JOIN mat_sys_user u ON u.user_id = r.applicant_id
        LEFT JOIN mat_dept d ON d.org_id = r.org_id
        LEFT JOIN mat_material m ON m.material_id = r.material_id
        LEFT JOIN mat_out_order o ON o.source_type = 1 AND o.source_id = r.req_id AND o.del_flag = 0
        WHERE r.del_flag = 0
        <if test="applicantId != null"> AND r.applicant_id = #{applicantId}</if>
        <if test="deptId != null"> AND r.org_id = #{deptId}</if>
        <if test="status != null"> AND r.req_status = #{status}</if>
        <if test="statuses != null and statuses.size() &gt; 0">
            AND r.req_status IN
            <foreach collection="statuses" item="st" open="(" separator="," close=")">#{st}</foreach>
        </if>
        <if test="keyword != null and keyword != ''">
            AND (r.req_no LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.user_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY r.req_id DESC
        </script>""")
    List<ReqOrder> selectOrders(@Param("applicantId") Long applicantId,
                                @Param("deptId") String deptId,
                                @Param("status") Integer status,
                                @Param("statuses") List<Integer> statuses,
                                @Param("keyword") String keyword);

    /** 单条详情（含物资、申请人、部门、最近一次审批意见） */
    @Select("""
        SELECT r.req_id, r.req_no, r.applicant_id, r.org_id, r.material_id, r.qty_apply, r.qty_approve,
               r.amount, r.req_status, r.created_at, r.updated_at,
               u.user_name AS applicant_name, u.person_no AS applicant_no, d.dept_name,
               m.material_name, m.material_code, m.unit, m.ref_price, m.material_type, m.is_controlled,
               (SELECT IFNULL(SUM(s.qty_available), 0) FROM mat_stock s
                  JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
                 WHERE s.material_id = r.material_id AND s.del_flag = 0 AND w.warehouse_type = 1
                   AND (s.expire_date IS NULL OR s.expire_date >= CURDATE())) AS available_qty,
               o.out_no
        FROM mat_req_order r
        LEFT JOIN mat_sys_user u ON u.user_id = r.applicant_id
        LEFT JOIN mat_dept d ON d.org_id = r.org_id
        LEFT JOIN mat_material m ON m.material_id = r.material_id
        LEFT JOIN mat_out_order o ON o.source_type = 1 AND o.source_id = r.req_id AND o.del_flag = 0
        WHERE r.req_id = #{reqId} AND r.del_flag = 0""")
    ReqOrder selectDetail(@Param("reqId") Long reqId);

    /** 今日同前缀单据数，用于生成 LY+yyyyMMdd+4位流水 */
    @Select("SELECT COUNT(*) FROM mat_req_order WHERE req_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);
}
