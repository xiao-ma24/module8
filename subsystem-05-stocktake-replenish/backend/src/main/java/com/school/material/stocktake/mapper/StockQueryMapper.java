package com.school.material.stocktake.mapper;

import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * 跨子系统只读查询：物资目录、仓库、库存余额、领用流水、在途量、系统参数。
 * 按《开发约束与规范》§5，本模块只读这些表，不写 stock / stock_flow。
 * 返回给前端的查询已把列名别名为驼峰。
 */
public interface StockQueryMapper {

    @Select("""
        SELECT material_id, material_code, material_name, category_code, material_type, unit,
               ref_price, safe_stock, service_life_years, shelf_life_days
        FROM mat_material WHERE del_flag = 0
        ORDER BY material_code""")
    List<Map<String, Object>> selectMaterials();

    @Select("""
        SELECT warehouse_id, warehouse_name, warehouse_type, keeper_id
        FROM mat_warehouse WHERE del_flag = 0 ORDER BY warehouse_id""")
    List<Map<String, Object>> selectWarehouses();

    @Select("""
        <script>
        SELECT material_id, warehouse_id, holder_id, batch_no, expire_date, inbound_date,
               qty_on_hand, qty_available, qty_locked
        FROM mat_stock
        WHERE del_flag = 0 AND qty_on_hand &gt; 0
        <if test="warehouseId != null"> AND warehouse_id = #{warehouseId}</if>
        ORDER BY material_id, expire_date, inbound_date
        </script>""")
    List<Map<String, Object>> selectBatches(@Param("warehouseId") Long warehouseId);

    /** 近 N 天领用出库量（biz_type = 2），用于日均消耗 d */
    @Select("""
        SELECT material_id, IFNULL(SUM(-change_qty), 0) AS consumed
        FROM mat_stock_flow
        WHERE biz_type = 2 AND flow_time >= #{from}
        GROUP BY material_id""")
    List<Map<String, Object>> selectConsumed(@Param("from") LocalDateTime from);

    /** 各物资首次/最近入库时间与次数，用于估算采购提前期 L */
    @Select("""
        SELECT material_id, COUNT(*) AS cnt, MIN(flow_time) AS first_in, MAX(flow_time) AS last_in
        FROM mat_stock_flow WHERE biz_type = 1 GROUP BY material_id""")
    List<Map<String, Object>> selectInboundStats();

    /** 在途量 qt：未完成采购订单（1 已确认 / 2 部分到货）中已订购未到货数量 */
    @Select("""
        SELECT poi.material_id, IFNULL(SUM(poi.order_qty - IFNULL(poi.arrived_qty, 0)), 0) AS in_transit
        FROM purchase_order_item poi
        JOIN purchase_order po ON po.po_id = poi.po_id
        WHERE po.po_status IN (1, 2) AND po.del_flag = 0
          AND poi.order_qty > IFNULL(poi.arrived_qty, 0)
        GROUP BY poi.material_id""")
    List<Map<String, Object>> selectInTransit();

    @Select("""
        SELECT supplier_id, supplier_name, star_level
        FROM mat_supplier WHERE del_flag = 0 AND supplier_status = 1
        ORDER BY star_level DESC""")
    List<Map<String, Object>> selectSuppliers();

    /** 申请部门候选（只读 dept，组织由平台管理端维护） */
    @Select("""
        SELECT org_id AS dept_id, dept_code, dept_name FROM mat_dept
        WHERE del_flag = 0 AND dept_status = 1 ORDER BY org_id""")
    List<Map<String, Object>> selectDepts();

    /** 盘点负责人候选：库管员 / 管理员（只读 sys_user，账号由平台管理端维护） */
    @Select("""
        SELECT user_id, person_no AS user_no, user_name, role_code, org_id AS dept_id
        FROM mat_sys_user WHERE del_flag = 0 AND user_status = 1
        ORDER BY user_id""")
    List<Map<String, Object>> selectUsers();

    /** 用户的主角色与多角色授权编码（用于盘点岗位校验） */
    @Select("""
        SELECT u.role_code FROM mat_sys_user u
        WHERE u.user_id = #{userId} AND u.del_flag = 0
        UNION
        SELECT r.role_code FROM mat_sys_user_role ur
        JOIN mat_sys_role r ON r.role_id = ur.role_id
        WHERE ur.user_id = #{userId}""")
    List<String> selectRoleCodes(Long userId);

    /** 系统参数（只读）：补货模型参数可由平台管理端在 sys_config 维护 */
    @Select("""
        SELECT config_key, config_value FROM sys_config
        WHERE config_key LIKE 'stocktake.%'""")
    List<Map<String, Object>> selectStocktakeConfigs();
}
