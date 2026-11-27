package com.school.material.borrow.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Base64;

/**
 * 密码校验工具，与子系统⓪ 平台管理端保持同一存储口径：
 * 新口令为「16 位十六进制盐$SHA-256(salt+raw) 的 Base64」，历史数据可能为无盐 SHA-256 十六进制。
 * 本子系统只做校验，不改写口令（口令维护属平台管理端）。
 */
public final class HashUtil {

    private static final char SEP = '$';

    private HashUtil() {
    }

    public static boolean matches(String raw, String stored) {
        if (stored == null || stored.isBlank()) return false;
        int i = stored.indexOf(SEP);
        if (i < 0) return stored.equalsIgnoreCase(sha256(raw));
        String salt = stored.substring(0, i);
        String expected = Base64.getEncoder().encodeToString(digest(salt + (raw == null ? "" : raw)));
        return stored.substring(i + 1).equals(expected);
    }

    public static String sha256(String raw) {
        return hex(digest(raw == null ? "" : raw));
    }

    private static byte[] digest(String text) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            return md.digest(text.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new IllegalStateException("SHA-256 不可用", e);
        }
    }

    private static String hex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }
}
