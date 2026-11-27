package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 部门月度领用额度（报告表2-4 material_dept_quota，联合主键 dept_id + quota_period） */
@Data
@TableName("material_dept_quota")
public class DeptQuota {
    private String deptId;
    /** 额度所属年月，如 202611 */
    private String quotaPeriod;
    /** 月度领用额度（元），由财务预算年度额度 ÷ 12 得到 */
    private BigDecimal monthQuota;
    /** 本月已用额度（元），出库时累加 */
    private BigDecimal usedAmount;
    /** 额度核定人，关联 sys_user */
    private Long approveBy;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String deptCode;
    @TableField(exist = false)
    private String approveName;
    @TableField(exist = false)
    private Integer deptSortNo;
    /** 财务预算表中该部门的年度额度（元），只读展示 */
    @TableField(exist = false)
    private BigDecimal annualBudget;
}
