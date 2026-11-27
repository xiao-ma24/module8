package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.entity.SysMenu;
import com.school.material.admin.entity.SysRole;
import com.school.material.admin.service.RoleService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/** 角色与菜单权限 */
@RestController
@RequestMapping("/api/v1/admin/roles")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class RoleController {

    private final RoleService roleService;

    @GetMapping
    public Result<List<SysRole>> list(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) Integer status) {
        return Result.ok(roleService.list(keyword, status));
    }

    @PostMapping
    public Result<SysRole> create(@RequestBody SysRole role) {
        role.setRoleId(null);
        return Result.ok(roleService.save(role));
    }

    @PutMapping
    public Result<SysRole> update(@RequestBody SysRole role) {
        return Result.ok(roleService.save(role));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        roleService.remove(id);
        return Result.ok();
    }

    @PostMapping("/{id}/menus")
    public Result<Void> grant(@PathVariable Long id, @RequestBody List<Long> menuIds) {
        roleService.saveMenus(id, menuIds);
        return Result.ok();
    }

    @GetMapping("/menus")
    public Result<List<SysMenu>> menus() {
        return Result.ok(roleService.menuTree());
    }
}
