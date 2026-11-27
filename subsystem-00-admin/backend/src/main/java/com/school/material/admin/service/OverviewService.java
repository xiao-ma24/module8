package com.school.material.admin.service;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * 平台数据总览。
 * 说明：物资、采购、库存等指标来自其他子系统表，未部署时自动降级为 0，不影响管理员端启动。
 */
@Service
@RequiredArgsConstructor
public class OverviewService {

    private final JdbcTemplate jdbc;

    public Map<String, Object> summary() {
        Map<String, Object> res = new LinkedHashMap<>();

        res.put("userTotal", safeInt("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0"));
        res.put("userActive", safeInt("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0 AND user_status = 1"));
        res.put("userDisabled", safeInt("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0 AND user_status = 0"));
        res.put("userNew7d", safeInt("SELECT COUNT(*) FROM mat_sys_user WHERE del_flag = 0 AND created_at >= NOW() - INTERVAL 7 DAY"));
        res.put("deptTotal", safeInt("SELECT COUNT(*) FROM mat_dept"));
        res.put("roleTotal", safeInt("SELECT COUNT(*) FROM mat_sys_role WHERE role_status = 1"));
        res.put("dictTotal", safeInt("SELECT COUNT(DISTINCT dict_type) FROM sys_dict_item"));
        res.put("configTotal", safeInt("SELECT COUNT(*) FROM sys_config"));

        res.put("materialTotal", safeInt("SELECT COUNT(*) FROM mat_material WHERE del_flag = 0"));
        res.put("supplierTotal", safeInt("SELECT COUNT(*) FROM mat_supplier WHERE del_flag = 0"));
        res.put("lowStock", safeInt("SELECT COUNT(*) FROM mat_stock WHERE qty_available < 100"));
        res.put("prPending", safeInt("SELECT COUNT(*) FROM purchase_request WHERE pr_status = 1"));
        res.put("poInTransit", safeInt("SELECT COUNT(*) FROM purchase_order WHERE po_status IN (1,2)"));
        res.put("inboundTodo", safeInt("SELECT COUNT(*) FROM inbound_order WHERE in_status = 0"));

        res.put("todayLogin", safeInt("SELECT COUNT(*) FROM sys_login_log WHERE DATE(created_at) = CURDATE()"));
        res.put("loginFail7d", safeInt("SELECT COUNT(*) FROM sys_login_log WHERE result = 0 AND created_at >= NOW() - INTERVAL 7 DAY"));
        res.put("todayOps", safeInt("SELECT COUNT(*) FROM mat_operation_log WHERE DATE(operate_time) = CURDATE()"));

        res.put("loginTrend", safeList("""
                SELECT DATE_FORMAT(d.d, '%m-%d') AS label,
                       (SELECT COUNT(*) FROM sys_login_log l WHERE DATE(l.created_at) = d.d AND l.result = 1) AS value
                FROM (SELECT CURDATE() - INTERVAL seq DAY AS d FROM
                      (SELECT 0 seq UNION SELECT 1 UNION SELECT 2 UNION SELECT 3
                       UNION SELECT 4 UNION SELECT 5 UNION SELECT 6) t) d
                ORDER BY d.d"""));
        res.put("roleDist", safeList("""
                SELECT r.role_name AS name, COUNT(u.user_id) AS value
                FROM mat_sys_role r LEFT JOIN mat_sys_user u ON u.role_code = r.role_code AND u.del_flag = 0
                GROUP BY r.role_id, r.role_name ORDER BY value DESC"""));
        res.put("moduleDist", safeList("""
                SELECT module AS name, COUNT(*) AS value FROM mat_operation_log
                GROUP BY module ORDER BY value DESC LIMIT 6"""));
        res.put("recentOps", safeList("""
                SELECT user_name AS userName, module, action, target, operate_time AS createdAt
                FROM mat_operation_log ORDER BY operate_time DESC LIMIT 6"""));
        return res;
    }

    private int safeInt(String sql) {
        try {
            Integer v = jdbc.queryForObject(sql, Integer.class);
            return v == null ? 0 : v;
        } catch (Exception e) {
            return 0;
        }
    }

    private List<Map<String, Object>> safeList(String sql) {
        try {
            return jdbc.queryForList(sql);
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
