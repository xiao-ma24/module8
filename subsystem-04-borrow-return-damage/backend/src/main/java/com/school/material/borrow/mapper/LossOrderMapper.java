package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.LossOrder;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

/** 报损单 Mapper */
@Mapper
public interface LossOrderMapper extends BaseMapper<LossOrder> {

    @Select("""
        <script>
        SELECT l.loss_id, l.loss_no, l.source_type, l.source_id, l.material_id, l.batch_no,
               l.warehouse_id, l.holder_id, l.loss_qty, l.reason, l.approve_status, l.dispose_result,
               l.executed, l.exec_time, l.exec_operator_id, l.created_at,
               m.material_name, m.material_code, m.unit, w.warehouse_name,
               b.borrow_no, b.condition_code, u.user_name AS borrower_name, u.org_id AS dept_id, d.dept_name,
               eo.user_name AS exec_operator_name
        FROM mat_loss_order l
        LEFT JOIN mat_material m ON m.material_id = l.material_id
        LEFT JOIN mat_warehouse w ON w.warehouse_id = l.warehouse_id
        LEFT JOIN mat_borrow_return b ON b.borrow_id = l.source_id AND l.source_type = 1
        LEFT JOIN mat_sys_user u ON u.user_id = b.borrower_id
        LEFT JOIN mat_dept d ON d.org_id = u.org_id
        LEFT JOIN mat_sys_user eo ON eo.user_id = l.exec_operator_id
        WHERE l.del_flag = 0
        <if test="status != null"> AND l.approve_status = #{status}</if>
        <if test="deptId != null"> AND u.org_id = #{deptId}</if>
        <if test="borrowerId != null"> AND b.borrower_id = #{borrowerId}</if>
        <if test="sourceType != null"> AND l.source_type = #{sourceType}</if>
        <if test="keyword != null and keyword != ''">
            AND (l.loss_no LIKE CONCAT('%', #{keyword}, '%')
                 OR m.material_name LIKE CONCAT('%', #{keyword}, '%')
                 OR b.borrow_no LIKE CONCAT('%', #{keyword}, '%'))
        </if>
        ORDER BY l.loss_id DESC
        </script>""")
    List<LossOrder> selectLosses(@Param("status") Integer status,
                                 @Param("deptId") String deptId,
                                 @Param("borrowerId") Long borrowerId,
                                 @Param("sourceType") Integer sourceType,
                                 @Param("keyword") String keyword);

    @Select("SELECT COUNT(*) FROM mat_loss_order WHERE loss_no LIKE CONCAT(#{prefix}, '%')")
    long countByNoPrefix(@Param("prefix") String prefix);

    /** 某借用单下仍在审批中的报损单数量（用于判断借用单是否「已报损」未闭环） */
    @Select("""
        SELECT COUNT(*) FROM mat_loss_order
        WHERE source_type = 1 AND source_id = #{borrowId} AND approve_status = 0 AND del_flag = 0""")
    long countPendingByBorrow(@Param("borrowId") Long borrowId);

    /** 以 executed=0 为条件置为已执行，重复核销更新 0 行，防止重复扣减库存 */
    @Update("""
        UPDATE mat_loss_order
        SET executed = 1, exec_time = NOW(), exec_operator_id = #{operatorId},
            dispose_result = #{disposeResult}, updated_by = #{operatorId}
        WHERE loss_id = #{lossId} AND executed = 0 AND approve_status = 1 AND del_flag = 0""")
    int markExecuted(@Param("lossId") Long lossId,
                     @Param("operatorId") Long operatorId,
                     @Param("disposeResult") Integer disposeResult);

    /** 审批结论回写（approve_status：1 通过、2 驳回） */
    @Update("""
        UPDATE mat_loss_order SET approve_status = #{status}, dispose_result = #{disposeResult},
               updated_by = #{operatorId}
        WHERE loss_id = #{lossId} AND approve_status = 0 AND del_flag = 0""")
    int markApproved(@Param("lossId") Long lossId,
                     @Param("status") Integer status,
                     @Param("disposeResult") Integer disposeResult,
                     @Param("operatorId") Long operatorId);
}
