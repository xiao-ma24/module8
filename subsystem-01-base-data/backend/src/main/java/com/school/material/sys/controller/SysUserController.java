package com.school.material.sys.controller;

import com.school.material.common.PageResult;
import com.school.material.common.Result;
import com.school.material.sys.dto.AssignRoleReq;
import com.school.material.sys.dto.UserRegisterReq;
import com.school.material.sys.dto.UserReq;
import com.school.material.sys.dto.UserVO;
import com.school.material.sys.service.SysUserService;
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
 * 平台用户管理接口
 */
@RestController
@RequestMapping("/sys/user")
@RequiredArgsConstructor
public class SysUserController {

    private final SysUserService sysUserService;

    /** 分页查询用户，返回部门、角色、状态 */
    @GetMapping("/list")
    public Result<PageResult<UserVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                           @RequestParam(defaultValue = "10") Integer pageSize,
                                           @RequestParam(required = false) String keyword,
                                           @RequestParam(required = false) String deptId,
                                           @RequestParam(required = false) Integer status) {
        return Result.ok(sysUserService.page(pageNum, pageSize, keyword, deptId, status));
    }

    @GetMapping("/{id}")
    public Result<UserVO> get(@PathVariable Long id) {
        return Result.ok(sysUserService.getById(id));
    }

    /** 新教师建档：默认密码，状态待启用 */
    @PostMapping("/register")
    public Result<UserVO> register(@RequestBody @Valid UserRegisterReq req) {
        return Result.ok(sysUserService.register(req));
    }

    /** 分配角色：覆盖原有角色，成功后状态置为启用 */
    @PostMapping("/assign-role")
    public Result<UserVO> assignRole(@RequestBody @Valid AssignRoleReq req) {
        return Result.ok(sysUserService.assignRoles(req));
    }

    @PostMapping
    public Result<Long> create(@RequestBody @Valid UserReq req) {
        return Result.ok(sysUserService.create(req));
    }

    @PutMapping("/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody @Valid UserReq req) {
        sysUserService.update(id, req);
        return Result.ok();
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        sysUserService.delete(id);
        return Result.ok();
    }
}
