package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 出库单（out_order，报告表2-15）。借用借出也生成出库单（source_type=2），
 * 与领用出库共用一张表，便于按仓库/批次统一追溯。
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

    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private String keeperName;
    @TableField(exist = false)
    private String borrowNo;
    @TableField(exist = false)
    private String borrowerName;
}
