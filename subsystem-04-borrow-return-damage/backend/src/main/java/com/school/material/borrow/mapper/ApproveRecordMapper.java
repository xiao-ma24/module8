package com.school.material.borrow.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.borrow.entity.ApproveRecord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/** 审批记录 Mapper：本子系统只写 biz_type=1（领用），读取时也只用 1。 */
@Mapper
public interface ApproveRecordMapper extends BaseMapper<ApproveRecord> {

    @Select("""
        SELECT a.record_id, a.biz_type, a.biz_id, a.approver_id, a.approve_result, a.approved_at, a.opinion,
               u.user_name AS approver_name
        FROM mat_approve_record a
        LEFT JOIN mat_sys_user u ON u.user_id = a.approver_id
        WHERE a.biz_type = #{bizType} AND a.biz_id = #{bizId} AND a.del_flag = 0
        ORDER BY a.record_id""")
    List<ApproveRecord> selectByBiz(@Param("bizType") Integer bizType, @Param("bizId") Long bizId);
}
