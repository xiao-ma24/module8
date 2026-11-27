package com.school.material.requisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单（out_order，报告表2-15）。
 * source_type：1 领用、2 借用、3 调拨（预留）。本子系统只产生 source_type=1。
 * batch_no：库管员按「先到期先出」选择的库存批次。
 */
@Data
@TableName("mat_out_order")
public class OutOrder {

    @TableId(value = "out_id", type = IdType.AUTO)
    private Long outId;
    private String outNo;
    private Integer sourceType;
    private Long sourceId;
    private Long warehouseId;
    private Long keeperId;
    private Long holderId;
    private Long materialId;
    private String batchNo;
    private BigDecimal outQty;
    private LocalDateTime outTime;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    // ---- 列表展示用 ----
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String keeperName;
    @TableField(exist = false)
    private String reqNo;
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private Integer reqStatus;
}
