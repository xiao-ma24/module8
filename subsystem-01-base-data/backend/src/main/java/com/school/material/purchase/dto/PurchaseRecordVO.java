package com.school.material.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 个人发票登记记录（查询结果）
 */
@Data
public class PurchaseRecordVO {

    private Long id;

    private String invoiceNo;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal qtyApply;

    private String supplierName;

    private String registrarUserId;

    private LocalDateTime registerTime;

    private String remark;

    /** 审核状态：0-待验收（待审核），1-已入库（已通过），2-已驳回 */
    private Integer auditStatus;

    /** 审核状态文本 */
    private String auditStatusText;

    /** 审核意见 */
    private String auditRemark;
}
