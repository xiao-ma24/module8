package com.school.material.mapper;

import com.school.material.dto.StockVO;
import com.school.material.entity.Stock;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.math.BigDecimal;
import java.util.List;

/**
 * 库存 Mapper：stock 为联合主键表，全部使用自定义 SQL
 */
@Mapper
public interface StockMapper {

    @Select("""
            SELECT material_id, warehouse_id, holder_id, batch_no, expire_date, inbound_date,
                   qty_on_hand, qty_available, qty_locked, created_by, created_at,
                   updated_by, updated_at, del_flag
            FROM mat_stock
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND batch_no = #{batchNo} AND del_flag = 0
            LIMIT 1
            """)
    Stock selectBatch(@Param("materialId") Long materialId,
                      @Param("warehouseId") Long warehouseId,
                      @Param("batchNo") String batchNo);

    @Select("""
            SELECT COALESCE(SUM(qty_on_hand), 0)
            FROM mat_stock
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId} AND del_flag = 0
            """)
    BigDecimal sumQtyOnHand(@Param("materialId") Long materialId,
                            @Param("warehouseId") Long warehouseId);

    /** 出库时按先进先出挑选有可用量的批次 */
    @Select("""
            SELECT batch_no
            FROM mat_stock
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND qty_available >= #{num} AND del_flag = 0
            ORDER BY inbound_date, batch_no
            LIMIT 1
            """)
    String selectAvailableBatch(@Param("materialId") Long materialId,
                                @Param("warehouseId") Long warehouseId,
                                @Param("num") BigDecimal num);

    @Insert("""
            INSERT INTO mat_stock (material_id, warehouse_id, batch_no, inbound_date,
                               qty_on_hand, qty_available, qty_locked,
                               created_at, updated_at, del_flag)
            VALUES (#{materialId}, #{warehouseId}, #{batchNo}, #{inboundDate},
                    #{qtyOnHand}, #{qtyAvailable}, #{qtyLocked}, NOW(), NOW(), 0)
            """)
    int insertBatch(Stock stock);

    /** 个人保管入库建行：记录保管人，可用量与预占量固定为 0，不参与公共领用 */
    @Insert("""
            INSERT INTO mat_stock (material_id, warehouse_id, holder_id, batch_no, inbound_date,
                               qty_on_hand, qty_available, qty_locked,
                               created_at, updated_at, del_flag)
            VALUES (#{materialId}, #{warehouseId}, #{holderId}, #{batchNo}, #{inboundDate},
                    #{qtyOnHand}, 0, 0, NOW(), NOW(), 0)
            """)
    int insertPersonalBatch(Stock stock);

    /** 个人保管入库：只增加在库量，可用量保持 0，且只能累加到同一保管人的批次 */
    @Update("""
            UPDATE mat_stock
            SET qty_on_hand = qty_on_hand + #{num},
                updated_at = NOW()
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND batch_no = #{batchNo} AND holder_id = #{holderId}
              AND qty_available = 0 AND del_flag = 0
            """)
    int increasePersonalBatch(@Param("materialId") Long materialId,
                              @Param("warehouseId") Long warehouseId,
                              @Param("batchNo") String batchNo,
                              @Param("holderId") Long holderId,
                              @Param("num") BigDecimal num);

    /** 入库：在库总量与可领量同步增加（batchNo 由 Service 解析，发票入库固定 DEFAULT） */
    @Update("""
            UPDATE mat_stock
            SET qty_on_hand = qty_on_hand + #{num},
                qty_available = qty_available + #{num},
                updated_at = NOW()
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND batch_no = #{batchNo} AND del_flag = 0
            """)
    int increaseBatch(@Param("materialId") Long materialId,
                      @Param("warehouseId") Long warehouseId,
                      @Param("batchNo") String batchNo,
                      @Param("num") BigDecimal num);

    /** 出库：带可用量校验的原子扣减，影响行数为 0 即库存不足 */
    @Update("""
            UPDATE mat_stock
            SET qty_on_hand = qty_on_hand - #{num},
                qty_available = qty_available - #{num},
                updated_at = NOW()
            WHERE material_id = #{materialId} AND warehouse_id = #{warehouseId}
              AND batch_no = #{batchNo} AND del_flag = 0
              AND qty_available >= #{num}
            """)
    int decreaseBatch(@Param("materialId") Long materialId,
                      @Param("warehouseId") Long warehouseId,
                      @Param("batchNo") String batchNo,
                      @Param("num") BigDecimal num);

    @Select("""
            <script>
            SELECT s.material_id AS material_id,
                   s.warehouse_id AS warehouse_id,
                   s.holder_id AS holder_id,
                   s.batch_no AS batch_no,
                   s.qty_on_hand AS qty_on_hand,
                   s.qty_available AS qty_available,
                   s.qty_locked AS qty_locked,
                   s.inbound_date AS inbound_date,
                   m.material_name AS material_name,
                   m.material_code AS material_code,
                   w.warehouse_name AS warehouse_name,
                   m.safe_stock AS safe_stock
            FROM mat_stock s
            LEFT JOIN mat_material m ON m.material_id = s.material_id AND m.del_flag = 0
            LEFT JOIN mat_warehouse w ON w.warehouse_id = s.warehouse_id AND w.del_flag = 0
            WHERE s.del_flag = 0
            <if test="materialId != null">
                AND s.material_id = #{materialId}
            </if>
            <if test="warehouseId != null">
                AND s.warehouse_id = #{warehouseId}
            </if>
            ORDER BY s.material_id, s.warehouse_id, s.batch_no
            LIMIT #{offset}, #{size}
            </script>
            """)
    List<StockVO> selectPageList(@Param("materialId") Long materialId,
                                 @Param("warehouseId") Long warehouseId,
                                 @Param("offset") long offset,
                                 @Param("size") long size);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM mat_stock s
            WHERE s.del_flag = 0
            <if test="materialId != null">
                AND s.material_id = #{materialId}
            </if>
            <if test="warehouseId != null">
                AND s.warehouse_id = #{warehouseId}
            </if>
            </script>
            """)
    long countPage(@Param("materialId") Long materialId,
                   @Param("warehouseId") Long warehouseId);
}
