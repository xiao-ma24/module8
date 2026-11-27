package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;

/** 库存表（stock）— 按物资、仓库、批次维护；个人保管库存不参与公共领用。 */
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
    private Integer warehouseType;   // 1实体仓库 2虚拟个人保管仓
    @TableField(exist = false)
    private String holderName;        // 个人保管仓的实际保管教师
    @TableField(exist = false)
    private BigDecimal safeStock;
    @TableField(exist = false)
    private BigDecimal monthConsumed;  // 近30天消耗（补货建议用）
    @TableField(exist = false)
    private BigDecimal inTransit;      // 在途量：未完成采购订单已订购未到货（补货模型 q_t）
}
