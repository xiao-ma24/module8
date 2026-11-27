package com.school.material.stocktake.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 预警清单条目（预警不落库，由规则 + 实时库存计算得出，只提示不改库存） */
@Data
public class WarningVO {

    /** 1 低于补货点/安全库存 2 临期 3 已过期 4 到期报废 */
    private Integer warningType;
    private String warningLabel;

    /** 高 / 中 */
    private String level;

    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;

    private Long warehouseId;
    private String warehouseName;

    private String batchNo;
    private LocalDate expireDate;

    /** 当可用量（或剩余天数） */
    private BigDecimal currentQty;

    /** 阈值（补货点，或临期天数） */
    private BigDecimal threshold;

    private String message;

    /** 该批次是否已有已完成的盘点单：true 表示已盘点，前端不再引导重复盘点 */
    private Boolean stocktakeHandled;

    /** 最近一张覆盖该批次的已完成盘点单号 */
    private String stocktakeNo;
}
