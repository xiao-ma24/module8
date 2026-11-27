package com.school.material.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.PageResult;
import com.school.material.common.Result;
import com.school.material.dto.StockChangeReq;
import com.school.material.dto.StockChangeResp;
import com.school.material.dto.StockVO;
import com.school.material.service.StockService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 库存接口（共享核心表，仅本子系统写，②③④⑤ 通过 /stock/change 变更）
 */
@RestController
@RequestMapping({"/stock", "/stocks"})
@RequiredArgsConstructor
public class StockController {

    private final StockService stockService;

    @GetMapping
    public Result<PageResult<StockVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) Long materialId,
                                            @RequestParam(required = false) Long warehouseId) {
        return Result.ok(stockService.page(pageNum, pageSize, materialId, warehouseId));
    }

    @RequireRole({"ADMIN"})
    @PostMapping("/change")
    public Result<StockChangeResp> change(@RequestBody @Valid StockChangeReq req) {
        return Result.ok(stockService.change(req));
    }
}
