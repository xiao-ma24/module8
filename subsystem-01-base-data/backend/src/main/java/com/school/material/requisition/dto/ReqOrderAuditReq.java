package com.school.material.requisition.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 库存变动申请审核请求
 */
@Data
public class ReqOrderAuditReq {

    @NotNull(message = "申请单 id 不能为空")
    private Long id;

    /** 审核结果：1-通过（变更库存），2-驳回（不变更库存） */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    /** 审核人工号（→师资系统） */
    @Size(max = 32, message = "审核人 id 长度不能超过 32")
    private String auditorUserId;

    /** 审核意见 */
    @Size(max = 255, message = "审核意见长度不能超过 255")
    private String auditRemark;
}
