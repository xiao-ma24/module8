package com.school.material.stocktake.mapper;

import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

/**
 * 采购计划（purchase_plan，报告表2-9）：补货计划统一写入采购模块已建的表，
 * 不另建 replenish_plan。⑤ 只写 plan_type = 1（常规补货）的行，plan_status 由本模块推进。
 */
public interface PurchasePlanMapper {

    @Select("SELECT MAX(plan_no) FROM mat_purchase_plan WHERE plan_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxNoByPrefix(@Param("prefix") String prefix);

    @Insert("""
        INSERT INTO mat_purchase_plan (plan_no, plan_type, purchase_mode, org_id, material_id,
                                   plan_qty, stock_qty, supplier_id, expect_date, plan_status,
                                   created_by, created_at)
        VALUES (#{planNo}, 1, 1, #{deptId}, #{materialId}, #{planQty}, #{stockQty},
                #{supplierId}, #{expectDate}, #{planStatus}, #{operatorId}, NOW())""")
    int insertPlan(@Param("planNo") String planNo,
                   @Param("deptId") String deptId,
                   @Param("materialId") Long materialId,
                   @Param("planQty") BigDecimal planQty,
                   @Param("stockQty") BigDecimal stockQty,
                   @Param("supplierId") Long supplierId,
                   @Param("expectDate") LocalDate expectDate,
                   @Param("planStatus") Integer planStatus,
                   @Param("operatorId") Long operatorId);

    @Select("""
        <script>
        SELECT p.plan_id AS planId, p.plan_no AS planNo, p.plan_type AS planType,
               p.material_id AS materialId, p.plan_qty AS planQty, p.stock_qty AS stockQty,
               p.supplier_id AS supplierId, p.expect_date AS expectDate, p.plan_status AS planStatus,
               p.org_id AS deptId, p.created_at AS createdAt,
               m.material_name AS materialName, m.material_code AS materialCode,
               m.unit AS unit, m.ref_price AS refPrice,
               s.supplier_name AS supplierName
        FROM mat_purchase_plan p
        JOIN mat_material m ON m.material_id = p.material_id
        LEFT JOIN mat_supplier s ON s.supplier_id = p.supplier_id
        WHERE p.del_flag = 0 AND p.plan_type = 1
        <if test="status != null"> AND p.plan_status = #{status}</if>
        ORDER BY p.plan_id DESC
        </script>""")
    List<Map<String, Object>> selectReplenishPlans(@Param("status") Integer status);

    /** 条件更新：只有期望状态才允许流转，避免重复审批 */
    @Update("""
        UPDATE mat_purchase_plan SET plan_status = #{status}, approve_id = #{approveId},
               updated_by = #{operatorId}, updated_at = NOW()
        WHERE plan_id = #{planId} AND plan_status = #{expectStatus} AND del_flag = 0""")
    int updateStatus(@Param("planId") Long planId,
                     @Param("status") Integer status,
                     @Param("expectStatus") Integer expectStatus,
                     @Param("approveId") Long approveId,
                     @Param("operatorId") Long operatorId);

    @Select("SELECT * FROM mat_purchase_plan WHERE plan_id = #{planId} AND del_flag = 0")
    Map<String, Object> selectOne(@Param("planId") Long planId);
}
