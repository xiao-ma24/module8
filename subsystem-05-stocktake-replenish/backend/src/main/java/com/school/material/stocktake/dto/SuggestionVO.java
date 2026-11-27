package com.school.material.stocktake.dto;

import lombok.Data;

import java.math.BigDecimal;

/** 补货建议条目：报告 2.5 节的 d / ROP / QS / QO 全部在此展开，便于前端与答辩展示 */
@Data
public class SuggestionVO {

    private Long materialId;
    private String materialCode;
    private String materialName;
    private String unit;
    private String categoryCode;

    /** 当前可用量 qa（只统计未过期、未冻结批次的 qty_available） */
    private BigDecimal qa;
    /** 在借量（个人保管仓尚未归还，补货时视同库存） */
    private BigDecimal onLoan;
    /** 在途量 qt（未完成采购订单已订购未到货数量） */
    private BigDecimal inTransit;
    /** 安全库存 S */
    private BigDecimal safeStock;
    /** 最近 90 天日均消耗 d */
    private BigDecimal daily;
    /** 采购提前期 L（天） */
    private Integer leadTimeDays;
    /** 覆盖天数 Dc */
    private Integer coverDays;
    /** 补货点 ROP = d × L + S */
    private BigDecimal rop;
    /** 建议补货量 QS = (Dc + L) × d + S − qa − qt */
    private BigDecimal suggestQty;
    /** 包装规格 P */
    private BigDecimal packSize;
    /** 取整后下单量 QO = ⌈QS ÷ P⌉ × P */
    private BigDecimal orderQty;

    private BigDecimal refPrice;
    private BigDecimal amount;

    private Long supplierId;
    private String supplierName;

    /** 触发来源：warning_rule.rule_id（为空表示按安全库存兜底） */
    private Long ruleId;
}
