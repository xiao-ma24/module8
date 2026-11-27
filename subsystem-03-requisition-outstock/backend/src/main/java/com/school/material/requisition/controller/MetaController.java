package com.school.material.requisition.controller;

import com.school.material.requisition.common.Result;
import com.school.material.requisition.entity.Material;
import com.school.material.requisition.entity.Stock;
import com.school.material.requisition.entity.Warehouse;
import com.school.material.requisition.service.MetaService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 基础数据查询（只读）：物资目录、仓库、库存、出库候选批次 */
@RestController
@RequestMapping("/api/v1/requisition/meta")
@RequiredArgsConstructor
public class MetaController {

    private final MetaService metaService;

    /** 可选物资：仅低值易耗品（低值耐用品走借用） */
    @GetMapping("/materials")
    public Result<List<Material>> materials(@RequestParam(required = false) String keyword) {
        return Result.ok(metaService.materials(keyword));
    }

    @GetMapping("/warehouses")
    public Result<List<Warehouse>> warehouses() {
        return Result.ok(metaService.warehouses());
    }

    @GetMapping("/stocks")
    public Result<List<Stock>> stocks(@RequestParam(required = false) String keyword) {
        return Result.ok(metaService.stocks(keyword));
    }

    /** 某物资的公共可领量与可选批次（先到期先出） */
    @GetMapping("/batches")
    public Result<Map<String, Object>> batches(@RequestParam Long materialId,
                                               @RequestParam(required = false) Long warehouseId) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("available", metaService.available(materialId));
        data.put("batches", metaService.batches(materialId, warehouseId));
        return Result.ok(data);
    }
}
