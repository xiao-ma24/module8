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
 * 领用单（req_order，报告表2-14）：一单一物资。
 * req_status：0 草稿、1 待审批、2 审批中、3 已批准、4 已驳回、5 已出库、6 已完成、7 已关闭。
 * 仅 3 及以后状态允许出库。
 */
@Data
@TableName("mat_req_order")
public class ReqOrder {

    @TableId(value = "req_id", type = IdType.AUTO)
    private Long reqId;
    private String reqNo;
    private Long applicantId;
    @TableField("org_id")
    private String deptId;
    private Long materialId;
    private BigDecimal qtyApply;
    private BigDecimal qtyApprove;
    private BigDecimal amount;
    private Integer reqStatus;

    private Long createdBy;
    private LocalDateTime createdAt;
    private Long updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    // ---- 列表展示用（不落库） ----
    @TableField(exist = false)
    private String applicantName;
    @TableField(exist = false)
    private String applicantNo;
    @TableField(exist = false)
    private String deptName;
    @TableField(exist = false)
    private String materialName;
    @TableField(exist = false)
    private String materialCode;
    @TableField(exist = false)
    private String unit;
    @TableField(exist = false)
    private BigDecimal refPrice;
    @TableField(exist = false)
    private Integer materialType;
    @TableField(exist = false)
    private Integer isControlled;
    @TableField(exist = false)
    private BigDecimal availableQty;
    @TableField(exist = false)
    private String approverName;
    @TableField(exist = false)
    private LocalDateTime approveTime;
    @TableField(exist = false)
    private String approveOpinion;
    @TableField(exist = false)
    private String outNo;
}
