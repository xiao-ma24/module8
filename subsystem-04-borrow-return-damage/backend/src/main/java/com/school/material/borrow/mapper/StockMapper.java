package com.school.material.borrow.mapper;

import com.school.material.borrow.entity.Stock;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存只读 Mapper。库存写操作一律通过子系统② InventoryService（HTTP），本类禁止任何写 SQL。
 */
@Mapper
public interface StockMapper {

    /** 某物资的公共可领总量（实体仓、未过期批次） */
    @Select("""
        SELECT IFNULL(SUM(s.qty_available), 0)
        FROM mat_stock s
        JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
        WHERE s.material_id = #{materialId} AND s.del_flag = 0
          AND s.holder_id IS NULL AND w.warehouse_type = 1
          AND (s.expire_date IS NULL OR s.expire_date >= CURDATE())""")
    BigDecimal sumAvailable(@Param("materialId") Long materialId);

    /**
     * 出库候选批次：只取实体仓、未过期、可用量大于 0 的批次，
     * 按「先到期先出（FEFO）」排序——到期日早的优先，无到期日的排最后。
     */
    @Select("""
        <script>
        SELECT s.material_id, s.warehouse_id, s.holder_id, s.batch_no, s.expire_date, s.inbound_date,
               s.qty_on_hand, s.qty_available, s.qty_locked,
               m.material_name, m.material_code, m.unit, w.warehouse_name, w.warehouse_type
        FROM mat_stock s
        JOIN mat_material m ON m.material_id = s.material_id AND m.del_flag = 0
        JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
        WHERE s.material_id = #{materialId} AND s.del_flag = 0
          AND s.qty_available &gt; 0 AND s.holder_id IS NULL AND w.warehouse_type = 1
          AND (s.expire_date IS NULL OR s.expire_date &gt;= CURDATE())
        <if test="warehouseId != null"> AND s.warehouse_id = #{warehouseId}</if>
        ORDER BY (s.expire_date IS NULL), s.expire_date, s.inbound_date, s.batch_no
        </script>""")
    List<Stock> selectFifoBatches(@Param("materialId") Long materialId,
                                  @Param("warehouseId") Long warehouseId);

    /** 库存总览（库管员查看公开库存用） */
    @Select("""
        <script>
        SELECT s.material_id, s.warehouse_id, s.holder_id, s.batch_no, s.expire_date, s.inbound_date,
               s.qty_on_hand, s.qty_available, s.qty_locked,
               m.material_name, m.material_code, m.unit, w.warehouse_name, w.warehouse_type
        FROM mat_stock s
        JOIN mat_material m ON m.material_id = s.material_id AND m.del_flag = 0
        JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
        WHERE s.del_flag = 0 AND s.holder_id IS NULL AND w.warehouse_type = 1
          AND s.qty_on_hand &gt; 0
        <if test="keyword != null and keyword != ''">
            AND (m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_code LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY m.material_code, s.expire_date
        </script>""")
    List<Stock> selectStockOverview(@Param("keyword") String keyword);
}
