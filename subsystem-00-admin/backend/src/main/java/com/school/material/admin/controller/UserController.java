package com.school.material.admin.controller;

import com.school.material.admin.common.Result;
import com.school.material.admin.entity.User;
import com.school.material.admin.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/** 用户管理 */
@RestController
@RequestMapping("/api/v1/admin/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping
    public Result<List<User>> list(@RequestParam(required = false) String keyword,
                                   @RequestParam(required = false) String roleCode,
                                   @RequestParam(required = false) String deptId,
                                   @RequestParam(required = false) Integer status,
                                   @RequestParam(required = false) Integer userType) {
        return Result.ok(userService.list(keyword, roleCode, deptId, status, userType));
    }

    @PostMapping
    public Result<User> create(@RequestBody User user) {
        user.setUserId(null);
        return Result.ok(userService.save(user));
    }

    @PutMapping
    public Result<User> update(@RequestBody User user) {
        return Result.ok(userService.save(user));
    }

    @DeleteMapping("/{id}")
    public Result<Void> remove(@PathVariable Long id) {
        userService.remove(id);
        return Result.ok();
    }

    @PostMapping("/{id}/status")
    public Result<Void> status(@PathVariable Long id, @RequestParam Integer status,
                               @RequestParam(required = false) String reason) {
        userService.toggleStatus(id, status, reason);
        return Result.ok();
    }

    /** 启用前校验部门/角色/手机号，通过后启用并留痕 */
    @PostMapping("/{id}/activate")
    public Result<Void> activate(@PathVariable Long id, @RequestParam(required = false) String reason) {
        userService.activate(id, reason);
        return Result.ok();
    }

    /** 停用并撤权（不删除账号，保留历史责任链） */
    @PostMapping("/{id}/disable")
    public Result<Void> disable(@PathVariable Long id, @RequestParam(required = false) String reason) {
        userService.disable(id, reason);
        return Result.ok();
    }

    /** 工号/手机号/邮箱查重，重复时返回冲突说明（提示合并或驳回） */
    @GetMapping("/check")
    public Result<Map<String, Object>> check(@RequestParam(required = false) String userNo,
                                             @RequestParam(required = false) String phone,
                                             @RequestParam(required = false) String email,
                                             @RequestParam(required = false) Long excludeId) {
        return Result.ok(userService.checkDuplicate(userNo, phone, email, excludeId));
    }

    /** 模拟从师资/学籍系统同步身份状态 */
    @PostMapping("/sync")
    public Result<Map<String, Object>> sync() {
        return Result.ok(userService.syncSource());
    }

    /** 师资系统工号候选：新增教职工账号时只能从师资系统在册人员中选择 */
    @GetMapping("/teacher-options")
    public Result<List<Map<String, Object>>> teacherOptions(@RequestParam(required = false) String keyword) {
        return Result.ok(userService.teacherOptions(keyword));
    }

    @PostMapping("/{id}/reset-pwd")
    public Result<Void> resetPwd(@PathVariable Long id) {
        userService.resetPassword(id);
        return Result.ok();
    }

    @PostMapping("/import")
    public Result<Map<String, Object>> importUsers(@RequestBody List<Map<String, String>> rows) {
        return Result.ok(userService.batchImport(rows));
    }
}
