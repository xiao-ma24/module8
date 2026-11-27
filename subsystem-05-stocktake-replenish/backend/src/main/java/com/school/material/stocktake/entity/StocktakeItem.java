package com.school.material.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 盘点明细（stocktake_item）— 按批次记录账面、实盘与差异，差异审批后才调库存 */
@Data
@TableName("stocktake_item")
public class StocktakeItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    private Long stocktakeId;

    private Long materialId;

    /** 库存批次号，与物资、仓库共同定位批次 */
    private String batchNo;

    /** 账面数量：建单时从 stock.qty_on_hand 快照，之后出库不影响本次差异 */
    private BigDecimal bookQuantity;

    private BigDecimal actualQuantity;

    /** 差异数量 = 实盘 - 账面，正盘盈负盘亏 */
    private BigDecimal diffQuantity;

    private String diffReason;

    /** 是否已录入实盘：0 未录入 1 已录入（实际数量默认 0，靠该标记区分“未录”与“录了 0”） */
    private Integer counted;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer delFlag;
}
