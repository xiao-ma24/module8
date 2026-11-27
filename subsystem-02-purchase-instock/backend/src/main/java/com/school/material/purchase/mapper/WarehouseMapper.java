package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.Warehouse;
import org.apache.ibatis.annotations.Select;
import java.util.List;

public interface WarehouseMapper extends BaseMapper<Warehouse> {

    @Select("""
        SELECT w.*, u.user_name AS keeper_name,
               (SELECT COUNT(*) FROM mat_stock s WHERE s.warehouse_id = w.warehouse_id AND s.qty_on_hand > 0) AS stock_count
        FROM mat_warehouse w LEFT JOIN mat_sys_user u ON u.user_id = w.keeper_id
        WHERE w.del_flag = 0 ORDER BY w.warehouse_id""")
    List<Warehouse> selectListWithKeeper();
}
