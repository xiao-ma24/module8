package com.school.material.admin.controller;

import com.school.material.admin.security.RequireRole;
import com.school.material.admin.common.Result;
import com.school.material.admin.service.ProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** 管理员个人中心 */
@RestController
@RequestMapping("/api/v1/admin/profile")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class ProfileController {

    private final ProfileService profileService;

    @GetMapping
    public Result<Map<String, Object>> profile(@RequestParam(required = false) Long userId) {
        return Result.ok(profileService.profile(userId));
    }
}
