package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.OutOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 出库单 Mapper（借用借出：source_type=2） */
@Mapper
public interface OutOrderMapper extends BaseMapper<OutOrder> {

    @Select("""
        <script>
        SELECT o.out_id, o.out_no, o.source_type, o.source_id, o.warehouse_id, o.keeper_id, o.holder_id,
               o.material_id, o.batch_no, o.out_qty, o.out_time,
               m.material_name, m.unit, w.warehouse_name, k.user_name AS keeper_name,
               b.borrow_no, u.user_name AS borrower_name
        FROM mat_out_order o
        LEFT JOIN mat_material m ON m.material_id = o.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = o.warehouse_id
        LEFT JOIN mat_sys_user k ON k.user_id = o.keeper_id
        LEFT JOIN mat_borrow_return b ON b.borrow_id = o.source_id AND o.source_type = 2
        LEFT JOIN mat_sys_user u ON u.user_id = b.borrower_id
        WHERE o.del_flag = 0 AND o.source_type = 2
        <if test="borrowerId != null"> AND b.borrower_id = #{borrowerId}</if>
        <if test="keyword != null and keyword != ''">
            AND (o.out_no LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR b.borrow_no LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY o.out_id DESC
        </script>""")
    List<OutOrder> selectBorrowOuts(@Param("borrowerId") Long borrowerId,
                                    @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM mat_out_order WHERE out_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);

    @Select("SELECT COUNT(*) FROM mat_out_order WHERE source_type = 2 AND source_id = #{borrowId} AND del_flag = 0")
    long countByBorrow(@Param("borrowId") Long borrowId);
}
