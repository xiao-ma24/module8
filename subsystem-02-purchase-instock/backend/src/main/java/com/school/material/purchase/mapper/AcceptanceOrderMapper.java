package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.AcceptanceOrder;
import org.apache.ibatis.annotations.*;

public interface AcceptanceOrderMapper extends BaseMapper<AcceptanceOrder> {

    @Select("""
        SELECT a.*, po.po_no, u.user_name AS inspector_name, s.supplier_name
        FROM acceptance_order a
        LEFT JOIN purchase_order po ON po.po_id = a.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN mat_sys_user u ON u.user_id = a.inspector_id
        WHERE a.del_flag = 0
        ORDER BY a.acpt_id DESC""")
    java.util.List<AcceptanceOrder> selectListWithNames();

    @Select("""
        SELECT a.*, po.po_no, u.user_name AS inspector_name, s.supplier_name
        FROM acceptance_order a
        LEFT JOIN purchase_order po ON po.po_id = a.po_id
        LEFT JOIN mat_supplier s ON s.supplier_id = po.supplier_id
        LEFT JOIN mat_sys_user u ON u.user_id = a.inspector_id
        WHERE a.acpt_id = #{id}""")
    AcceptanceOrder selectDetail(@Param("id") Long id);
}
