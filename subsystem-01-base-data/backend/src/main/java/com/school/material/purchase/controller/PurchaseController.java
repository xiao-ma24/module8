package com.school.material.purchase.controller;

import com.school.material.security.CurrentUser;
import com.school.material.security.CurrentUserService;
import com.school.material.security.RequireRole;
import com.school.material.common.Result;
import com.school.material.purchase.dto.PurchaseAuditReq;
import com.school.material.purchase.dto.PurchaseRecordVO;
import com.school.material.purchase.dto.PurchaseRegisterReq;
import com.school.material.purchase.dto.PurchaseRegisterResp;
import com.school.material.purchase.service.PurchaseRequestService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/purchase")
@RequiredArgsConstructor
public class PurchaseController {

    private final PurchaseRequestService purchaseRequestService;
    private final CurrentUserService currentUserService;

    /** 个人发票登记（仅登记，状态为待验收，不变更库存） */
    @RequireRole({"ADMIN", "APPLICANT", "TEACHER", "DEPT_HEAD"})
    @PostMapping("/register")
    public Result<PurchaseRegisterResp> register(@Valid @RequestBody PurchaseRegisterReq req) {
        // 统一登录下登记人以凭证为准，忽略前端传入的 registrarUserId
        CurrentUser current = currentUserService.currentOrNull();
        if (current != null) {
            req.setRegistrarUserId(current.userId());
        }
        return Result.ok(purchaseRequestService.register(req));
    }

    /** 查询个人发票登记记录：不传参查全部，可按登记人、审核状态筛选 */
    @RequireRole({"ADMIN", "APPLICANT", "TEACHER", "DEPT_HEAD", "STORE_KEEPER"})
    @GetMapping("/records")
    public Result<List<PurchaseRecordVO>> records(
            @RequestParam(required = false) String registrarUserId,
            @RequestParam(required = false) Integer auditStatus) {
        return Result.ok(purchaseRequestService.listRecords(registrarUserId, auditStatus));
    }

    /** 查询待验收（0）的发票登记记录，供管理员验收入库 */
    @RequireRole({"ADMIN", "STORE_KEEPER"})
    @GetMapping("/pending")
    public Result<List<PurchaseRecordVO>> pending() {
        return Result.ok(purchaseRequestService.listPending());
    }

    /** 管理员审核：1-通过并入库，2-驳回 */
    @RequireRole({"ADMIN"})
    @PostMapping("/audit")
    public Result<PurchaseRecordVO> audit(@Valid @RequestBody PurchaseAuditReq req) {
        return Result.ok(purchaseRequestService.audit(req));
    }
}
