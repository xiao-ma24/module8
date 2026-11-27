package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 采购订单表（purchase_order）— 单据号 PO+年月日+4位流水 */
@Data
@TableName("purchase_order")
public class PurchaseOrder {
    @TableId(type = IdType.AUTO)
    private Long poId;
    private String poNo;
    private Long planId;
    private Long prId;
    private Long supplierId;
    private Long materialId;
    private BigDecimal poQty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private LocalDate poDate;
    private Long buyerId;
    private BigDecimal acceptQty;
    private LocalDate orderDate;
    private LocalDate expectDate;
    private String contactPerson;
    private String contactPhone;
    private BigDecimal totalAmount;
    private Integer payType;      // 1货到付款 2预付 3月度结算
    private Integer invoiceType;  // 1专票 2普票 3无需发票
    private Integer poStatus;     // 0待确认 1已确认 2部分到货 3已完成 4已取消
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private String prNo;
    @TableField(exist = false)
    private String planNo;
    @TableField(exist = false)
    private String buyerName;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private BigDecimal supplierStar;
    /** 剩余可到货数量：订购量-已到货量，为 0 时不再允许到货登记 */
    @TableField(exist = false)
    private BigDecimal remainQty;
    /** 该供应商本物资历史成交均价（比价用） */
    @TableField(exist = false)
    private BigDecimal historyAvgPrice;
}
