package com.school.material.purchase.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 采购发票入库登记请求
 */
@Data
public class PurchaseRegisterReq {

    @NotBlank(message = "发票号码不能为空")
    @Size(max = 32, message = "发票号码长度不能超过 32")
    private String invoiceNo;

    @NotNull(message = "物资 id 不能为空")
    private Long materialId;

    @NotNull(message = "入库仓库 id 不能为空")
    private Long warehouseId;

    @NotNull(message = "入库数量不能为空")
    @DecimalMin(value = "0.001", message = "入库数量必须大于 0")
    @JsonAlias("quantity")
    private BigDecimal qtyApply;

    @Size(max = 128, message = "供应商名称长度不能超过 128")
    private String supplierName;

    @NotBlank(message = "登记人 id 不能为空")
    @Size(max = 32, message = "登记人 id 长度不能超过 32")
    private String registrarUserId;

    @Size(max = 255, message = "备注长度不能超过 255")
    private String remark;
}
