package com.school.material.dto;

import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存变更结果
 */
@Data
public class StockChangeResp {

    private Long materialId;
    private Long warehouseId;
    private String direction;
    /** 本次变更数量 */
    private BigDecimal changeQty;
    /** 变更后该批次在库总量 */
    private BigDecimal qtyOnHand;
    /** 变更后该批次可领量 */
    private BigDecimal qtyAvailable;
    private String sourceNo;
    /** true 表示该 sourceNo 已处理过（幂等命中），本次未重复执行 */
    private boolean duplicated;
}
