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
 * 借用分次归还明细（borrow_return_item）。
 * accept_result：1 完好入库、2 损坏核销、3 缺失核销。
 * 完好入库恢复可用量；损坏/缺失转报损登记，经审批后核销在库量。
 */
@Data
@TableName("borrow_return_item")
public class BorrowReturnItem {

    @TableId(value = "item_id", type = IdType.AUTO)
    private Long itemId;
    private Long borrowId;
    private String itemNo;
    private BigDecimal returnQty;
    private Integer acceptResult;
    private Long warehouseId;
    private String batchNo;
    private Long keeperId;
    private LocalDateTime returnTime;
    private String remark;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String keeperName;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String unit;
}
