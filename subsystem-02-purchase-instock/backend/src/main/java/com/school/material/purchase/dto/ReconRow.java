package com.school.material.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 财务核对行：读取入库成本接口表（material_inbound_interface）的实际入库单价与金额 */
@Data
public class ReconRow {
    private String inNo;
    private LocalDate inDate;
    private String poNo;
    private Long supplierId;
    private String supplierName;
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private BigDecimal qty;
    private BigDecimal inPrice;
    private BigDecimal inAmount;
    private String warehouseName;
    private String operatorName;
}
