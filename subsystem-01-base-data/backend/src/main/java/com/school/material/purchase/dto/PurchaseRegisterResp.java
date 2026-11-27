package com.school.material.purchase.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 采购发票入库登记结果
 */
@Data
public class PurchaseRegisterResp {

    private Long id;

    private String invoiceNo;

    private Long materialId;

    private Long warehouseId;

    private BigDecimal qtyApply;

    private String supplierName;

    private String registrarUserId;

    private LocalDateTime registerTime;

    /** 该物资在该仓库的当前在库总量 qty_on_hand（登记阶段不变更库存） */
    private BigDecimal qtyOnHand;

    /** 审核状态：0-待验收，1-已入库，2-已驳回 */
    private Integer auditStatus;

    /** true 表示该发票的该物资行已登记过，本次未重复登记 */
    private boolean duplicated;
}
