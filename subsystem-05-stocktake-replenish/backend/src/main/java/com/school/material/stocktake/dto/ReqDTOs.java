package com.school.material.stocktake.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/** 子系统⑤ 请求对象集合（Req 后缀，与 controller 一一对应） */
public class ReqDTOs {

    /** 创建盘点单：按仓库 + 物资集合生成账面快照 */
    @Data
    public static class CreateOrder {
        private Long warehouseId;
        private LocalDate stocktakeDate;
        private Long auditorId;
        private String remark;
        /** 为空表示按该仓库全部在库物资建单 */
        private List<Long> materialIds;
        private Long operatorId;
    }

    /** 录入实盘：差异超过账面 10% 时 confirmOver10 必须为 true（二次录入确认） */
    @Data
    public static class Entry {
        private Long itemId;
        private BigDecimal actualQuantity;
        private String diffReason;
        private Boolean confirmOver10;
        private Long operatorId;
    }

    /** 完成盘点并调账 */
    @Data
    public static class Finish {
        private Long operatorId;
        private String opinion;
    }

    /** 保存预警规则 */
    @Data
    public static class RuleSave {
        private Long ruleId;
        private Long materialId;
        private Long warehouseId;
        private Integer warningType;
        private BigDecimal minQuantity;
        private BigDecimal maxQuantity;
        private Integer expireDays;
        private Integer serviceLifeYears;
        private Integer enable;
        private String remark;
        private Long operatorId;
    }

    /** 由补货建议生成采购计划（复用 purchase_plan，plan_type=1 常规补货） */
    @Data
    public static class PlanCreate {
        private List<PlanLine> lines;
        private String deptId;
        private Long operatorId;
    }

    @Data
    public static class PlanLine {
        private Long materialId;
        private BigDecimal planQty;
        private BigDecimal stockQty;
        private BigDecimal suggestQty;
        private BigDecimal rop;
        private BigDecimal daily;
        private Long supplierId;
        private LocalDate expectDate;
    }

    /** 采购计划审批：通过 → 已批准(2)，驳回 → 已关闭(4) */
    @Data
    public static class PlanApprove {
        private List<Long> planIds;
        private Long approverId;
        private Boolean pass;
        private String opinion;
    }
}
