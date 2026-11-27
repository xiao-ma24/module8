package com.school.material.purchase.controller;

import com.school.material.purchase.security.RequireRole;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.dto.ReqDTOs;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.service.InboundService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 入库管理 */
@RestController
@RequestMapping("/api/v1/purchase/inbounds")
@RequiredArgsConstructor
@RequireRole({"STORE_KEEPER", "ADMIN"})
public class InboundController {

    private final InboundService service;

    @GetMapping
    public Result<List<InboundOrder>> list() {
        return Result.ok(service.list());
    }

    @GetMapping("/expiring")
    public Result<List<InboundItem>> expiring() {
        return Result.ok(service.expiring());
    }

    @GetMapping("/{id}")
    public Result<InboundOrder> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @GetMapping("/{id}/items")
    public Result<List<InboundItem>> items(@PathVariable Long id) {
        return Result.ok(service.items(id));
    }

    /** 入库前补充批次号/生产日期/质保期 */
    @PostMapping("/{id}/extras")
    public Result<Void> extras(@PathVariable Long id, @RequestBody List<ReqDTOs.InboundExtra> extras) {
        service.fillExtras(id, extras);
        return Result.ok();
    }

    /** 确认入库：联动库存+流水+订单+预算 */
    @PostMapping("/{id}/confirm")
    public Result<Void> confirm(@PathVariable Long id) {
        service.confirm(id);
        return Result.ok();
    }

    /** 财务回填报销凭证号（模拟第5组回调） */
    @PostMapping("/{id}/voucher")
    public Result<Void> voucher(@PathVariable Long id, @RequestBody ReqDTOs.VoucherFill dto) {
        service.fillVoucher(id, dto);
        return Result.ok();
    }
}
