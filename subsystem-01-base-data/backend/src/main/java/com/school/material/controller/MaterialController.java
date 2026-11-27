package com.school.material.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.PageResult;
import com.school.material.common.Result;
import com.school.material.dto.MaterialReq;
import com.school.material.dto.MaterialVO;
import com.school.material.service.MaterialService;
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
 * 物资档案接口（②③④⑤ 跨子系统调用的公共契约）
 */
@RestController
@RequestMapping("/materials")
@RequiredArgsConstructor
public class MaterialController {

    private final MaterialService materialService;

    @GetMapping
    public Result<PageResult<MaterialVO>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                               @RequestParam(defaultValue = "10") Integer pageSize,
                                               @RequestParam(required = false) String materialName,
                                               @RequestParam(required = false) String categoryCode,
                                               @RequestParam(required = false) Integer status) {
        return Result.ok(materialService.page(pageNum, pageSize, materialName, categoryCode, status));
    }

    @GetMapping("/{id}")
    public Result<MaterialVO> get(@PathVariable Long id) {
        return Result.ok(materialService.getById(id));
    }

    @RequireRole({"ADMIN"})
    @PostMapping
    public Result<Long> create(@RequestBody @Valid MaterialReq req) {
        return Result.ok(materialService.create(req));
    }

    @RequireRole({"ADMIN"})
    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid MaterialReq req) {
        materialService.update(id, req);
        return Result.ok();
    }

    @RequireRole({"ADMIN"})
    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        materialService.delete(id);
        return Result.ok();
    }
}
