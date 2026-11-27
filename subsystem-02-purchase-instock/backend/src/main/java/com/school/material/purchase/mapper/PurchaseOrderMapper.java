package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.PurchaseOrder;
import org.apache.ibatis.annotations.*;

public interface PurchaseOrderMapper extends BaseMapper<PurchaseOrder> {

    @Select("""
        SELECT po.*, s.supplier_name, s.star_level AS supplier_star, pr.pr_no,
               pl.plan_no, bu.user_name AS buyer_name, m.material_name,
               IFNULL((SELECT SUM(poi.order_qty - IFNULL(poi.arrived_qty, 0))
                         FROM purchase_order_item poi
                        WHERE poi.po_id = po.po_id AND poi.del_flag = 0), 0) AS remain_qty
        FROM purchase_order po
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN purchase_request pr ON pr.pr_id = po.pr_id
        LEFT JOIN mat_purchase_plan pl ON pl.plan_id = po.plan_id
        LEFT JOIN mat_sys_user bu ON bu.user_id = po.buyer_id
        LEFT JOIN mat_material m ON m.material_id = po.material_id
        WHERE po.del_flag = 0
        ORDER BY po.po_id DESC""")
    java.util.List<PurchaseOrder> selectListWithNames();

    @Select("""
        SELECT po.*, s.supplier_name, s.star_level AS supplier_star, pr.pr_no,
               pl.plan_no, bu.user_name AS buyer_name, m.material_name
        FROM purchase_order po
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN purchase_request pr ON pr.pr_id = po.pr_id
        LEFT JOIN mat_purchase_plan pl ON pl.plan_id = po.plan_id
        LEFT JOIN mat_sys_user bu ON bu.user_id = po.buyer_id
        LEFT JOIN mat_material m ON m.material_id = po.material_id
        WHERE po.po_id = #{id}""")
    PurchaseOrder selectDetail(@Param("id") Long id);

    /** 历史成交均价（比价）：该物资所有已完成订单的平均单价 */
    @Select("""
        SELECT IFNULL(AVG(poi.unit_price), 0) FROM purchase_order_item poi
        JOIN purchase_order po ON po.po_id = poi.po_id AND po.po_status IN (1,2,3)
        WHERE poi.material_id = #{materialId}""")
    java.math.BigDecimal historyAvgPrice(@Param("materialId") Long materialId);

    /** 供应商评级更新：验收评分与现有星级加权（6:4） */
    @Update("""
        UPDATE mat_supplier SET star_level = ROUND(0.6 * #{score} + 0.4 * star_level)
        WHERE supplier_id = #{supplierId}""")
    int refreshStar(@Param("supplierId") Long supplierId, @Param("score") Integer score);
}
