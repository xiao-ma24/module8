package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 采购申请表（purchase_request）— 单据号 CG+年月日+4位流水 */
@Data
@TableName("purchase_request")
public class PurchaseRequest {
    @TableId(type = IdType.AUTO)
    private Long prId;
    private String prNo;
    private Integer sourceType;    // 1部门申购 2领用转申购 3安全库存预警 4实验室自备
    private Integer purchaseMode;  // 1集中采购入公共库存 2科研经费自购登记入个人保管库存
    private Long sourceId;
    private Long applicantId;
    private Long deptId;
    private Long holderId;         // 科研经费自购的实际保管教师
    private String reimburseVoucherNo; // 自购验收登记后生成的报销凭证号
    private String usagePurpose;
    private String budgetNo;
    private LocalDate expectDate;
    private BigDecimal totalAmount;
    private Integer priority;     // 1普通 2加急
    private Integer prStatus;     // 0草稿 1待审批 2已批准 3已驳回 4已转订单
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String budgetSubjectName;
    @TableField(exist = false)
    private String sourceTypeName;
}
