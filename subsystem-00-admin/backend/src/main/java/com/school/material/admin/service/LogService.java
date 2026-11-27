package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.entity.SysAuditLog;
import com.school.material.admin.entity.SysLoginLog;
import com.school.material.admin.mapper.SysAuditLogMapper;
import com.school.material.admin.mapper.SysLoginLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 审计日志查询 */
@Service
@RequiredArgsConstructor
public class LogService {

    private final SysAuditLogMapper auditMapper;
    private final SysLoginLogMapper loginMapper;
    private final JdbcTemplate jdbc;

    public List<SysAuditLog> auditLogs(String module, String keyword, Integer result) {
        return auditMapper.selectList(Wrappers.<SysAuditLog>lambdaQuery()
                .eq(module != null && !module.isBlank(), SysAuditLog::getModule, module)
                .eq(result != null, SysAuditLog::getResult, result)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysAuditLog::getUserName, keyword).or().like(SysAuditLog::getTarget, keyword)
                        .or().like(SysAuditLog::getDetail, keyword))
                .orderByDesc(SysAuditLog::getCreatedAt));
    }

    public List<SysLoginLog> loginLogs(String keyword, Integer result) {
        return loginMapper.selectList(Wrappers.<SysLoginLog>lambdaQuery()
                .eq(result != null, SysLoginLog::getResult, result)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(SysLoginLog::getUserNo, keyword).or().like(SysLoginLog::getUserName, keyword)
                        .or().like(SysLoginLog::getIp, keyword))
                .orderByDesc(SysLoginLog::getCreatedAt));
    }

    /** 模块分布、失败次数等统计，供日志页顶部卡片使用 */
    public Map<String, Object> stats() {
        Map<String, Object> res = new LinkedHashMap<>();
        res.put("auditTotal", safeInt("SELECT COUNT(*) FROM mat_operation_log"));
        res.put("loginTotal", safeInt("SELECT COUNT(*) FROM sys_login_log"));
        res.put("loginFail", safeInt("SELECT COUNT(*) FROM sys_login_log WHERE result = 0"));
        res.put("todayOps", safeInt("SELECT COUNT(*) FROM mat_operation_log WHERE DATE(operate_time) = CURDATE()"));
        res.put("modules", jdbc.queryForList(
                "SELECT module AS name, COUNT(*) AS value FROM mat_operation_log GROUP BY module ORDER BY value DESC"));
        return res;
    }

    public void purge(int days) {
        jdbc.update("DELETE FROM mat_operation_log WHERE operate_time < NOW() - INTERVAL ? DAY", days);
        jdbc.update("DELETE FROM sys_login_log WHERE created_at < NOW() - INTERVAL ? DAY", days);
    }

    private int safeInt(String sql) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }
}
