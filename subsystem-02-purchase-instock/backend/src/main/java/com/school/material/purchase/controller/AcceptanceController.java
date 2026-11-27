package com.school.material.purchase.controller;

import com.school.material.purchase.security.RequireRole;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.dto.ReqDTOs;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.service.AcceptanceService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 到货验收 */
@RestController
@RequestMapping("/api/v1/purchase/acceptances")
@RequiredArgsConstructor
@RequireRole({"CHECKER", "STORE_KEEPER", "ADMIN", "PURCHASER"})
public class AcceptanceController {

    private final AcceptanceService service;

    @GetMapping
    public Result<List<AcceptanceOrder>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/{id}")
    public Result<AcceptanceOrder> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<AcceptanceItem>> items(@PathVariable Long id) {
        return Result.ok(service.items(id));
    }

    @PostMapping
    public Result<AcceptanceOrder> create(@RequestBody ReqDTOs.AcptCreate dto) {
        return Result.ok(service.create(dto));
    }

    /** 完成验收：自动生成待入库单并返回 */
    @PostMapping("/{id}/complete")
    public Result<InboundOrder> complete(@PathVariable Long id, @RequestBody ReqDTOs.AcptComplete dto) {
        return Result.ok(service.complete(id, dto));
    }

    /** AI 润色验收报告：携带页面当前核验明细时按最新数据生成草稿 */
    @PostMapping("/{id}/report")
    public Result<String> report(@PathVariable Long id,
                                 @RequestBody(required = false) ReqDTOs.AcptComplete dto) {
        return Result.ok(service.aiReport(id, dto));
    }

    /** 退换货跟踪列表 */
    @GetMapping("/returns")
    public Result<List<AcceptanceItem>> returns() {
        return Result.ok(service.returns());
    }

    /** 标记退换货处理完成 */
    @PostMapping("/returns/{aiId}/finish")
    public Result<Void> finishReturn(@PathVariable Long aiId,
                                     @RequestParam(required = false) String handler) {
        service.finishReturn(aiId, handler);
        return Result.ok(null);
    }
}
