package com.school.material.admin.controller;

import com.school.material.admin.common.Result;
import com.school.material.admin.service.AuditService;
import com.school.material.admin.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 登录认证 */
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody Map<String, String> body, HttpServletRequest req) {
        String device = req.getHeader("User-Agent");
        String dev = device == null ? "-" : device.substring(0, Math.min(device.length(), 70));
        return Result.ok(authService.login(body.get("userNo"), body.get("password"), AuditService.clientIp(), dev));
    }

    /** 注册页下拉选项（部门、可申请岗位、人员类型、密码最小长度） */
    @GetMapping("/register/options")
    public Result<Map<String, Object>> registerOptions() {
        return Result.ok(authService.registerOptions());
    }

    /** 自助注册：自动建档为「待启用」，管理员启用后方可登录 */
    @PostMapping("/register")
    public Result<Map<String, Object>> register(@RequestBody Map<String, String> body) {
        return Result.ok(authService.register(body));
    }

    @GetMapping("/me")
    public Result<Map<String, Object>> me(@RequestHeader("X-Token") String token) {
        return Result.ok(authService.me(token));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.logout(token);
        return Result.ok();
    }

    /** 修改本人密码（首次登录强制改密也走这里） */
    @PostMapping("/change-pwd")
    public Result<Void> changePwd(@RequestBody Map<String, String> body) {
        var lu = com.school.material.admin.auth.AuthContext.get();
        authService.changePassword(lu == null ? null : lu.getUserId(),
                body.get("oldPwd"), body.get("newPwd"));
        return Result.ok();
    }
}
