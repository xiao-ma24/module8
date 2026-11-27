package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 入库单表（inbound_order）— 单据号 RK+年月日+4位流水 */
@Data
@TableName("inbound_order")
public class InboundOrder {
    @TableId(type = IdType.AUTO)
    private Long inId;
    private String inNo;
    private Long acptId;
    private Long poId;
    private Long warehouseId;
    private Long keeperId;
    private Integer purchaseMode;  // 1集中采购 2科研经费自购
    private Long holderId;         // 科研经费自购的实际保管教师
    private LocalDateTime inDate;
    private BigDecimal totalAmount;
    private String voucherNo;   // 第5组财务报销凭证号
    private Integer inStatus;   // 0待入库 1已入库
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String inNoDisplay;
    @TableField(exist = false)
    private String poNo;
    @TableField(exist = false)
    private String acptNo;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private Integer warehouseType;   // 1实体仓库 2虚拟个人保管仓
    @TableField(exist = false)
    private String holderName;        // 科研经费自购的实际保管教师
    @TableField(exist = false)
    private String keeperName;
    @TableField(exist = false)
    private String supplierName;
}
