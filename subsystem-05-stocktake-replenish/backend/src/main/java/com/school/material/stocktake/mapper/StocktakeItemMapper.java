package com.school.material.stocktake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.stocktake.entity.StocktakeItem;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 盘点明细：按盘点单读取，联物资档案展示名称、编码、单位与参考单价（列名别名驼峰） */
public interface StocktakeItemMapper extends BaseMapper<StocktakeItem> {

    @Select("""
        SELECT i.item_id AS itemId, i.stocktake_id AS stocktakeId, i.material_id AS materialId,
               i.batch_no AS batchNo, i.book_quantity AS bookQuantity, i.actual_quantity AS actualQuantity,
               i.diff_quantity AS diffQuantity, i.diff_reason AS diffReason, i.counted AS counted,
               m.material_name AS materialName, m.material_code AS materialCode,
               m.material_type AS materialType, m.unit AS unit, m.ref_price AS refPrice
        FROM stocktake_item i
        JOIN mat_material m ON m.material_id = i.material_id
        WHERE i.stocktake_id = #{stocktakeId} AND i.del_flag = 0
        ORDER BY i.item_id""")
    List<Map<String, Object>> selectByOrder(@Param("stocktakeId") Long stocktakeId);

    /** 已完成的盘点明细：物资 + 批次 → 最近一次完成的盘点单号 */
    @Select("""
        SELECT i.material_id AS materialId, i.batch_no AS batchNo,
               o.stocktake_no AS stocktakeNo
        FROM stocktake_item i
        JOIN stocktake_order o ON o.stocktake_id = i.stocktake_id
        WHERE o.status = 2 AND o.del_flag = 0 AND i.del_flag = 0
        ORDER BY o.stocktake_id DESC""")
    List<Map<String, Object>> selectCompletedBatchKeys();
}
