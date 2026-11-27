package com.school.material.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 物资新增/修改请求
 */
@Data
public class MaterialReq {

    @NotBlank(message = "物资编码不能为空")
    @Size(max = 32, message = "物资编码长度不能超过 32")
    private String materialNo;

    @NotBlank(message = "物资名称不能为空")
    @Size(max = 80, message = "物资名称长度不能超过 80")
    private String materialName;

    @NotBlank(message = "分类编码不能为空")
    @Size(max = 8, message = "分类编码长度不能超过 8")
    private String categoryCode;

    /** 物资类型：1低值易耗品 2低值耐用品，默认 1 */
    private Integer materialType;

    @NotBlank(message = "计量单位不能为空")
    @Size(max = 10, message = "计量单位长度不能超过 10")
    private String unit;

    @NotNull(message = "参考单价不能为空")
    @DecimalMin(value = "0", message = "参考单价不能小于 0")
    private BigDecimal referencePrice;

    /** 1管控物资 0普通物资，默认 0 */
    private Integer isControlled;

    /** 限用年限（年），低值耐用品必填 */
    private Integer serviceLifeYears;

    /** 保质期天数，需保质期管理的易耗品填写 */
    private Integer shelfLifeDays;

    @DecimalMin(value = "0", message = "安全库存不能小于 0")
    private BigDecimal safeStock;
}
