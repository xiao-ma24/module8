package com.school.material.borrow.controller;

import com.school.material.borrow.security.RequireRole;
import com.school.material.borrow.auth.RequireRoles;
import com.school.material.borrow.common.Role;
import com.school.material.borrow.common.Result;
import com.school.material.borrow.dto.BorrowDTOs;
import com.school.material.borrow.entity.ApproveRecord;
import com.school.material.borrow.entity.LossOrder;
import com.school.material.borrow.service.LossService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 报损：借用损坏/缺失转报损 → 审批 → 库管员核销 */
@RestController
@RequestMapping("/api/v1/borrow/losses")
@RequiredArgsConstructor
public class LossController {

    private final LossService service;

    @GetMapping
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<LossOrder>> list(@RequestParam(required = false) Integer status,
                                        @RequestParam(required = false) Integer sourceType,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(status, sourceType, keyword));
    }

    @GetMapping("/{id}/logs")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<ApproveRecord>> logs(@PathVariable Long id) {
        return Result.ok(service.logs(id));
    }

    /** 审批：result=1 通过（须给 disposeResult）、2 驳回（须填意见） */
    @PostMapping("/{id}/approve")
    @RequireRoles({Role.DEPT_HEAD})
    @RequireRole({"DEPT_HEAD", "ADMIN"})
    public Result<Void> approve(@PathVariable Long id, @RequestBody BorrowDTOs.LossApprove dto) {
        service.approve(id, dto);
        return Result.ok();
    }

    /** 核销执行：扣减库存并置 executed=1，返回赔偿金额（disposeResult=1 时） */
    @PostMapping("/{id}/execute")
    @RequireRoles({Role.STORE_KEEPER})
    @RequireRole({"STORE_KEEPER", "ADMIN"})
    public Result<Map<String, Object>> execute(@PathVariable Long id,
                                               @RequestBody(required = false) BorrowDTOs.LossExecute dto) {
        return Result.ok(service.execute(id, dto));
    }
}
