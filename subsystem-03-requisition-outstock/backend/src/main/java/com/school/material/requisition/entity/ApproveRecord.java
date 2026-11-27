package com.school.material.requisition.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 统一审批记录（approve_record，报告表2-13），跨模块共享控制数据。
 * biz_type：1 领用、2 借用、3 报损、4 采购、5 报废（本子系统只写 1）。
 * approve_result：0 驳回、1 通过；仅记录单次审批结论，单据进度看各单据表状态。
 */
@Data
@TableName("mat_approve_record")
public class ApproveRecord {

    @TableId(value = "record_id", type = IdType.AUTO)
    private Long recordId;
    private Integer bizType;
    private Long bizId;
    private Long approverId;
    private Integer approveResult;
    @TableField("approved_at")
    private LocalDateTime approveTime;
    private String opinion;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String approverName;
}
