package com.school.material.admin.controller;

import com.school.material.admin.common.Result;
import com.school.material.admin.entity.DeptQuota;
import com.school.material.admin.service.DeptQuotaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 部门月度领用额度（报告表2-4） */
@RestController
@RequestMapping("/api/v1/admin/dept-quotas")
@RequiredArgsConstructor
public class DeptQuotaController {

    private final DeptQuotaService quotaService;

    @GetMapping
    public Result<List<DeptQuota>> list(@RequestParam(required = false) String period,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(quotaService.list(period, keyword));
    }
}
