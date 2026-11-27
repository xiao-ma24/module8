package com.school.material.requisition.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.requisition.entity.ReqOrder;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ReqOrderMapper extends BaseMapper<ReqOrder> {

    /** 当天最大申请单号，用于生成四位流水 */
    @Select("SELECT MAX(req_no) FROM mat_req_order WHERE req_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxReqNo(@Param("prefix") String prefix);

    /** 按工号查 sys_user.user_id，查不到返回 null */
    @Select("SELECT user_id FROM mat_sys_user WHERE person_no = #{userNo} AND del_flag = 0 LIMIT 1")
    Long selectUserIdByNo(@Param("userNo") String userNo);
}
