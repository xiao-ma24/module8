package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.PurchaseRequest;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchaseRequestMapper extends BaseMapper<PurchaseRequest> {

    /** 当天最大采购申请单号，用于生成四位流水 */
    @Select("SELECT MAX(pr_no) FROM purchase_request WHERE pr_no LIKE CONCAT(#{prefix}, '%')")
    String selectMaxPrNo(@Param("prefix") String prefix);

    /** 按工号查 sys_user.user_id，查不到返回 null */
    @Select("SELECT user_id FROM mat_sys_user WHERE person_no = #{userNo} AND del_flag = 0 LIMIT 1")
    Long selectUserIdByNo(@Param("userNo") String userNo);

    /** 按 user_id 查归属部门 org_id，用于自备登记补齐部门 */
    @Select("SELECT org_id FROM mat_sys_user WHERE user_id = #{userId} AND del_flag = 0")
    String selectDeptIdByUserId(@Param("userId") Long userId);
}
