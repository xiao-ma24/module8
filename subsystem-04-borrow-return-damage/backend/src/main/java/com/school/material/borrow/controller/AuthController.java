package com.school.material.borrow.controller;

import com.school.material.borrow.auth.AuthContext;
import com.school.material.borrow.auth.LoginUser;
import com.school.material.borrow.common.Result;
import com.school.material.borrow.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** 登录认证：与平台管理端共用 sys_user 账号体系与口令口径 */
@RestController
@RequestMapping("/api/v1/borrow/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    public record LoginReq(String userNo, String password) {
    }

    public record HandoffReq(Long userId) {
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@RequestBody LoginReq req) {
        return Result.ok(authService.login(req.userNo(), req.password()));
    }

    /** 统一登录入口登录成功后，凭用户 ID 换发本子系统令牌 */
    @PostMapping("/handoff")
    public Result<Map<String, Object>> handoff(@RequestBody HandoffReq req) {
        return Result.ok(authService.handoff(req.userId()));
    }

    @PostMapping("/logout")
    public Result<Void> logout(@RequestHeader(value = "X-Token", required = false) String token) {
        authService.logout(token);
        return Result.ok();
    }

    @GetMapping("/me")
    public Result<LoginUser> me() {
        return Result.ok(AuthContext.require());
    }
}
