package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 采购申请主表（团队 purchase_request 表）
 * 扩展列：warehouse_id、supplier_name、audit_remark、registrar_user_no
 */
@Data
@TableName("purchase_request")
public class PurchaseRequest {

    @TableId(value = "pr_id", type = IdType.AUTO)
    private Long prId;

    /** 采购申请单号 CG+yyyyMMdd+四位流水 */
    private String prNo;

    /** 1部门申购 2领用转申购 3安全库存预警 4实验室自备 */
    private Integer sourceType;

    /** 1集中采购 2科研经费自购登记 */
    private Integer purchaseMode;

    private Long sourceId;

    private Long applicantId;

    private Long deptId;

    private Long holderId;

    /** 发票号（唯一，作为子系统①登记幂等键） */
    private String reimburseVoucherNo;

    private String usagePurpose;

    private String budgetNo;

    private LocalDate expectDate;

    private BigDecimal totalAmount;

    private Integer priority;

    /** 1待验收 2已入库 3已驳回 */
    private Integer prStatus;

    private String remark;

    /** 子系统①扩展：验收入库仓库 */
    private Long warehouseId;

    /** 子系统①扩展：供应商名称 */
    private String supplierName;

    /** 子系统①扩展：登记人工号 */
    private String registrarUserNo;

    /** 子系统①扩展：验收驳回意见 */
    private String auditRemark;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
