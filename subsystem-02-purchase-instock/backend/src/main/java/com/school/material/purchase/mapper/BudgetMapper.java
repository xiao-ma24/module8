package com.school.material.purchase.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.school.material.purchase.entity.Budget;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import java.math.BigDecimal;

/** 第5组财务系统模拟：开发期本地库，合并后替换为真实财务接口 */
public interface BudgetMapper extends BaseMapper<Budget> {

    /** 冻结额度（提交审批时）：校验可用余额充足后冻结 */
    @Update("""
        UPDATE budget SET frozen = IFNULL(frozen,0) + #{amt}
        WHERE budget_no = #{budgetNo} AND (IFNULL(balance,0) - IFNULL(frozen,0)) >= #{amt}""")
    int freeze(@Param("budgetNo") String budgetNo, @Param("amt") BigDecimal amt);

    /** 解冻（驳回/取消时） */
    @Update("UPDATE budget SET frozen = IFNULL(frozen,0) - #{amt} WHERE budget_no = #{budgetNo}")
    int unfreeze(@Param("budgetNo") String budgetNo, @Param("amt") BigDecimal amt);

    /** 确认扣减（入库完成时）：余额减、冻结同步释放 */
    @Update("UPDATE budget SET balance = balance - #{amt}, frozen = IFNULL(frozen,0) - #{amt} WHERE budget_no = #{budgetNo}")
    int consume(@Param("budgetNo") String budgetNo, @Param("amt") BigDecimal amt);
}
