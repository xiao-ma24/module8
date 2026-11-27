package com.school.material.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.PageResult;
import com.school.material.common.Result;
import com.school.material.dto.WarehouseReq;
import com.school.material.entity.Warehouse;
import com.school.material.service.WarehouseService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 仓库接口
 */
@RestController
@RequestMapping("/warehouses")
@RequiredArgsConstructor
public class WarehouseController {

    private final WarehouseService warehouseService;

    @GetMapping
    public Result<PageResult<Warehouse>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                              @RequestParam(defaultValue = "10") Integer pageSize,
                                              @RequestParam(required = false) String warehouseName,
                                              @RequestParam(required = false) Integer status) {
        return Result.ok(warehouseService.page(pageNum, pageSize, warehouseName, status));
    }

    @GetMapping("/{id}")
    public Result<Warehouse> get(@PathVariable Long id) {
        return Result.ok(warehouseService.getById(id));
    }

    @RequireRole({"ADMIN"})
    @PostMapping
    public Result<Long> create(@RequestBody @Valid WarehouseReq req) {
        return Result.ok(warehouseService.create(req));
    }

    @RequireRole({"ADMIN"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid WarehouseReq req) {
        warehouseService.update(id, req);
        return Result.ok();
    }

    @RequireRole({"ADMIN"})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        warehouseService.delete(id);
        return Result.ok();
    }
}
