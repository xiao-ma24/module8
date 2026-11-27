package com.school.material.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存变更请求（幂等键：sourceNo）
 */
@Data
public class StockChangeReq {

    @NotNull(message = "物资 id 不能为空")
    private Long materialId;

    @NotNull(message = "仓库 id 不能为空")
    private Long warehouseId;

    @NotBlank(message = "库存变更方向不能为空")
    private String direction;

    /** 本次变动量：入库时同时增加 qty_on_hand / qty_available，出库时同时扣减 */
    @NotNull(message = "变更数量不能为空")
    @JsonAlias("quantity")
    private BigDecimal num;

    @NotBlank(message = "来源单据号不能为空")
    @Size(max = 32, message = "来源单据号长度不能超过 32")
    private String sourceNo;

    /** 批次号，可空：入库默认 DEFAULT，出库默认为空时按先进先出选批次 */
    @Size(max = 32, message = "批次号长度不能超过 32")
    private String batchNo;
}
