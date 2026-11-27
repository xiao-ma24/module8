package com.school.material.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.Result;
import com.school.material.dto.CategoryTreeResp;
import com.school.material.dto.MaterialCategoryReq;
import com.school.material.service.MaterialCategoryService;
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

import java.util.List;

/**
 * 物资分类接口（全局前缀 /api/v1/basedata 由 context-path 提供）
 */
@RestController
@RequestMapping("/categories")
@RequiredArgsConstructor
public class MaterialCategoryController {

    private final MaterialCategoryService materialCategoryService;

    @GetMapping("/tree")
    public Result<List<CategoryTreeResp>> tree(@RequestParam(required = false) Integer status) {
        return Result.ok(materialCategoryService.tree(status));
    }

    @GetMapping("/{id}")
    public Result<CategoryTreeResp> get(@PathVariable Long id) {
        return Result.ok(materialCategoryService.getById(id));
    }

    @RequireRole({"ADMIN"})
    @PostMapping
    public Result<Long> create(@RequestBody @Valid MaterialCategoryReq req) {
        return Result.ok(materialCategoryService.create(req));
    }

    @RequireRole({"ADMIN"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MaterialCategoryReq req) {
        materialCategoryService.update(id, req);
        return Result.ok();
    }

    @RequireRole({"ADMIN"})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialCategoryService.delete(id);
        return Result.ok();
    }
}
