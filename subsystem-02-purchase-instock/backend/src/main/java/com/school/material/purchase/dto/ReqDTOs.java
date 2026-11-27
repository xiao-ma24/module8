package com.school.material.purchase.dto;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 请求体集合：嵌套静态类，避免文件爆炸 */
public class ReqDTOs {

    /** 创建采购申请 */
    @Data
    public static class PrCreate {
        private Long applicantId;
        private Long deptId;
        private String usagePurpose;
        private String budgetNo;
        private LocalDate expectDate;
        private Integer priority;
        private Integer sourceType;
        private Integer purchaseMode;  // 1集中采购 2科研经费自购
        private Long holderId;         // 自购教师，个人保管库存责任人
        private Long sourceId;
        private String remark;
        private List<Item> items;

        @Data
        public static class Item {
            private Long materialId;
            private BigDecimal qtyApply;
        }
    }

    /** 审批 */
    @Data
    public static class Approve {
        private Long approverId;
        private Integer action;   // 1同意 2驳回
        private String opinion;
    }

    /** 审批时部分核准 */
    @Data
    public static class ConfirmItem {
        private Long priId;
        private BigDecimal qtyConfirmed;
    }

    /** 生成采购订单（从申请单转） */
    @Data
    public static class ToOrder {
        private Long supplierId;
        private Long buyerId;      // 采购员，关联 sys_user
        private Integer payType;
        private Integer invoiceType;
        private LocalDate expectDate;
        private String remark;
        private List<PriceItem> prices;

        @Data
        public static class PriceItem {
            private Long priId;       // 申请明细，定位物资
            private BigDecimal unitPrice;  // 成交单价
        }
    }

    /** 创建验收单 */
    @Data
    public static class AcptCreate {
        private Long poId;
        private Long inspectorId;
        private String deliverer;
        private String photoUrls;
        private List<AcptItem> items;

        @Data
        public static class AcptItem {
            private Long poiId;
            private BigDecimal checkQty;
            private BigDecimal passQty;
            private String failReason;
            private Integer handleType;
        }
    }

    /** 完成验收：评分 */
    @Data
    public static class AcptComplete {
        private Integer acptScore;
        private Long warehouseId;
        private Long keeperId;
        /** 当前执行验收的人（登录人）；为空时回落到验收单登记人 */
        private Long operatorId;
        /** 验收人填写的验收情况说明（必填） */
        private String report;
        /** 验收人核验的明细（送检/合格/不合格），到货登记阶段只登记到货数量 */
        private List<AcptItemCheck> items;

        @Data
        public static class AcptItemCheck {
            private Long itemId;
            private BigDecimal checkQty;
            private BigDecimal passQty;
            private Integer handleType;
            private String failReason;
        }
    }

    /** 入库明细补充：批次/质保期/二维码 */
    @Data
    public static class InboundExtra {
        private Long iiId;
        private String batchNo;
        private LocalDate produceDate;
        private LocalDate expireDate;
    }

    /** 财务回填凭证号（模拟第5组回调） */
    @Data
    public static class VoucherFill {
        private String voucherNo;
    }
}
