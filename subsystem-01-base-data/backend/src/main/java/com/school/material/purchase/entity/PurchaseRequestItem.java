package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购申请明细（团队 purchase_request_item 表）
 */
@Data
@TableName("purchase_request_item")
public class PurchaseRequestItem {

    @TableId(value = "pri_id", type = IdType.AUTO)
    private Long priId;

    private Long prId;

    private Long materialId;

    private BigDecimal qtyApply;

    private BigDecimal refPrice;

    private BigDecimal amount;

    private BigDecimal qtyConfirmed;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
