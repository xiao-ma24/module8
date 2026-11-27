package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 采购计划表（purchase_plan，报告表2-9）— 一计划一物资，单号 CG+年月日+4位流水 */
@Data
@TableName("mat_purchase_plan")
public class PurchasePlan {
    @TableId(type = IdType.AUTO)
    private Long planId;
    private String planNo;
    private Integer planType;        // 1常规补货 2临时采购
    private Integer purchaseMode;    // 1集中采购 2科研经费自购
    @TableField("org_id")
    private String deptId;
    private Long holderId;
    private String reimburseVoucherNo;
    private Long materialId;
    private BigDecimal planQty;
    private BigDecimal stockQty;
    private Long supplierId;
    private LocalDate expectDate;
    private Integer planStatus;      // 0草稿 1待审批 2已批准 3已下单 4已关闭
    private Long approveId;
    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;
}
