package com.school.material.sys.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.Result;
import com.school.material.sys.entity.SysRole;
import com.school.material.sys.service.SysRoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 角色查询接口（分配角色时选择用）
 */
@RestController
@RequestMapping("/sys/role")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class SysRoleController {

    private final SysRoleService sysRoleService;

    @GetMapping("/list")
    public Result<List<SysRole>> list(@RequestParam(required = false) Integer status) {
        return Result.ok(sysRoleService.list(status));
    }
}
