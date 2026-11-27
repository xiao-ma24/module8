package com.school.material.admin.common;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * 密码摘要工具。
 * 新口令统一存「16 位十六进制盐$SHA-256 加盐摘要(Base64)」，共 61 字符；
 * 历史数据可能是无盐 SHA-256 十六进制，登录校验时兼容并在成功后自动升级。
 */
public final class HashUtil {

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final int SALT_BYTES = 8;
    private static final char SEP = '$';

    private HashUtil() {
    }

    /** 生成加盐摘要：salt$base64(sha256(salt + raw)) */
    public static String hash(String raw) {
        byte[] saltBytes = new byte[SALT_BYTES];
        RANDOM.nextBytes(saltBytes);
        String salt = hex(saltBytes);
        return salt + SEP + Base64.getEncoder().encodeToString(digest(salt + (raw == null ? "" : raw)));
    }

    /** 校验口令：兼容历史无盐摘要 */
    public static boolean matches(String raw, String stored) {
        if (stored == null || stored.isBlank()) return false;
        int i = stored.indexOf(SEP);
        if (i < 0) return stored.equalsIgnoreCase(sha256(raw));
        String salt = stored.substring(0, i);
        String expected = Base64.getEncoder().encodeToString(digest(salt + (raw == null ? "" : raw)));
        return stored.substring(i + 1).equals(expected);
    }

    /** 是否为加盐摘要 */
    public static boolean isSalted(String stored) {
        return stored != null && stored.indexOf(SEP) > 0;
    }

    /** 历史无盐摘要，仅用于兼容旧数据 */
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
