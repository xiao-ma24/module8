package com.school.material.purchase.controller;

import com.school.material.purchase.common.Result;
import com.school.material.purchase.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 工作台 / 统计分析 */
@RestController
@RequestMapping("/api/v1/purchase/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService service;

    @GetMapping("/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(service.summary());
    }
}
