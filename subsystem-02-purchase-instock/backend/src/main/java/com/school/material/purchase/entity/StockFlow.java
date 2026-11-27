package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 库存流水表（stock_flow）— 金日勋负责。入库写入 biz_type=1 */
@Data
@TableName("mat_stock_flow")
public class StockFlow {
    @TableId(type = IdType.AUTO)
    private Long flowId;
    private Long materialId;
    private Long warehouseId;
    private String batchNo;
    private Integer bizType;   // 1入库 2领用 3借用 4归还 5盘盈 6盘亏 7报废核销
    private Long bizId;
    private Long bizItemId;    // 来源单据明细ID：整单多明细/分批出库/分次归还的幂等标识
    private BigDecimal changeQty;  // 正入负出；借用/归还不计入在库总量增减
    private Long operatorId;
    private LocalDateTime flowTime;
}
