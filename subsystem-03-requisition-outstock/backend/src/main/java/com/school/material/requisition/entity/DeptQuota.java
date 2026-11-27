package com.school.material.requisition.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 部门月度领用额度（material_dept_quota，报告表2-4）。
 * 联合主键 (dept_id, quota_period)，quota_period 形如 202611；按月核定额度，出库时累加 used_amount。
 * 由平台管理端「部门月度额度」页维护，本子系统只读 + 出库时累加已用额度。
 */
@Data
@TableName("material_dept_quota")
public class DeptQuota {

    private String deptId;
    private String quotaPeriod;
    private BigDecimal monthQuota;
    private BigDecimal usedAmount;
    private Long approveBy;
    private LocalDateTime updateTime;

    @TableField(exist = false)
    private String deptName;

    /** 剩余额度 = 月度额度 − 本月已用 */
    public BigDecimal left() {
        BigDecimal q = monthQuota == null ? BigDecimal.ZERO : monthQuota;
        BigDecimal u = usedAmount == null ? BigDecimal.ZERO : usedAmount;
        return q.subtract(u);
    }
}
