package com.school.material.stocktake.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 预警规则（warning_rule）— 三档：1 安全库存 2 临期 3 到期报废。
 * warehouse_id = 0 表示适用于全部仓库（避免 NULL 使唯一索引失效）。
 */
@Data
@TableName("warning_rule")
public class WarningRule {

    @TableId(type = IdType.AUTO)
    private Long ruleId;

    private Long materialId;

    private Long warehouseId;

    /** 1 安全库存（低于下限预警） 2 临期 3 到期报废 */
    private Integer warningType;

    /** 安全库存下限 */
    private BigDecimal minQuantity;

    /** 库存上限（高于上限提示积压，可选） */
    private BigDecimal maxQuantity;

    /** 临期提前天数（warning_type = 2） */
    private Integer expireDays;

    /** 到期报废年限（warning_type = 3，为空取 material.service_life_years） */
    private Integer serviceLifeYears;

    /** 0 停用 1 启用 */
    private Integer enable;

    private String remark;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;

    @TableLogic
    private Integer delFlag;
}
