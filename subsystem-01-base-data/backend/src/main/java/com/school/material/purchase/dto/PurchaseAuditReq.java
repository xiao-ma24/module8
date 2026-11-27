package com.school.material.purchase.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 个人发票登记审核请求
 */
@Data
public class PurchaseAuditReq {

    @NotNull(message = "登记记录 id 不能为空")
    private Long id;

    /** 审核结果：1-通过（入库），2-驳回（不入库） */
    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    /** 审核人工号（→师资系统） */
    private String auditorUserId;

    /** 审核意见 */
    private String auditRemark;
}
