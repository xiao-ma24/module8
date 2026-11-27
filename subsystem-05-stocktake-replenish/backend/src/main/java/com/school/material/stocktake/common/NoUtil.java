package com.school.material.stocktake.common;

/**
 * 单据号生成：前缀 + yyyyMMdd + 流水位数（报告表3-32）
 * PD 盘点单 4 位；CG 采购计划 4 位；YJ 预警 5 位。
 */
public class NoUtil {

    public static String today() {
        return java.time.LocalDate.now().toString().replace("-", "");
    }

    /** countToday = 今日已有单据数（调用方按前缀 + 日期模糊统计） */
    public static String next(String prefix, long countToday, int width) {
        return prefix + today() + String.format("%0" + width + "d", countToday + 1);
    }
}
