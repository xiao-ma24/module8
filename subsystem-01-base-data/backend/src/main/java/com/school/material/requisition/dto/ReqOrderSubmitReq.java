package com.school.material.requisition.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 库存变动申请提交请求
 */
@Data
public class ReqOrderSubmitReq {

    @NotNull(message = "物资 id 不能为空")
    private Long materialId;

    @NotNull(message = "仓库 id 不能为空")
    private Long warehouseId;

    /** 变动方向：IN-入库，OUT-出库 */
    @NotBlank(message = "变动方向不能为空")
    private String direction;

    @NotNull(message = "申请数量不能为空")
    @JsonAlias("quantity")
    private BigDecimal qtyApply;

    /** 来源单号，仅透传返回 */
    @Size(max = 32, message = "来源单号长度不能超过 32")
    private String sourceNo;

    /** 申请人 id（工号，→师资系统） */
    @NotBlank(message = "申请人不能为空")
    @Size(max = 32, message = "申请人 id 长度不能超过 32")
    private String applicantUserId;

    /** 批次号，可空，默认 DEFAULT */
    @Size(max = 32, message = "批次号长度不能超过 32")
    private String batchNo;
}
