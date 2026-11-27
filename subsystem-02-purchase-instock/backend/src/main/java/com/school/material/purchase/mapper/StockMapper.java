package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.*;
import org.apache.ibatis.annotations.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * 库存操作：以“物资+仓库+批次”为库存最小单元，全部走自定义 SQL。
 * 所有余额变动都带数量前置条件，更新行数必须为 1；禁止上层先读后写。
 * 数量口径：qty_on_hand 在库总量（含在借与预占）、qty_available 公共可领量、qty_locked 预占量。
 */
public interface StockMapper extends BaseMapper<Stock> {

    /** 库存总览（联物资/仓库），支持物资名模糊过滤；在途量=未完成采购订单已订购未到货数量（补货模型 q_t） */
    @Select("""
        <script>
        SELECT s.material_id, s.warehouse_id, s.holder_id, s.batch_no, s.expire_date, s.inbound_date,
               s.qty_on_hand, s.qty_available, s.qty_locked,
               m.material_name, m.material_code, m.unit, m.safe_stock, m.category_code,
               w.warehouse_name, w.warehouse_type,
               hu.user_name AS holder_name,
               (SELECT IFNULL(SUM(poi.order_qty - IFNULL(poi.arrived_qty, 0)), 0)
                  FROM purchase_order_item poi
                  JOIN purchase_order po ON po.po_id = poi.po_id
                 WHERE poi.material_id = s.material_id
                   AND po.po_status IN (1, 2) AND po.del_flag = 0
                   AND poi.order_qty &gt; IFNULL(poi.arrived_qty, 0)) AS in_transit
        FROM mat_stock s
        JOIN mat_material m ON m.material_id = s.material_id AND m.del_flag = 0
        JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
        LEFT JOIN mat_sys_user hu ON hu.user_id = s.holder_id
        <where>
            s.qty_on_hand &gt; 0
            <if test="keyword != null and keyword != ''"> AND m.material_name LIKE CONCAT('%', #{keyword}, '%')</if>
            <if test="warehouseId != null"> AND s.warehouse_id = #{warehouseId}</if>
            <if test="lowOnly != null and lowOnly"> AND s.qty_available &lt;= m.safe_stock</if>
        </where>
        ORDER BY (s.qty_available &lt;= m.safe_stock) DESC, m.material_code
        </script>""")
    List<Stock> selectStockList(@Param("keyword") String keyword,
                                @Param("warehouseId") Long warehouseId,
                                @Param("lowOnly") Boolean lowOnly);

    /** 某物资近30天消耗量（补货建议依据）：借用/归还/预占流水不参与消耗统计 */
    @Select("""
        SELECT IFNULL(SUM(-f.change_qty), 0)
        FROM mat_stock_flow f
        WHERE f.material_id = #{materialId} AND f.change_qty < 0
          AND f.biz_type IN (2, 6, 7)
          AND f.flow_time >= DATE_SUB(NOW(), INTERVAL 30 DAY)""")
    BigDecimal sumMonthConsumed(@Param("materialId") Long materialId);

    /** 幂等建行：同一物资+仓库+批次只存在一行，并发由联合主键保证。 */
    @Insert("""
        INSERT IGNORE INTO mat_stock (material_id, warehouse_id, holder_id, batch_no, expire_date, inbound_date,
                                  qty_on_hand, qty_available, qty_locked)
        VALUES (#{materialId}, #{warehouseId}, #{holderId}, #{batchNo}, #{expireDate}, #{inboundDate}, 0, 0, 0)""")
    int ensureRow(@Param("materialId") Long materialId,
                  @Param("warehouseId") Long warehouseId,
                  @Param("holderId") Long holderId,
                  @Param("batchNo") String batchNo,
                  @Param("expireDate") LocalDate expireDate,
                  @Param("inboundDate") LocalDate inboundDate);

    /** 事务内锁定余额行，用于预占、出库、核销前的状态校验。 */
    @Select("""
        SELECT material_id, warehouse_id, holder_id, batch_no, expire_date, inbound_date,
               qty_on_hand, qty_available, qty_locked
        FROM mat_stock
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0
        FOR UPDATE""")
    Stock lockOne(@Param("materialId") Long materialId,
                  @Param("warehouseId") Long warehouseId,
                  @Param("batchNo") String batchNo);

    /** 公共库存合格入库：在库 +q，可用 +q。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand + #{qty}, qty_available = qty_available + #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0""")
    int increasePublic(@Param("materialId") Long materialId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("batchNo") String batchNo,
                       @Param("qty") BigDecimal qty);

    /** 个人保管库存入库：在库 +q，可用量与预占量必须保持 0，且保管人必须一致。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand + #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND holder_id = #{holderId} AND qty_available = 0 AND qty_locked = 0""")
    int increasePersonal(@Param("materialId") Long materialId,
                         @Param("warehouseId") Long warehouseId,
                         @Param("batchNo") String batchNo,
                         @Param("holderId") Long holderId,
                         @Param("qty") BigDecimal qty);

    /** 审批通过预占：可用 -q，预占 +q。 */
    @Update("""
        UPDATE mat_stock SET qty_available = qty_available - #{qty}, qty_locked = qty_locked + #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_available >= #{qty}""")
    int reserve(@Param("materialId") Long materialId,
                @Param("warehouseId") Long warehouseId,
                @Param("batchNo") String batchNo,
                @Param("qty") BigDecimal qty);

    /** 取消/关闭释放预占：可用 +q，预占 -q。 */
    @Update("""
        UPDATE mat_stock SET qty_available = qty_available + #{qty}, qty_locked = qty_locked - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_locked >= #{qty}""")
    int releaseReserve(@Param("materialId") Long materialId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("batchNo") String batchNo,
                       @Param("qty") BigDecimal qty);

    /** 领用出库消耗已有预占：在库 -q，预占 -q，可用量已在审批时扣减，此处不再重复扣减。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand - #{qty}, qty_locked = qty_locked - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_locked >= #{qty} AND qty_on_hand >= #{qty}""")
    int consumeReserved(@Param("materialId") Long materialId,
                        @Param("warehouseId") Long warehouseId,
                        @Param("batchNo") String batchNo,
                        @Param("qty") BigDecimal qty);

    /** 借出并消耗预占：在库不变，可用量不变（审批时已扣），预占 -q。 */
    @Update("""
        UPDATE mat_stock SET qty_locked = qty_locked - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_locked >= #{qty}""")
    int borrowReserved(@Param("materialId") Long materialId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("batchNo") String batchNo,
                       @Param("qty") BigDecimal qty);

    /** 领用出库（未提前预占）：在库 -q，可用 -q。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand - #{qty}, qty_available = qty_available - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_available >= #{qty} AND qty_on_hand >= #{qty}""")
    int issueDirect(@Param("materialId") Long materialId,
                    @Param("warehouseId") Long warehouseId,
                    @Param("batchNo") String batchNo,
                    @Param("qty") BigDecimal qty);

    /** 借出且未提前预占：在库不变，可用 -q。 */
    @Update("""
        UPDATE mat_stock SET qty_available = qty_available - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_available >= #{qty}""")
    int borrowUnreserved(@Param("materialId") Long materialId,
                         @Param("warehouseId") Long warehouseId,
                         @Param("batchNo") String batchNo,
                         @Param("qty") BigDecimal qty);

    /** 完好归还：在库不变，可用 +q；不能超过在库中被借走的部分。 */
    @Update("""
        UPDATE mat_stock SET qty_available = qty_available + #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_available + qty_locked + #{qty} <= qty_on_hand""")
    int returnGood(@Param("materialId") Long materialId,
                   @Param("warehouseId") Long warehouseId,
                   @Param("batchNo") String batchNo,
                   @Param("qty") BigDecimal qty);

    /** 已借出物资报损核销：在库 -q，可用与预占不动；被核销的数量不能来自公共可用库存。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_on_hand >= #{qty}
          AND qty_available + qty_locked <= qty_on_hand - #{qty}""")
    int writeOffBorrowed(@Param("materialId") Long materialId,
                         @Param("warehouseId") Long warehouseId,
                         @Param("batchNo") String batchNo,
                         @Param("qty") BigDecimal qty);

    /** 库内可用物资报废/盘亏核销：在库 -q，可用 -q。 */
    @Update("""
        UPDATE mat_stock SET qty_on_hand = qty_on_hand - #{qty}, qty_available = qty_available - #{qty}
        WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND batch_no = #{batchNo}
          AND del_flag = 0 AND qty_available >= #{qty} AND qty_on_hand >= #{qty}""")
    int scrapAvailable(@Param("materialId") Long materialId,
                       @Param("warehouseId") Long warehouseId,
                       @Param("batchNo") String batchNo,
                       @Param("qty") BigDecimal qty);

    /**
     * 出库选批：只取实体仓、未过期且可领量大于 0 的批次，按先到期先出排序。
     * 个人保管仓与过期未核销批次一律不进入可领候选。
     */
    @Select("""
        <script>
        SELECT s.material_id, s.warehouse_id, s.holder_id, s.batch_no, s.expire_date, s.inbound_date,
               s.qty_on_hand, s.qty_available, s.qty_locked
        FROM mat_stock s
        JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
        WHERE s.material_id = #{materialId} AND s.del_flag = 0
          AND s.qty_available > 0
          AND w.warehouse_type = 1
          AND (s.expire_date IS NULL OR s.expire_date &gt;= CURDATE())
          <if test="warehouseId != null"> AND s.warehouse_id = #{warehouseId}</if>
        ORDER BY (s.expire_date IS NULL), s.expire_date, s.inbound_date, s.batch_no
        </script>""")
    List<Stock> selectFifoBatches(@Param("materialId") Long materialId,
                                  @Param("warehouseId") Long warehouseId);
}
