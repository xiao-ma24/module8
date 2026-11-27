package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/** 物资目录表（material）— 全组共享一级数据，本人负责 */
@Data
@TableName("mat_material")
public class Material {
    @TableId(type = IdType.AUTO)
    private Long materialId;
    private String materialCode;
    private String materialName;
    private String categoryCode;   // 01办公 02实验 03后勤 04通用
    private Integer materialType;  // 1低值易耗品 2低值耐用品
    private String unit;
    private BigDecimal refPrice;
    private Integer isControlled;  // 1是 0否
    private Integer serviceLifeYears; // 限用年限（低值耐用品）
    private Integer shelfLifeDays;    // 保质期天数（易耗品），实际到期日按批次维护
    private BigDecimal safeStock;
    @TableField(fill = FieldFill.INSERT)
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;
}
