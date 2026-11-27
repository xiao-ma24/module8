package com.school.material.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资列表/详情响应
 */
@Data
public class MaterialVO {

    private Long id;

    private String materialNo;

    private String materialName;

    private String categoryCode;

    private String categoryName;

    private Integer materialType;

    private String unit;

    private BigDecimal referencePrice;

    private Integer isControlled;

    private Integer serviceLifeYears;

    private Integer shelfLifeDays;

    private BigDecimal safeStock;

    private LocalDateTime createdAt;
}
