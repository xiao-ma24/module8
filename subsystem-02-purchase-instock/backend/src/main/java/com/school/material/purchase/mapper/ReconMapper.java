package com.school.material.purchase.mapper;

import com.school.material.purchase.dto.ReconRow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

/** 财务核对：读取入库成本接口表（material_inbound_interface），按实际入库单价与金额核算 */
public interface ReconMapper {

    @Select("""
        <script>
        SELECT mii.in_no AS inNo, mii.in_date AS inDate, po.po_no AS poNo,
               s.supplier_id AS supplierId, s.supplier_name AS supplierName,
               m.material_id AS materialId, m.material_code AS materialCode,
               m.material_name AS materialName, m.unit AS unit,
               mii.in_qty AS qty, mii.in_price AS inPrice, mii.in_amount AS inAmount,
               w.warehouse_name AS warehouseName, u.user_name AS operatorName
        FROM material_inbound_interface mii
        JOIN mat_material m ON m.material_id = mii.material_id
        JOIN inbound_order io ON io.in_no = mii.in_no AND io.del_flag = 0
        LEFT JOIN purchase_order po ON po.po_id = io.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = mii.warehouse_id
        LEFT JOIN mat_sys_user u ON u.user_id = io.keeper_id
        WHERE mii.del_flag = 0
        <if test="supplierId != null"> AND po.supplier_id = #{supplierId} </if>
        <if test="materialId != null"> AND mii.material_id = #{materialId} </if>
        <if test="startDate != null"> AND mii.in_date &gt;= #{startDate} </if>
        <if test="endDate != null"> AND mii.in_date &lt;= #{endDate} </if>
        ORDER BY mii.in_date DESC, mii.in_no DESC, mii.material_id
        </script>""")
    List<ReconRow> selectRecon(@Param("supplierId") Long supplierId,
                               @Param("materialId") Long materialId,
                               @Param("startDate") LocalDate startDate,
                               @Param("endDate") LocalDate endDate);
}
