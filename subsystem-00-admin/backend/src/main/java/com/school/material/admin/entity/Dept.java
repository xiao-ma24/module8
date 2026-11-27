package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;

/** 部门表（全平台共用，org_id 为第4组师资系统 6 位组织编码） */
@Data
@TableName("mat_dept")
public class Dept {
    @TableId(value = "org_id", type = IdType.INPUT)
    private String deptId;
    private String deptName;
    private String deptCode;
    private String parentId;
    @TableField("leader_id")
    private Long managerId;
    /** 部门年度领用额度（数据字典 annual_quota） */
    private BigDecimal annualQuota;
    /** 本年度已发生领用金额（数据字典 used_amount） */
    private BigDecimal usedAmount;
    private Integer sortNo;
    @TableField("dept_status")
    private Integer deptStatus;
    private String remark;

    @TableField(exist = false)
    private String managerName;
    @TableField(exist = false)
    private Integer userCount;
}
