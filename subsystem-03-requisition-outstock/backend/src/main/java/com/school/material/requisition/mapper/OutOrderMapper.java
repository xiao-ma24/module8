package com.school.material.requisition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.requisition.entity.OutOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 出库单 Mapper */
@Mapper
public interface OutOrderMapper extends BaseMapper<OutOrder> {

    @Select("""
        <script>
        SELECT o.out_id, o.out_no, o.source_type, o.source_id, o.warehouse_id, o.keeper_id, o.holder_id,
               o.material_id, o.batch_no, o.out_qty, o.out_time,
               m.material_name, m.material_code, m.unit,
               w.warehouse_name, k.user_name AS keeper_name,
               r.req_no, r.req_status, u.user_name AS applicant_name, d.dept_name
        FROM mat_out_order o
        LEFT JOIN mat_material m ON m.material_id = o.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = o.warehouse_id
        LEFT JOIN mat_sys_user k ON k.user_id = o.keeper_id
        LEFT JOIN mat_req_order r ON r.req_id = o.source_id AND o.source_type = 1
        LEFT JOIN mat_sys_user u ON u.user_id = r.applicant_id
        LEFT JOIN mat_dept d ON d.org_id = r.org_id
        WHERE o.del_flag = 0 AND o.source_type = 1
        <if test="applicantId != null"> AND r.applicant_id = #{applicantId}</if>
        <if test="deptId != null"> AND r.org_id = #{deptId}</if>
        <if test="keyword != null and keyword != ''">
            AND (o.out_no LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR r.req_no LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY o.out_id DESC
        </script>""")
    List<OutOrder> selectOutOrders(@Param("applicantId") Long applicantId,
                                   @Param("deptId") String deptId,
                                   @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM mat_out_order WHERE out_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);

    @Select("SELECT COUNT(*) FROM mat_out_order WHERE source_type = 1 AND source_id = #{reqId} AND del_flag = 0")
    long countByReq(@Param("reqId") Long reqId);
}
