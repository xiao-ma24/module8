package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.*;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 明细查询统一放这里，减少文件数量 */
public interface ItemMappers {

    /** 申请明细（联物资） */
    @Select("""
        SELECT pri.*, m.material_code, m.material_name, m.unit, m.is_controlled
        FROM purchase_request_item pri
        JOIN mat_material m ON m.material_id = pri.material_id
        WHERE pri.pr_id = #{prId}""")
    List<PurchaseRequestItem> selectPrItems(@Param("prId") Long prId);

    /** 订单明细（联物资，含参考价与历史均价） */
    @Select("""
        SELECT poi.*, m.material_code, m.material_name, m.unit, m.ref_price, m.is_controlled
        FROM purchase_order_item poi
        JOIN mat_material m ON m.material_id = poi.material_id
        WHERE poi.po_id = #{poId}""")
    List<PurchaseOrderItem> selectPoItems(@Param("poId") Long poId);

    /** 验收明细（联物资） */
    @Select("""
        SELECT ai.*, m.material_code, m.material_name, m.unit, m.is_controlled, poi.unit_price
        FROM acceptance_item ai
        JOIN mat_material m ON m.material_id = ai.material_id
        LEFT JOIN purchase_order_item poi ON poi.poi_id = ai.poi_id
        WHERE ai.acpt_id = #{acptId}""")
    List<AcceptanceItem> selectAcptItems(@Param("acptId") Long acptId);

    /** 退换货跟踪：不合格且处置为退货/换货的验收明细 */
    @Select("""
        SELECT ai.*, a.acpt_no, a.acpt_date, po.po_no, s.supplier_name,
               m.material_code, m.material_name, m.unit, m.is_controlled
        FROM acceptance_item ai
        JOIN acceptance_order a ON a.acpt_id = ai.acpt_id
        JOIN mat_material m ON m.material_id = ai.material_id
        LEFT JOIN purchase_order po ON po.po_id = a.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        WHERE IFNULL(ai.fail_qty, 0) > 0 AND ai.handle_type IN (2, 3)
          AND a.acpt_status IN (2, 3)
        ORDER BY IFNULL(ai.handle_status, 0) ASC, ai.ai_id DESC""")
    List<AcceptanceItem> selectReturns();

    /** 入库明细（联物资） */
    @Select("""
        SELECT ii.*, m.material_code, m.material_name, m.unit, m.is_controlled
        FROM inbound_item ii
        JOIN mat_material m ON m.material_id = ii.material_id
        WHERE ii.in_id = #{inId}""")
    List<InboundItem> selectInItems(@Param("inId") Long inId);

    /** 订单明细在途送检数量：已登记到货但尚未完成验收的数量，用于防止重复登记超发 */
    @Select("""
        SELECT IFNULL(SUM(ai.check_qty), 0)
        FROM acceptance_item ai
        JOIN acceptance_order a ON a.acpt_id = ai.acpt_id AND a.del_flag = 0
        WHERE ai.poi_id = #{poiId} AND a.acpt_status = 1""")
    java.math.BigDecimal selectPendingCheckQty(@Param("poiId") Long poiId);
}
