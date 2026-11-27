package com.school.material.purchase.controller;

import com.school.material.purchase.security.RequireRole;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.dto.ReqDTOs;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.service.PurchaseRequestService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 采购申请：四类来源归集 → 预算校验 → 分级审批 → 转订单 */
@RestController
@RequestMapping("/api/v1/purchase/requests")
@RequiredArgsConstructor
@RequireRole({"PURCHASER", "APPLICANT", "ADMIN", "DEPT_HEAD"})
public class PurchaseRequestController {

    private final PurchaseRequestService service;

    @GetMapping
    public Result<List<PurchaseRequest>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/pending")
    public Result<List<PurchaseRequest>> pending() {
        return Result.ok(service.pending());
    }

    @GetMapping("/{id}")
    public Result<PurchaseRequest> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<PurchaseRequestItem>> items(@PathVariable Long id) {
        return Result.ok(service.items(id));
    }

    @GetMapping("/{id}/logs")
    public Result<List<ApproveLog>> logs(@PathVariable Long id) {
        return Result.ok(service.approveLogs(id));
    }

    @PostMapping
    public Result<PurchaseRequest> create(@RequestBody ReqDTOs.PrCreate dto) {
        return Result.ok(service.create(dto));
    }

    @PostMapping("/from-plan/{planId}")
    public Result<PurchaseRequest> fromPlan(@PathVariable Long planId) {
        return Result.ok(service.createFromPlan(planId));
    }

    @PostMapping("/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.ok();
    }

    @PostMapping("/{id}/approve")
    @RequireRole({"DEPT_HEAD", "ADMIN"})
    public Result<Void> approve(@PathVariable Long id,
                                @RequestParam Long approverId,
                                @RequestParam Integer action,
                                @RequestParam(required = false) String opinion,
                                @RequestBody(required = false) List<ReqDTOs.ConfirmItem> confirms) {
        ReqDTOs.Approve a = new ReqDTOs.Approve();
        a.setApproverId(approverId);
        a.setAction(action);
        a.setOpinion(opinion);
        service.approve(id, a, confirms);
        return Result.ok();
    }
}
