package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/** 采购订单明细表（purchase_order_item） */
@Data
@TableName("purchase_order_item")
public class PurchaseOrderItem {
    @TableId(type = IdType.AUTO)
    private Long poiId;
    private Long poId;
    private Long planId;
    private Long materialId;
    private BigDecimal orderQty;
    private BigDecimal unitPrice;
    private BigDecimal amount;
    private BigDecimal arrivedQty;   // 累计到货
    private BigDecimal inboundQty;   // 累计入库

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private BigDecimal refPrice;
    @TableField(exist = false)
    private BigDecimal historyAvgPrice;
    @TableField(exist = false)
    private Integer isControlled;
}
