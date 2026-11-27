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
 * 报损单（loss_order，报告表2-17）。
 * source_type：1 借用转报损、2 盘亏报损、3 到期或超限报废。
 * approve_status：0 待审批、1 通过、2 驳回（与审批记录的 approve_result 口径不同）。
 * dispose_result：1 赔偿、2 免赔、3 报废核销。executed：0 未执行、1 已执行（防重复核销）。
 */
@Data
@TableName("mat_loss_order")
public class LossOrder {

    @TableId(value = "loss_id", type = IdType.AUTO)
    private Long lossId;
    private String lossNo;
    private Integer sourceType;
    private Long sourceId;
    private Long materialId;
    private String batchNo;
    private Long warehouseId;
    private Long holderId;
    private BigDecimal lossQty;
    private String reason;
    private Integer approveStatus;
    private Integer disposeResult;
    private Integer executed;
    private LocalDateTime execTime;
    private Long execOperatorId;

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
    private String borrowNo;
    @TableField(exist = false)
    private String borrowerName;
    @TableField(exist = false)
    private String deptId;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String execOperatorName;
    /** 验收结论（来自借用单 condition_code，仅展示用） */
    @TableField(exist = false)
    private Integer conditionCode;
}
