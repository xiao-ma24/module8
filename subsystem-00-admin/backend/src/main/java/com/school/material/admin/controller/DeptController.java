package com.school.material.admin.controller;

import com.school.material.admin.common.Result;
import com.school.material.admin.entity.Dept;
import com.school.material.admin.service.DeptService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 部门（组织架构） */
@RestController
@RequestMapping("/api/v1/admin/depts")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping
    public Result<List<Dept>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) Integer status) {
        return Result.ok(deptService.list(keyword, status));
    }

    @PostMapping
    public Result<Dept> create(@RequestBody Dept dept) {
        dept.setDeptId(null);
        return Result.ok(deptService.save(dept));
    }

    @PutMapping
    public Result<Dept> update(@RequestBody Dept dept) {
        return Result.ok(deptService.save(dept));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable String id) {
        deptService.remove(id);
        return Result.ok();
    }
}
