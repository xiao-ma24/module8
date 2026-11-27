package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.service.OverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 平台数据总览 */
@RestController
@RequestMapping("/api/v1/admin/overview")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class OverviewController {

    private final OverviewService overviewService;

    @GetMapping
    public Result<Map<String, Object>> summary() {
        return Result.ok(overviewService.summary());
    }
}
