package com.school.material.borrow.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 子系统④ 请求/响应对象 */
public final class BorrowDTOs {

    /** 新建借用申请（一单一物资） */
    public record Create(Long materialId, BigDecimal borrowQty, LocalDate dueDate) {
    }

    /** 借用审批：result=1 通过、0 驳回；opinion 驳回必填 */
    public record Approve(Integer result, String opinion) {
    }

    /** 借出登记：库管员按先到期先出选定仓库与批次 */
    public record Lend(Long borrowId, Long warehouseId, String batchNo) {
    }

    /**
     * 归还验收（支持分次、部分归还）：
     * acceptResult 1 完好入库、2 损坏核销、3 缺失核销；remark 非完好时必填。
     */
    public record ReturnReq(Long borrowId, BigDecimal returnQty, Integer acceptResult, String remark) {
    }

    /** 报损审批：result=1 通过、2 驳回；disposeResult 1 赔偿、2 免赔、3 报废核销 */
    public record LossApprove(Integer result, Integer disposeResult, String opinion) {
    }

    /** 报损核销执行 */
    public record LossExecute(Integer disposeResult) {
    }
}
