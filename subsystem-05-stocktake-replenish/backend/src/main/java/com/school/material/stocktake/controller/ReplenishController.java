package com.school.material.stocktake.controller;

import com.school.material.stocktake.security.RequireRole;
import com.school.material.stocktake.common.Result;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.dto.SuggestionVO;
import com.school.material.stocktake.service.ReplenishService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 补货建议与采购计划：建议只读库存；确认后写入共享表 purchase_plan（plan_type=1） */
@RestController
@RequestMapping("/api/v1/stocktake")
@RequiredArgsConstructor
@RequireRole({"STOCKTAKER", "ADMIN", "STORE_KEEPER"})
public class ReplenishController {

    private final ReplenishService service;

    @GetMapping("/replenish/suggestions")
    public Result<List<SuggestionVO>> suggestions(@RequestParam(required = false) String keyword) {
        return Result.ok(service.suggestions(keyword, null));
    }

    @GetMapping("/replenish/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(service.summary());
    }

    @PostMapping("/replenish/plans")
    public Result<Map<String, Object>> createPlans(@RequestBody ReqDTOs.PlanCreate dto) {
        return Result.ok(service.createPlans(dto));
    }

    @GetMapping("/plans")
    public Result<List<Map<String, Object>>> plans(@RequestParam(required = false) Integer status) {
        return Result.ok(service.plans(status));
    }

    @PostMapping("/plans/submit")
    public Result<Map<String, Object>> submit(@RequestBody ReqDTOs.PlanApprove dto) {
        int n = service.submit(dto.getPlanIds(), dto.getApproverId());
        return Result.ok(Map.<String, Object>of("updated", n));
    }

    /** 管理者审批：pass=true 已批准，false 已关闭（驳回） */
    @PostMapping("/plans/approve")
    @RequireRole({"PURCHASER", "ADMIN"})
    public Result<Map<String, Object>> approve(@RequestBody ReqDTOs.PlanApprove dto) {
        int n = service.approve(dto);
        return Result.ok(Map.<String, Object>of("updated", n));
    }
}
