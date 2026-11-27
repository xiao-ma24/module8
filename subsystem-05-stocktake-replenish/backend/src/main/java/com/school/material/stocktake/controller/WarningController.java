package com.school.material.stocktake.controller;

import com.school.material.stocktake.security.RequireRole;
import com.school.material.stocktake.common.Result;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.dto.WarningVO;
import com.school.material.stocktake.entity.WarningRule;
import com.school.material.stocktake.service.WarningService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 库存预警：三档规则扫描 + 规则维护；预警只提示、不改库存 */
@RestController
@RequestMapping("/api/v1/stocktake")
@RequiredArgsConstructor
@RequireRole({"STOCKTAKER", "ADMIN", "STORE_KEEPER"})
public class WarningController {

    private final WarningService service;

    @GetMapping("/warnings")
    public Result<List<WarningVO>> warnings(@RequestParam(required = false) Integer warningType,
                                            @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(warningType, keyword));
    }

    @GetMapping("/warnings/summary")
    public Result<Map<String, Object>> summary() {
        return Result.ok(service.summary());
    }

    /** 手动触发扫描（定时任务每日 02:00 自动执行） */
    @PostMapping("/warnings/scan")
    public Result<List<WarningVO>> scan() {
        return Result.ok(service.scan());
    }

    @GetMapping("/rules")
    public Result<List<Map<String, Object>>> rules() {
        return Result.ok(service.rules());
    }

    @PostMapping("/rules")
    public Result<WarningRule> saveRule(@RequestBody ReqDTOs.RuleSave dto) {
        return Result.ok(service.saveRule(dto));
    }

    @DeleteMapping("/rules/{ruleId}")
    public Result<Void> deleteRule(@PathVariable Long ruleId) {
        service.deleteRule(ruleId);
        return Result.ok();
    }
}
