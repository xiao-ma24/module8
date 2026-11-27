package com.school.material.admin.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 演示级令牌存储（内存）。
 * 正式合并大系统时应替换为统一 SSO + JWT + Redis，本类仅用于单机演示。
 */
@Component
public class TokenStore {

    @Value("${auth.token-expire-minutes:480}")
    private long expireMinutes;

    private final Map<String, LoginUser> store = new ConcurrentHashMap<>();

    public String issue(LoginUser user) {
        String token = "tk_" + UUID.randomUUID().toString().replace("-", "");
        user.setExpireAt(System.currentTimeMillis() + expireMinutes * 60_000);
        store.put(token, user);
        return token;
    }

    public LoginUser get(String token) {
        if (token == null || token.isBlank()) return null;
        LoginUser u = store.get(token);
        if (u == null) return null;
        if (u.getExpireAt() < System.currentTimeMillis()) {
            store.remove(token);
            return null;
        }
        return u;
    }

    public void revoke(String token) {
        if (token != null) store.remove(token);
    }
}
