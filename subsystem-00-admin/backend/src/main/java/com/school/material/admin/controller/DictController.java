package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.entity.SysDictItem;
import com.school.material.admin.entity.SysDictType;
import com.school.material.admin.service.DictService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 数据字典 */
@RestController
@RequestMapping("/api/v1/admin/dicts")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class DictController {

    private final DictService dictService;

    @GetMapping("/types")
    public Result<List<SysDictType>> types(@RequestParam(required = false) String keyword) {
        return Result.ok(dictService.types(keyword));
    }

    @PostMapping("/types")
    public Result<SysDictType> saveType(@RequestBody SysDictType type) {
        return Result.ok(dictService.saveType(type));
    }

    @DeleteMapping("/types/{dictType}")
    public Result<Void> removeType(@PathVariable String dictType) {
        dictService.removeType(dictType);
        return Result.ok();
    }

    @GetMapping("/items")
    public Result<List<SysDictItem>> items(@RequestParam(required = false) String dictType,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(dictService.items(dictType, keyword, status));
    }

    @PostMapping("/items")
    public Result<SysDictItem> saveItem(@RequestBody SysDictItem item) {
        if (item.getItemId() == null) return Result.ok(dictService.saveItem(item));
        return Result.ok(dictService.saveItem(item));
    }

    @DeleteMapping("/items/{id}")
    public Result<Void> removeItem(@PathVariable Long id) {
        dictService.removeItem(id);
        return Result.ok();
    }

    /** 全量字典，供各子系统前端缓存 */
    @GetMapping("/all")
    public Result<Map<String, List<Map<String, Object>>>> all() {
        return Result.ok(dictService.all());
    }
}
