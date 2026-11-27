package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/** 部门表（dept）— 韦森宁负责，dept_id 来源第4组师资系统，本系统只读 */
@Data
@TableName("mat_dept")
public class Dept {
    @TableId(value = "org_id", type = IdType.INPUT)
    private String deptId;
    private String deptName;
    @TableField("leader_id")
    private Long managerId;
    private BigDecimal annualQuota;
    private BigDecimal usedAmount;
    @TableLogic
    private Integer delFlag;
}
