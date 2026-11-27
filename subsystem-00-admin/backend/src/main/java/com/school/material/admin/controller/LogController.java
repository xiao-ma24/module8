package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.entity.SysAuditLog;
import com.school.material.admin.entity.SysLoginLog;
import com.school.material.admin.service.LogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 安全审计：操作日志 / 登录日志 */
@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class LogController {

    private final LogService logService;

    @GetMapping("/logs")
    public Result<List<SysAuditLog>> auditLogs(@RequestParam(required = false) String module,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer result) {
        return Result.ok(logService.auditLogs(module, keyword, result));
    }

    @GetMapping("/login-logs")
    public Result<List<SysLoginLog>> loginLogs(@RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) Integer result) {
        return Result.ok(logService.loginLogs(keyword, result));
    }

    @GetMapping("/log-stats")
    public Result<Map<String, Object>> stats() {
        return Result.ok(logService.stats());
    }
}
