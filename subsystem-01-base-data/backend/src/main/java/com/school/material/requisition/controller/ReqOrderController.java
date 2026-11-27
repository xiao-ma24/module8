package com.school.material.requisition.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.Result;
import com.school.material.requisition.dto.ReqOrderAuditReq;
import com.school.material.requisition.dto.ReqOrderSubmitReq;
import com.school.material.requisition.dto.ReqOrderVO;
import com.school.material.requisition.service.ReqOrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 库存变动申请审批接口：入库/出库先申请，审核通过后才变更库存
 */
@RestController
@RequestMapping("/stock-apply")
@RequiredArgsConstructor
@RequireRole({"APPLICANT", "STUDENT", "DEPT_HEAD", "ADMIN"})
public class ReqOrderController {

    private final ReqOrderService reqOrderService;

    /** 提交库存变动申请（待审核，不变更库存） */
    @PostMapping("/submit")
    public Result<ReqOrderVO> submit(@Valid @RequestBody ReqOrderSubmitReq req) {
        return Result.ok(reqOrderService.submit(req));
    }

    /** 审核申请：1-通过并变更库存，2-驳回 */
    @PostMapping("/audit")
    @RequireRole({"DEPT_HEAD", "ADMIN"})
    public Result<ReqOrderVO> audit(@Valid @RequestBody ReqOrderAuditReq req) {
        return Result.ok(reqOrderService.audit(req));
    }

    /** 查询申请记录，可按审核状态筛选 */
    @GetMapping("/list")
    public Result<List<ReqOrderVO>> list(@RequestParam(required = false) Integer auditStatus) {
        return Result.ok(reqOrderService.list(auditStatus));
    }
}
