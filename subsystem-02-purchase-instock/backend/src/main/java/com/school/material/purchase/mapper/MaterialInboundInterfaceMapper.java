package com.school.material.purchase.mapper;

import com.school.material.purchase.entity.MaterialInboundInterface;
import org.apache.ibatis.annotations.Insert;

/**
 * 入库成本接口写入：联合主键 (in_no, material_id)。
 * 确认入库与库存、流水同事务提交；重复执行时按同一入库单+物资累加，不产生重复成本行。
 */
public interface MaterialInboundInterfaceMapper {

    @Insert("""
        INSERT INTO material_inbound_interface
            (in_no, material_id, in_price, in_qty, in_amount, in_date, warehouse_id)
        VALUES
            (#{inNo}, #{materialId}, #{inPrice}, #{inQty}, #{inAmount}, #{inDate}, #{warehouseId})
        ON DUPLICATE KEY UPDATE
            in_price = #{inPrice},
            in_qty = in_qty + #{inQty},
            in_amount = in_amount + #{inAmount},
            in_date = #{inDate},
            warehouse_id = #{warehouseId},
            updated_at = CURRENT_TIMESTAMP""")
    int upsert(MaterialInboundInterface row);
}
