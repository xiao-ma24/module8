package com.school.material.requisition.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 库存变动申请（查询/提交结果）
 */
@Data
public class ReqOrderVO {

    private Long id;

    private String applyNo;

    private Long materialId;

    private Long warehouseId;

    /** IN-入库，OUT-出库 */
    private String direction;

    private BigDecimal qtyApply;

    private String sourceNo;

    private String applicantUserId;

    /** 0-待审核，1-已通过，2-已驳回 */
    private Integer auditStatus;

    private String auditStatusText;

    private String auditRemark;

    private LocalDateTime applyTime;
}
