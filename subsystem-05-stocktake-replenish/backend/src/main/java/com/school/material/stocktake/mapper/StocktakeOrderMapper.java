package com.school.material.stocktake.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.stocktake.entity.StocktakeOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/** 盘点单：台账查询带仓库名与差异统计，列名显式别名为驼峰，避免 Map 结果集受配置影响 */
public interface StocktakeOrderMapper extends BaseMapper<StocktakeOrder> {

    @Select("""
        <script>
        SELECT o.stocktake_id AS stocktakeId, o.stocktake_no AS stocktakeNo, o.warehouse_id AS warehouseId,
               o.stocktake_date AS stocktakeDate, o.auditor_id AS auditorId, o.status AS status,
               o.remark AS remark, o.created_at AS createdAt,
               w.warehouse_name AS warehouseName,
               u.user_name AS auditorName,
               (SELECT COUNT(*) FROM stocktake_item i
                 WHERE i.stocktake_id = o.stocktake_id AND i.del_flag = 0) AS itemCount,
               (SELECT IFNULL(SUM(i.diff_quantity), 0) FROM stocktake_item i
                 WHERE i.stocktake_id = o.stocktake_id AND i.del_flag = 0) AS diffTotal
        FROM stocktake_order o
        LEFT JOIN mat_warehouse w ON w.warehouse_id = o.warehouse_id
        LEFT JOIN mat_sys_user u ON u.user_id = o.auditor_id
        WHERE o.del_flag = 0
        <if test="status != null"> AND o.status = #{status}</if>
        <if test="keyword != null and keyword != ''">
          AND (o.stocktake_no LIKE CONCAT('%', #{keyword}, '%') OR w.warehouse_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY o.stocktake_id DESC
        </script>""")
    List<Map<String, Object>> selectOrderList(@Param("status") Integer status,
                                              @Param("keyword") String keyword);
}
