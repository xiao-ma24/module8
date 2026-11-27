package com.school.material.borrow.controller;

import com.school.material.borrow.security.RequireRole;
import com.school.material.borrow.auth.RequireRoles;
import com.school.material.borrow.common.Role;
import com.school.material.borrow.common.Result;
import com.school.material.borrow.dto.BorrowDTOs;
import com.school.material.borrow.entity.ApproveRecord;
import com.school.material.borrow.entity.BorrowReturn;
import com.school.material.borrow.entity.BorrowReturnItem;
import com.school.material.borrow.entity.OutOrder;
import com.school.material.borrow.service.BorrowService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 借用归还：提交申请 → 部门负责人审批 → 库管员借出登记 → 库管员归还验收（可分次）
 * → 损坏/缺失转报损。角色口径见 {@link Role}。
 */
@RestController
@RequestMapping("/api/v1/borrow")
@RequiredArgsConstructor
@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN", "STORE_KEEPER"})
public class BorrowController {

    private final BorrowService service;

    /** 借用单列表：借用人=本人、部门负责人=本部门、库管员/管理员=全部 */
    @GetMapping("/orders")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<BorrowReturn>> list(@RequestParam(required = false) Integer status,
                                          @RequestParam(required = false) String keyword) {
        return Result.ok(service.list(status, false, keyword));
    }

    @GetMapping("/orders/pending-approve")
    @RequireRoles({Role.STORE_KEEPER})
    public Result<List<BorrowReturn>> pendingApprove() {
        return Result.ok(service.pendingApprove());
    }

    @GetMapping("/orders/{id}")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<BorrowReturn> detail(@PathVariable Long id) {
        return Result.ok(service.detail(id));
    }

    /** 分次归还明细 */
    @GetMapping("/orders/{id}/items")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<BorrowReturnItem>> items(@PathVariable Long id) {
        return Result.ok(service.returnItems(id));
    }

    @GetMapping("/orders/{id}/logs")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<ApproveRecord>> logs(@PathVariable Long id) {
        return Result.ok(service.logs(id));
    }

    @PostMapping("/orders")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD})
    public Result<BorrowReturn> create(@RequestBody BorrowDTOs.Create dto) {
        return Result.ok(service.create(dto));
    }

    @PostMapping("/orders/{id}/approve")
    @RequireRoles({Role.STORE_KEEPER})
    @RequireRole({"DEPT_HEAD", "ADMIN"})
    public Result<Void> approve(@PathVariable Long id, @RequestBody BorrowDTOs.Approve dto) {
        service.approve(id, dto);
        return Result.ok();
    }

    /** 待借出清单（库管员） */
    @GetMapping("/lends/pending")
    @RequireRoles({Role.STORE_KEEPER})
    public Result<List<BorrowReturn>> pendingLend(@RequestParam(required = false) String keyword) {
        return Result.ok(service.pendingLend(keyword));
    }

    /** 借出登记：选仓库与批次，库存只扣可用量 */
    @PostMapping("/lends")
    @RequireRoles({Role.STORE_KEEPER})
    @RequireRole({"STORE_KEEPER", "ADMIN"})
    public Result<OutOrder> lend(@RequestBody BorrowDTOs.Lend dto) {
        return Result.ok(service.lend(dto));
    }

    @GetMapping("/outbounds")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<OutOrder>> outbounds(@RequestParam(required = false) String keyword) {
        return Result.ok(service.outbounds(keyword));
    }

    /** 在借清单 */
    @GetMapping("/returns/borrowed")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<BorrowReturn>> borrowed(@RequestParam(required = false) String keyword) {
        return Result.ok(service.borrowedList(keyword));
    }

    /** 逾期清单（按逾期天数倒序） */
    @GetMapping("/returns/overdue")
    @RequireRoles({Role.APPLICANT, Role.STUDENT, Role.DEPT_HEAD, Role.STORE_KEEPER})
    public Result<List<BorrowReturn>> overdue(@RequestParam(required = false) String keyword) {
        return Result.ok(service.overdue(keyword));
    }

    /** 归还验收：acceptResult 1 完好入库 / 2 损坏核销 / 3 缺失核销，支持分次归还 */
    @PostMapping("/returns")
    @RequireRoles({Role.STORE_KEEPER})
    @RequireRole({"STORE_KEEPER", "ADMIN"})
    public Result<Map<String, Object>> doReturn(@RequestBody BorrowDTOs.ReturnReq dto) {
        return Result.ok(service.doReturn(dto));
    }

    @GetMapping("/dashboard")
    public Result<Map<String, Object>> dashboard() {
        return Result.ok(service.dashboard());
    }
}
