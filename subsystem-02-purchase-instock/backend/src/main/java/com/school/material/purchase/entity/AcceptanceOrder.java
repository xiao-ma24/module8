package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** 验收单表（acceptance_order）— 单据号 YS+年月日+4位流水 */
@Data
@TableName("acceptance_order")
public class AcceptanceOrder {
    @TableId(type = IdType.AUTO)
    private Long acptId;
    private String acptNo;
    private Long poId;
    private LocalDate acptDate;
    private Long inspectorId;
    private String deliverer;
    private String photoUrls;
    private Integer acptScore;    // 1-5 验收评分
    private Integer acptStatus;   // 0待验收 1验收中 2已完成 3质量异议
    private String qualityReport;
    private String remark;
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    @TableLogic
    private Integer delFlag;

    @TableField(exist = false)
    private String poNo;
    @TableField(exist = false)
    private String inspectorName;
    @TableField(exist = false)
    private String supplierName;
}
