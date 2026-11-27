package com.school.material.purchase.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;
import java.time.LocalDateTime;

/** 审批记录表（approve_record）— 韦森宁负责。采购审批 biz_type=4 */
@Data
@TableName("mat_approve_record")
public class ApproveLog {
    @TableId(value = "record_id", type = IdType.AUTO)
    private Long logId;
    private Integer bizType;   // 1领用 2借用 3报损 4采购申请（本系统扩展）
    private Long bizId;
    private Long approverId;
    private Integer approveResult;   // 0驳回 1通过（数据字典 approve_result）
    @TableField("approved_at")
    private LocalDateTime approveTime;
    private String opinion;

    @TableField(exist = false)
    private String approverName;
}
