package com.school.material.stocktake.controller;

import com.school.material.stocktake.common.Result;
import com.school.material.stocktake.mapper.StockQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * 本模块前端所需的只读基础数据。
 * 正式合并到子系统① 后，前端改为直接调用 /api/v1/basedata/**，本控制器可删除。
 */
@RestController
@RequestMapping("/api/v1/stocktake/meta")
@RequiredArgsConstructor
public class MetaController {

    private final StockQueryMapper query;

    @GetMapping("/materials")
    public Result<List<Map<String, Object>>> materials() {
        return Result.ok(query.selectMaterials());
    }

    @GetMapping("/warehouses")
    public Result<List<Map<String, Object>>> warehouses() {
        return Result.ok(query.selectWarehouses());
    }

    @GetMapping("/suppliers")
    public Result<List<Map<String, Object>>> suppliers() {
        return Result.ok(query.selectSuppliers());
    }

    @GetMapping("/users")
    public Result<List<Map<String, Object>>> users() {
        return Result.ok(query.selectUsers());
    }

    @GetMapping("/depts")
    public Result<List<Map<String, Object>>> depts() {
        return Result.ok(query.selectDepts());
    }
}
