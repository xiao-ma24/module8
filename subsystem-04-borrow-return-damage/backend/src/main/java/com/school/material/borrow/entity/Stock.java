package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * 库存（stock，只读）：最小单元为「物资 + 仓库 + 批次」。
 * 写入一律通过子系统② InventoryService，本子系统只查询（可用量校验、先到期先出选批）。
 */
@Data
@TableName("mat_stock")
public class Stock {

    private Long materialId;
    private Long warehouseId;
    private Long holderId;
    private String batchNo;
    private LocalDate expireDate;
    private LocalDate inboundDate;
    private BigDecimal qtyOnHand;
    private BigDecimal qtyAvailable;
    private BigDecimal qtyLocked;

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private Integer warehouseType;
}
