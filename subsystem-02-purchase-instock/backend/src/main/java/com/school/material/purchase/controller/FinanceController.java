package com.school.material.purchase.controller;

import com.school.material.purchase.common.Result;
import com.school.material.purchase.dto.ReconRow;
import com.school.material.purchase.mapper.ReconMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/** 财务核对：按供应商/物资/日期查询入库成本依据（实际入库单价、金额、数量、经办人） */
@RestController
@RequestMapping("/api/v1/purchase/finance")
@RequiredArgsConstructor
public class FinanceController {

    private final ReconMapper reconMapper;

    @GetMapping("/recon")
    public Result<List<ReconRow>> recon(
            @RequestParam(required = false) Long supplierId,
            @RequestParam(required = false) Long materialId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {
        return Result.ok(reconMapper.selectRecon(supplierId, materialId, startDate, endDate));
    }
}
