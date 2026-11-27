package com.school.material.borrow.common;

/**
 * 单据号生成：前缀 + yyyyMMdd + 4位流水。
 * 本子系统使用：LY 领用单、CK 出库单；归还 GH、报损 BS 由子系统④使用。
 */
public final class NoUtil {

    private NoUtil() {
    }

    public static String today() {
        return java.time.LocalDate.now().toString().replace("-", "");
    }

    /** countToday = 今日同前缀单据数（调用方按「前缀+日期」统计） */
    public static String next(String prefix, long countToday) {
        return prefix + today() + String.format("%04d", countToday + 1);
    }
}
