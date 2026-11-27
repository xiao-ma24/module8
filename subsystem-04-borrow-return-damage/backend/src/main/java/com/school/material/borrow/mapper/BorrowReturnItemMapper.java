package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.BorrowReturnItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 借用分次归还明细 Mapper */
@Mapper
public interface BorrowReturnItemMapper extends BaseMapper<BorrowReturnItem> {

    @Select("""
        SELECT i.item_id, i.borrow_id, i.item_no, i.return_qty, i.accept_result, i.warehouse_id,
               i.batch_no, i.keeper_id, i.return_time, i.remark,
               k.user_name AS keeper_name, m.material_name, m.unit
        FROM borrow_return_item i
        LEFT JOIN mat_sys_user k ON k.user_id = i.keeper_id
        LEFT JOIN mat_borrow_return b ON b.borrow_id = i.borrow_id
        LEFT JOIN mat_material m ON m.material_id = b.material_id
        WHERE i.borrow_id = #{borrowId} AND i.del_flag = 0
        ORDER BY i.item_id""")
    List<BorrowReturnItem> selectByBorrow(@Param("borrowId") Long borrowId);

    @Select("SELECT COUNT(*) FROM borrow_return_item WHERE item_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);
}
