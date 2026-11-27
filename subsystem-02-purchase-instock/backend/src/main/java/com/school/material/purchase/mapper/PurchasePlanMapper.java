package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.PurchasePlan;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface PurchasePlanMapper extends BaseMapper<PurchasePlan> {

    /** 今日计划单号最大流水（purchase_plan 与 purchase_request 共用 CG 号段，取两者较大值避免重号） */
    @Select("""
        SELECT GREATEST(
            IFNULL((SELECT MAX(CAST(RIGHT(plan_no, 4) AS UNSIGNED)) FROM mat_purchase_plan
                     WHERE plan_no LIKE CONCAT(#{prefix}, '%')), 0),
            IFNULL((SELECT MAX(CAST(RIGHT(pr_no, 4) AS UNSIGNED)) FROM purchase_request
                     WHERE pr_no LIKE CONCAT(#{prefix}, '%')), 0))""")
    int maxSeq(@Param("prefix") String prefix);
}
