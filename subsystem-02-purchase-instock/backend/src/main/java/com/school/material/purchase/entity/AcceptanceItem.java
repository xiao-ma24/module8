package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 验收明细表（acceptance_item） */
@Data
@TableName("acceptance_item")
public class AcceptanceItem {
    @TableId(type = IdType.AUTO)
    private Long aiId;
    private Long acptId;
    private Long poiId;
    private Long materialId;
    private BigDecimal checkQty;
    private BigDecimal passQty;
    private BigDecimal failQty;
    private String failReason;
    private Integer handleType;     // 1入库 2退货 3换货
    private Integer handleStatus;   // 0待处理 1已完成
    private LocalDateTime handledAt;
    private String handledBy;

    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private BigDecimal unitPrice;
    @TableField(exist = false)
    private Integer isControlled;

    // 退换货跟踪列表用
    @TableField(exist = false)
    private String acptNo;
    @TableField(exist = false)
    private String poNo;
    @TableField(exist = false)
    private String supplierName;
    @TableField(exist = false)
    private LocalDate acptDate;
}
