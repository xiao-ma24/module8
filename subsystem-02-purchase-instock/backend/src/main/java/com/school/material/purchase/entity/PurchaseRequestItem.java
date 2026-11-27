package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;

/** 采购申请明细表（purchase_request_item） */
@Data
@TableName("purchase_request_item")
public class PurchaseRequestItem {
    @TableId(type = IdType.AUTO)
    private Long priId;
    private Long prId;
    private Long materialId;
    private BigDecimal qtyApply;
    private BigDecimal refPrice;
    private BigDecimal amount;
    private BigDecimal qtyConfirmed;   // 核准数量，默认=申请数量

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private Integer isControlled;
}
