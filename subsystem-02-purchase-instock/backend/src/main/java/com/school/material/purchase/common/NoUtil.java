package com.school.material.purchase.common;

/** 单据号生成：前缀 + yyyyMMdd + 4位流水（CG计划/申请 PO订单 YS验收 RK入库 FP凭证） */
public class NoUtil {

    public static String today() {
        return java.time.LocalDate.now().toString().replace("-", "");
    }

    /** count = 今日已有单据数（调用方按前缀+日期模糊统计） */
    public static String next(String prefix, long countToday) {
        return prefix + today() + String.format("%04d", countToday + 1);
    }
}
