package com.school.material.purchase.controller;

import com.school.material.purchase.common.Result;
import com.school.material.purchase.security.CurrentUser;
import com.school.material.purchase.security.CurrentUserService;
import com.school.material.purchase.service.ProfileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "个人中心")
@RestController
@RequestMapping("/api/v1/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final ProfileService profileService;
    private final CurrentUserService currentUserService;
    private final JdbcTemplate jdbcTemplate;

    @Operation(summary = "个人中心聚合数据：账号信息、岗位工作量、待办、操作记录")
    @GetMapping
    public Result<Map<String, Object>> profile(@RequestParam(required = false) Long userId) {
        // 统一身份优先：DBA 登录后以 CurrentUserService 为准，忽略前端传入的 userId
        CurrentUser current = currentUserService.currentOrNull();
        Long effectiveUserId = userId;
        if (current != null) {
            effectiveUserId = resolveUserId(current.userId());
        }
        return Result.ok(profileService.profile(effectiveUserId));
    }

    /** 统一身份下发的可能是工号（T0002），换成本库 mat_sys_user.user_id；本身是数字则直接用。 */
    private Long resolveUserId(String identity) {
        if (identity == null || identity.isBlank()) {
            return null;
        }
        try {
            return Long.valueOf(identity);
        } catch (NumberFormatException ignored) {
            // 继续按工号查询
        }
        try {
            return jdbcTemplate.queryForObject(
                    "SELECT user_id FROM mat_sys_user WHERE person_no = ? AND del_flag = 0",
                    Long.class, identity);
        } catch (Exception e) {
            return null;
        }
    }
}
