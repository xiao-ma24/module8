package com.school.material.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存列表项（关联物资、仓库）
 */
@Data
public class StockVO {

    private Long materialId;

    private Long warehouseId;

    /** 个人保管人：非空表示个人保管库存，不计入公共可用 */
    private Long holderId;

    private String batchNo;

    private BigDecimal qtyOnHand;

    private BigDecimal qtyAvailable;

    private BigDecimal qtyLocked;

    private LocalDate inboundDate;

    private String materialName;

    private String materialCode;

    private String warehouseName;

    private BigDecimal safeStock;
}
