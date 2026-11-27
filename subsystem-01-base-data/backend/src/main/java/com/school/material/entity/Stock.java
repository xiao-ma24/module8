package com.school.material.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 库存（团队 stock 表，联合主键 material_id + warehouse_id + batch_no，无自增 id）
 */
@Data
@TableName("mat_stock")
public class Stock {

    private Long materialId;

    private Long warehouseId;

    /** 实际保管人 */
    private Long holderId;

    /** 库存批次号 */
    private String batchNo;

    private LocalDate expireDate;

    /** 验收登记日期 */
    private LocalDate inboundDate;

    /** 在库总量 */
    private BigDecimal qtyOnHand;

    /** 可领量 */
    private BigDecimal qtyAvailable;

    /** 预占数量 */
    private BigDecimal qtyLocked;

    private Long createdBy;

    private LocalDateTime createdAt;

    private Long updatedBy;

    private LocalDateTime updatedAt;

    private Integer delFlag;
}
