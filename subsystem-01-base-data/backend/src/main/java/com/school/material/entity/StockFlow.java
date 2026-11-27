package com.school.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存流水（团队 stock_flow 表，append-only）
 * 幂等唯一键：(biz_type, biz_id, biz_item_id)
 */
@Data
@TableName("mat_stock_flow")
public class StockFlow {

    @TableId(value = "flow_id", type = IdType.AUTO)
    private Long flowId;

    private Long materialId;

    private Long warehouseId;

    private String batchNo;

    /** 1入库 2领用 3借用 4归还 5盘盈 6盘亏 7报废核销 */
    private Integer bizType;

    /** 业务单据 id */
    private Long bizId;

    /** 来源单据明细 id，无明细时填 0 */
    private Long bizItemId;

    /** 正入负出 */
    private BigDecimal changeQty;

    /** 操作人，0 表示系统 */
    private Long operatorId;

    private LocalDateTime flowTime;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
