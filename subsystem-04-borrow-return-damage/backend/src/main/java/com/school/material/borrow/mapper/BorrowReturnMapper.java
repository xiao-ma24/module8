package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.BorrowReturn;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 借用单 Mapper。逾期状态不落库（报告 borrow_status 无「逾期」取值），
 * 由 due_date 与当前日期实时计算，便于催还清单直接使用。
 */
@Mapper
public interface BorrowReturnMapper extends BaseMapper<BorrowReturn> {

    @Select("""
        <script>
        SELECT b.borrow_id, b.borrow_no, b.borrower_id, b.material_id, b.warehouse_id, b.batch_no,
               b.borrow_qty, b.returned_qty, b.written_off_qty, b.due_date, b.return_time,
               b.condition_code, b.borrow_status, b.created_at, b.updated_at,
               u.user_name AS borrower_name, u.person_no AS borrower_no, u.org_id AS dept_id,
               d.dept_name,
               m.material_name, m.material_code, m.unit, m.ref_price,
               w.warehouse_name,
               (SELECT IFNULL(SUM(s.qty_available), 0) FROM mat_stock s
                  JOIN mat_warehouse sw ON sw.warehouse_id = s.warehouse_id AND sw.del_flag = 0
                 WHERE s.material_id = b.material_id AND s.del_flag = 0 AND s.holder_id IS NULL
                   AND sw.warehouse_type = 1
                   AND (s.expire_date IS NULL OR s.expire_date &gt;= CURDATE())) AS available_qty,
               CASE WHEN b.borrow_status = 2 AND b.due_date &lt; CURDATE() THEN 1 ELSE 0 END AS overdue,
               CASE WHEN b.borrow_status = 2 AND b.due_date &lt; CURDATE() THEN DATEDIFF(CURDATE(), b.due_date) ELSE 0 END AS overdue_days,
               o.out_no
        FROM mat_borrow_return b
        LEFT JOIN mat_sys_user u ON u.user_id = b.borrower_id
        LEFT JOIN mat_dept d ON d.org_id = u.org_id
        LEFT JOIN mat_material m ON m.material_id = b.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = b.warehouse_id
        LEFT JOIN mat_out_order o ON o.source_type = 2 AND o.source_id = b.borrow_id AND o.del_flag = 0
        WHERE b.del_flag = 0
        <if test="borrowerId != null"> AND b.borrower_id = #{borrowerId}</if>
        <if test="deptId != null"> AND u.org_id = #{deptId}</if>
        <if test="status != null"> AND b.borrow_status = #{status}</if>
        <if test="statuses != null and statuses.size() &gt; 0">
            AND b.borrow_status IN
            <foreach collection="statuses" item="st" open="(" separator="," close=")">#{st}</foreach>
        </if>
        <if test="overdueOnly != null and overdueOnly">
            AND b.borrow_status = 2 AND b.due_date &lt; CURDATE()
        </if>
        <if test="keyword != null and keyword != ''">
            AND (b.borrow_no LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR u.user_name LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY b.borrow_id DESC
        </script>""")
    List<BorrowReturn> selectBorrows(@Param("borrowerId") Long borrowerId,
                                     @Param("deptId") String deptId,
                                     @Param("status") Integer status,
                                     @Param("statuses") List<Integer> statuses,
                                     @Param("overdueOnly") Boolean overdueOnly,
                                     @Param("keyword") String keyword);

    @Select("""
        SELECT b.borrow_id, b.borrow_no, b.borrower_id, b.material_id, b.warehouse_id, b.batch_no,
               b.borrow_qty, b.returned_qty, b.written_off_qty, b.due_date, b.return_time,
               b.condition_code, b.borrow_status, b.created_at, b.updated_at,
               u.user_name AS borrower_name, u.person_no AS borrower_no, u.org_id AS dept_id,
               d.dept_name, m.material_name, m.material_code, m.unit, m.ref_price,
               w.warehouse_name,
               (SELECT IFNULL(SUM(s.qty_available), 0) FROM mat_stock s
                  JOIN mat_warehouse sw ON sw.warehouse_id = s.warehouse_id AND sw.del_flag = 0
                 WHERE s.material_id = b.material_id AND s.del_flag = 0 AND s.holder_id IS NULL
                   AND sw.warehouse_type = 1
                   AND (s.expire_date IS NULL OR s.expire_date >= CURDATE())) AS available_qty,
               CASE WHEN b.borrow_status = 2 AND b.due_date < CURDATE() THEN 1 ELSE 0 END AS overdue,
               CASE WHEN b.borrow_status = 2 AND b.due_date < CURDATE() THEN DATEDIFF(CURDATE(), b.due_date) ELSE 0 END AS overdue_days,
               o.out_no
        FROM mat_borrow_return b
        LEFT JOIN mat_sys_user u ON u.user_id = b.borrower_id
        LEFT JOIN mat_dept d ON d.org_id = u.org_id
        LEFT JOIN mat_material m ON m.material_id = b.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = b.warehouse_id
        LEFT JOIN mat_out_order o ON o.source_type = 2 AND o.source_id = b.borrow_id AND o.del_flag = 0
        WHERE b.borrow_id = #{borrowId} AND b.del_flag = 0""")
    BorrowReturn selectDetail(@Param("borrowId") Long borrowId);

    @Select("SELECT COUNT(*) FROM mat_borrow_return WHERE borrow_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);
}
