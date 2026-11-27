package com.school.material.stocktake.controller;

import com.school.material.stocktake.security.RequireRole;
import com.school.material.stocktake.common.Result;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.entity.StocktakeOrder;
import com.school.material.stocktake.service.StocktakeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 盘点单：建单 → 开始 → 录入实盘 → 完成并调账（调账经子系统② 库存服务） */
@RestController
@RequestMapping("/api/v1/stocktake/orders")
@RequiredArgsConstructor
@RequireRole({"STOCKTAKER", "ADMIN"})
public class StocktakeController {

    private final StocktakeService service;

    @GetMapping
    public Result<List<Map<String, Object>>> list(@RequestParam(required = false) Integer status,
                                                  @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(status, keyword));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @PostMapping
    public Result<StocktakeOrder> create(@RequestBody ReqDTOs.CreateOrder dto) {
        return Result.ok(service.create(dto));
    }

    @PostMapping("/{id}/start")
    public Result<Void> start(@PathVariable Long id, @RequestParam(required = false) Long operatorId) {
        service.start(id, operatorId);
        return Result.ok();
    }

    @PostMapping("/{id}/items")
    public Result<Map<String, Object>> entry(@PathVariable Long id, @RequestBody ReqDTOs.Entry dto) {
        return Result.ok(service.entry(dto));
    }

    @PostMapping("/{id}/finish")
    public Result<Map<String, Object>> finish(@PathVariable Long id, @RequestBody ReqDTOs.Finish dto) {
        return Result.ok(service.finish(id, dto.getOperatorId(), dto.getOpinion()));
    }
}
