package com.school.material.borrow.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 借用归还（borrow_return，报告表2-16）：一单一物资，借用与归还同表。
 * borrow_status：0 待审批、1 待借出、2 在借、3 已归还、4 已报损、5 已核销。
 * 约束：returned_qty + written_off_qty <= borrow_qty。
 */
@Data
@TableName("mat_borrow_return")
public class BorrowReturn {

    @TableId(value = "borrow_id", type = IdType.AUTO)
    private Long borrowId;
    private String borrowNo;
    private Long borrowerId;
    private Long materialId;
    private Long warehouseId;
    private String batchNo;
    private BigDecimal borrowQty;
    private BigDecimal returnedQty;
    private BigDecimal writtenOffQty;
    private LocalDate dueDate;
    private LocalDateTime returnTime;
    private Integer conditionCode;
    private Integer borrowStatus;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    // ---- 列表展示用 ----
    @TableField(exist = false)
    private String borrowerName;
    @TableField(exist = false)
    private String borrowerNo;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String deptId;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private BigDecimal refPrice;
    @TableField(exist = false)
    private String warehouseName;
    @TableField(exist = false)
    private BigDecimal availableQty;
    /** 是否逾期（在借且已过预计归还日期，实时计算不落库） */
    @TableField(exist = false)
    private Boolean overdue;
    @TableField(exist = false)
    private Long overdueDays;
    @TableField(exist = false)
    private String outNo;
}
