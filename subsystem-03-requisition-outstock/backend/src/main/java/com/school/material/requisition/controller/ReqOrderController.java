package com.school.material.requisition.controller;

import com.school.material.requisition.security.RequireRole;
import com.school.material.requisition.auth.RequireRoles;
import com.school.material.requisition.common.Role;
import com.school.material.requisition.common.Result;
import com.school.material.requisition.dto.ReqDTOs;
import com.school.material.requisition.entity.ApproveRecord;
import com.school.material.requisition.entity.ReqOrder;
import com.school.material.requisition.service.RequisitionService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 领用申请：提交 → 部门负责人审批（可部分批准）→ 库管员出库 → 申请人签收。
 * 角色口径见 {@link Role}；数据范围在 Service 层按角色过滤。
 */
@RestController
@RequestMapping("/api/v1/requisition/orders")
@RequiredArgsConstructor
@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN", "STORE_KEEPER"})
public class ReqOrderController {

    private final RequisitionService service;

    /** 领用单列表：申请人=本人、部门负责人=本部门、库管员/管理员=全部 */
    @GetMapping
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<ReqOrder>> list(@RequestParam(required = false) Integer status,
                                       @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(status, keyword));
    }

    /** 待我审批（库管员 / 管理员兜底） */
    @GetMapping("/pending-approve")
    @RequireRoles({Role.STORE_KEEPER})
    public Result<List<ReqOrder>> pendingApprove() {
        return Result.ok(service.pendingApprove());
    }

    @GetMapping("/{id}")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<ReqOrder> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    @GetMapping("/{id}/logs")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<ApproveRecord>> logs(@PathVariable Long id) {
        return Result.ok(service.logs(id));
    }

    /** 提交领用申请（提交时校验可用库存与部门年度额度） */
    @PostMapping
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD})
    public Result<ReqOrder> create(@RequestBody ReqDTOs.Create dto) {
        return Result.ok(service.create(dto));
    }

    @PostMapping("/{id}/submit")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD})
    public Result<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.ok();
    }

    /** 审批：result=1 通过（可部分批准），result=0 驳回（必填意见） */
    @PostMapping("/{id}/approve")
    @RequireRoles({Role.STORE_KEEPER})
    @RequireRole({"DEPT_HEAD", "ADMIN"})
    public Result<Void> approve(@PathVariable Long id, @RequestBody ReqDTOs.Approve dto) {
        service.approve(id, dto);
        return Result.ok();
    }

    @PostMapping("/{id}/withdraw")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD})
    public Result<Void> withdraw(@PathVariable Long id) {
        service.withdraw(id);
        return Result.ok();
    }

    /** 申请人签收：已出库 → 已完成 */
    @PostMapping("/{id}/receive")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD})
    public Result<Void> receive(@PathVariable Long id) {
        service.receive(id);
        return Result.ok();
    }
}
