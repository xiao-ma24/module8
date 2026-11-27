package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.InboundOrder;
import org.apache.ibatis.annotations.*;

public interface InboundOrderMapper extends BaseMapper<InboundOrder> {

    @Select("""
        SELECT i.*, po.po_no, a.acpt_no, w.warehouse_name, w.warehouse_type,
               u.user_name AS keeper_name, hu.user_name AS holder_name,
               s.supplier_name, po.supplier_id
        FROM inbound_order i
        LEFT JOIN purchase_order po ON po.po_id = i.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN acceptance_order a ON a.acpt_id = i.acpt_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = i.warehouse_id
        LEFT JOIN mat_sys_user u ON u.user_id = i.keeper_id
        LEFT JOIN mat_sys_user hu ON hu.user_id = i.holder_id
        WHERE i.del_flag = 0
        ORDER BY i.in_id DESC""")
    java.util.List<InboundOrder> selectListWithNames();

    @Select("""
        SELECT i.*, po.po_no, a.acpt_no, w.warehouse_name, w.warehouse_type,
               u.user_name AS keeper_name, hu.user_name AS holder_name,
               s.supplier_name, po.supplier_id
        FROM inbound_order i
        LEFT JOIN purchase_order po ON po.po_id = i.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN acceptance_order a ON a.acpt_id = i.acpt_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = i.warehouse_id
        LEFT JOIN mat_sys_user u ON u.user_id = i.keeper_id
        LEFT JOIN mat_sys_user hu ON hu.user_id = i.holder_id
        WHERE i.in_id = #{id}""")
    InboundOrder selectDetail(@Param("id") Long id);
}
