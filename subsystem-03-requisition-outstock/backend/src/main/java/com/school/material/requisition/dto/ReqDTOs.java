package com.school.material.requisition.dto;

import java.math.BigDecimal;

/** 子系统③ 请求/响应对象 */
public final class ReqDTOs {

    /** 新建领用申请（一单一物资） */
    public record Create(Long materialId, BigDecimal qtyApply, Boolean submit) {
    }

    /** 审批：result=1 通过（可部分批准）、0 驳回；opinion 驳回必填 */
    public record Approve(Integer result, BigDecimal qtyApprove, String opinion) {
    }

    /** 出库确认：库管员选定仓库与批次，实发数量可小于核准数量 */
    public record Outbound(Long reqId, Long warehouseId, String batchNo, BigDecimal outQty) {
    }
}
