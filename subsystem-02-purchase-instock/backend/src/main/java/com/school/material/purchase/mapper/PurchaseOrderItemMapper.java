package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.PurchaseOrderItem;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface PurchaseOrderItemMapper extends BaseMapper<PurchaseOrderItem> {

    /** 仍有可到货数量的订单：订单明细中订购量 > 已到货量 */
    @Select("""
        SELECT po_id FROM purchase_order_item
        WHERE del_flag = 0
        GROUP BY po_id
        HAVING SUM(order_qty - IFNULL(arrived_qty, 0)) > 0""")
    List<Long> selectReceivablePoIds();
}
