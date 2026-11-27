package com.school.material.sys.controller;

import com.school.material.common.Result;
import com.school.material.sys.dto.DeptReq;
import com.school.material.sys.entity.Dept;
import com.school.material.sys.service.DeptService;
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
 * 部门管理接口
 */
@RestController
@RequestMapping("/sys/dept")
@RequiredArgsConstructor
public class DeptController {

    private final DeptService deptService;

    @GetMapping("/list")
    public Result<List<Dept>> list(@RequestParam(required = false) String parentId,
                                   @RequestParam(required = false) Integer status) {
        return Result.ok(deptService.list(parentId, status));
    }

    @GetMapping("/{id}")
    public Result<Dept> get(@PathVariable String id) {
        return Result.ok(deptService.getById(id));
    }

    @PostMapping
    public Result<String> create(@RequestBody @Valid DeptReq req) {
        return Result.ok(deptService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable String id, @RequestBody @Valid DeptReq req) {
        deptService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable String id) {
        deptService.delete(id);
        return Result.ok();
    }
}
