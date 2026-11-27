package com.school.material.requisition.controller;

import com.school.material.requisition.security.RequireRole;
import com.school.material.requisition.auth.RequireRoles;
import com.school.material.requisition.common.Role;
import com.school.material.requisition.common.Result;
import com.school.material.requisition.dto.ReqDTOs;
import com.school.material.requisition.entity.OutOrder;
import com.school.material.requisition.entity.ReqOrder;
import com.school.material.requisition.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 出库办理：库管员拣货、选批次、确认出库（调用子系统②扣减库存） */
@RestController
@RequestMapping("/api/v1/requisition/outbounds")
@RequiredArgsConstructor
@RequireRole({"STORE_KEEPER", "ADMIN"})
public class OutboundController {

    private final RequisitionService service;

    @GetMapping
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<OutOrder>> list(@RequestParam(required = false) String keyword) {
        return Result.ok(service.outbounds(keyword));
    }

    /** 待出库的领用单（已批准未出库） */
    @GetMapping("/pending")
    @RequireRoles({Role.STORE_KEEPER})
    public Result<List<ReqOrder>> pending(@RequestParam(required = false) String keyword) {
        return Result.ok(service.pendingOutbound(keyword));
    }

    /** 出库确认：选仓库 + 批次 + 实发数量，库存由子系统②统一扣减 */
    @PostMapping("/confirm")
    @RequireRoles({Role.STORE_KEEPER})
    public Result<OutOrder> confirm(@RequestBody ReqDTOs.Outbound dto) {
        return Result.ok(service.outbound(dto));
    }
}
