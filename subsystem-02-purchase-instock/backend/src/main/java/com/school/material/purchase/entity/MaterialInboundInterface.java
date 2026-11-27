package com.school.material.purchase.entity;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 物资入库成本接口表（material_inbound_interface，报告表2-11）：供第5组财务按实际入库单价成本结转 */
@Data
public class MaterialInboundInterface {
    private String inNo;
    private Long materialId;
    private BigDecimal inPrice;
    private BigDecimal inQty;
    private BigDecimal inAmount;
    private LocalDate inDate;
    private Long warehouseId;
}
