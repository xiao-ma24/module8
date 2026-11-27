package com.school.material.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 仓库新增/修改请求
 */
@Data
public class WarehouseReq {

    @NotBlank(message = "仓库名称不能为空")
    @Size(max = 64, message = "仓库名称长度不能超过 64")
    private String warehouseName;

    /** 1中心库 2科室库，默认 1 */
    private Integer warehouseType;

    @Size(max = 255, message = "位置长度不能超过 255")
    private String location;

    /** 保管人 user_id */
    private Long keeperId;
}
