package com.school.material.requisition.controller;

import com.school.material.requisition.auth.RequireRoles;
import com.school.material.requisition.common.Role;
import com.school.material.requisition.common.Result;
import com.school.material.requisition.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 工作台与统计 */
@RestController
@RequestMapping("/api/v1/requisition")
@RequiredArgsConstructor
public class DashboardController {

    private final RequisitionService service;

    /** 工作台：按当前角色返回待办数量、金额与部门额度使用情况 */
    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(service.dashboard());
    }

    /** 领用统计：按物资、按部门汇总（管理员/部门负责人/库管员） */
    @GetMapping("/stats")
    @RequireRoles({Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<Map<String, Object>> stats() {
        return Result.ok(service.stats());
    }
}
