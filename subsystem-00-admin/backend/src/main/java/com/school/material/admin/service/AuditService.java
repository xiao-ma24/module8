package com.school.material.admin.service;

import com.school.material.admin.auth.AuthContext;
import com.school.material.admin.auth.LoginUser;
import com.school.material.admin.entity.SysAuditLog;
import com.school.material.admin.mapper.SysAuditLogMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;

/** 操作审计：所有写操作统一调用 write(...) 落库 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private final SysAuditLogMapper auditLogMapper;

    public void write(String module, String action, String target, String detail) {
        write(module, action, target, detail, 1);
    }

    public void write(String module, String action, String target, String detail, int result) {
        write(module, action, target, detail, result, null, null);
    }

    /** 带变更前后快照的审计（对齐文档表2-16 before_value/after_value） */
    public void write(String module, String action, String target, String detail, String before, String after) {
        write(module, action, target, detail, 1, before, after);
    }

    public void write(String module, String action, String target, String detail, int result, String before, String after) {
        SysAuditLog log = new SysAuditLog();
        LoginUser u = AuthContext.get();
        if (u != null) {
            log.setUserId(u.getUserId());
            log.setUserName(u.getUserName());
        } else {
            log.setUserId(0L);   // 未登录场景（自助注册、系统任务）记 0，满足非空约束
        }
        log.setModule(module);
        log.setAction(action);
        log.setTarget(target);
        log.setDetail(detail);
        log.setBeforeValue(before);
        log.setAfterValue(after);
        log.setIp(clientIp());
        log.setResult(result);
        log.setCostMs(0);
        log.setCreatedAt(LocalDateTime.now());
        auditLogMapper.insert(log);
    }

    public static String clientIp() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs == null) return "-";
            HttpServletRequest req = attrs.getRequest();
            String xf = req.getHeader("X-Forwarded-For");
            if (xf != null && !xf.isBlank()) return xf.split(",")[0].trim();
            return req.getRemoteAddr();
        } catch (Exception e) {
            return "-";
        }
    }
}
