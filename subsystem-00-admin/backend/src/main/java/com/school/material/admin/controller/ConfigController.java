package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.entity.SysConfig;
import com.school.material.admin.service.ConfigService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 系统参数 */
@RestController
@RequestMapping("/api/v1/admin/configs")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class ConfigController {

    private final ConfigService configService;

    @GetMapping
    public Result<List<SysConfig>> list(@RequestParam(required = false) String group,
                                        @RequestParam(required = false) String keyword) {
        return Result.ok(configService.list(group, keyword));
    }

    @PostMapping
    public Result<SysConfig> create(@RequestBody SysConfig config) {
        config.setConfigId(null);
        return Result.ok(configService.save(config));
    }

    @PutMapping
    public Result<SysConfig> update(@RequestBody SysConfig config) {
        return Result.ok(configService.save(config));
    }

    @PutMapping("/{id}/value")
    public Result<Void> updateValue(@PathVariable Long id, @RequestBody Map<String, String> body) {
        configService.updateValue(id, body.get("value"));
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        configService.remove(id);
        return Result.ok();
    }
}
