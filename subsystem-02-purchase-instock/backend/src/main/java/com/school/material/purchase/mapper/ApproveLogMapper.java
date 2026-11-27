package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.ApproveLog;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Param;
import java.util.List;

public interface ApproveLogMapper extends BaseMapper<ApproveLog> {

    @Select("""
        SELECT l.*, l.record_id AS log_id, u.user_name AS approver_name
        FROM mat_approve_record l LEFT JOIN mat_sys_user u ON u.user_id = l.approver_id
        WHERE l.biz_type = #{bizType} AND l.biz_id = #{bizId}
        ORDER BY l.record_id""")
    List<ApproveLog> selectByBiz(@Param("bizType") Integer bizType, @Param("bizId") Long bizId);
}
