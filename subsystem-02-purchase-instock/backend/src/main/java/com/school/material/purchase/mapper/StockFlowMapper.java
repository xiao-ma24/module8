package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.StockFlow;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

public interface StockFlowMapper extends BaseMapper<StockFlow> {

    /** 库存流水查询（物资目录导出用）：按物资编码/名称模糊匹配，可按时间范围过滤 */
    @Select("""
        <script>
        SELECT f.flow_id AS flowId, f.flow_time AS flowTime, f.biz_type AS bizType,
               f.change_qty AS changeQty, f.batch_no AS batchNo, f.biz_id AS bizId,
               m.material_code AS materialCode, m.material_name AS materialName, m.unit AS unit,
               w.warehouse_name AS warehouseName, u.user_name AS operatorName
        FROM mat_stock_flow f
        LEFT JOIN mat_material m ON m.material_id = f.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = f.warehouse_id
        LEFT JOIN mat_sys_user u ON u.user_id = f.operator_id
        <where>
            <if test="keyword != null and keyword != ''">
                AND (m.material_code LIKE CONCAT('%', #{keyword}, '%')
                     OR m.material_name LIKE CONCAT('%', #{keyword}, '%'))
            </if>
            <if test="from != null"> AND f.flow_time &gt;= #{from}</if>
            <if test="to != null"> AND f.flow_time &lt;= #{to}</if>
        </where>
        ORDER BY f.flow_time DESC, f.flow_id DESC
        LIMIT 5000
        </script>""")
    List<Map<String, Object>> selectFlows(@Param("keyword") String keyword,
                                          @Param("from") LocalDateTime from,
                                          @Param("to") LocalDateTime to);
}
