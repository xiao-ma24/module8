package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import com.baomidou.mybatisplus.annotation.TableField;

/**
 * 部门（dept，只读）：annual_quota 年度领用额度、used_amount 本年度已用。
 * 领用出库时累加 used_amount，用于额度校验。
 */
@Data
@TableName("mat_dept")
public class Dept {

    @TableId(value = "org_id", type = IdType.INPUT)
    private String deptId;
    private String deptCode;
    private String deptName;
    private String parentId;
    private Long leaderId;
    private BigDecimal annualQuota;
    private BigDecimal usedAmount;
    private Integer sortNo;
    @TableField("dept_status")
    private Integer status;
    private String remark;
    @TableLogic
    private Integer delFlag;
}
