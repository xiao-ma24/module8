package com.school.material.purchase.controller;

import com.school.material.purchase.security.RequireRole;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.dto.ReqDTOs;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.service.PurchaseOrderService;
import com.school.material.purchase.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 采购订单 */
@RestController
@RequestMapping("/api/v1/purchase/orders")
@RequiredArgsConstructor
@RequireRole({"PURCHASER", "ADMIN"})
public class PurchaseOrderController {

    private final PurchaseOrderService orderService;
    private final PurchaseRequestService requestService;

    @GetMapping
    public Result<List<PurchaseOrder>> list() {
        return Result.ok(orderService.list());
    }

    @RequireRole({"PURCHASER", "ADMIN", "CHECKER"})
    @GetMapping("/receivable")
    public Result<List<PurchaseOrder>> receivable() {
        return Result.ok(orderService.receivable());
    }

    @GetMapping("/{id}")
    public Result<PurchaseOrder> detail(@PathVariable Long id) {
        return Result.ok(orderService.detail(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<PurchaseOrderItem>> items(@PathVariable Long id) {
        return Result.ok(orderService.items(id));
    }

    /** 申请转订单（比价信息在明细 historyAvgPrice 中返回） */
    @PostMapping("/from-request/{prId}")
    public Result<PurchaseOrder> toOrder(@PathVariable Long prId, @RequestBody ReqDTOs.ToOrder dto) {
        return Result.ok(orderService.toOrder(prId, dto));
    }

    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        orderService.confirm(id);
        return Result.ok();
    }

    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id) {
        orderService.cancel(id);
        return Result.ok();
    }

    /** 转订单前给前端确认价格用：某申请的明细+历史均价 */
    @GetMapping("/price-check/{prId}")
    public Result<List<PurchaseRequestItem>> priceCheck(@PathVariable Long prId) {
        return Result.ok(requestService.items(prId));
    }
}
