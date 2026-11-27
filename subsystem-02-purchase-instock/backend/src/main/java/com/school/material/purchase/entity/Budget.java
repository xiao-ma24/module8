package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/** 预算科目（budget）— 第5组财务系统模拟表，本系统只读+冻结/扣减演示 */
@Data
@TableName("budget")
public class Budget {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String budgetNo;
    private String fundCardNo;
    private String subjectName;
    private BigDecimal balance;
    private Integer frozen;   // 已冻结额度
}
