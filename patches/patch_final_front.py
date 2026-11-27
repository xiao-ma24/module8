import re
from pathlib import Path

ROOT = Path(r"C:\Users\MECHREVO\Documents\Codex\2026-11-09\w-xia\outputs\integration\m08-final\物资管理-DBA交付-最终")

# 1) 02 个人中心：工号换成本库 user_id
profile = ROOT / "subsystem-02-purchase-instock/backend/src/main/java/com/school/material/purchase/controller/ProfileController.java"
profile.write_text('''package com.school.material.purchase.controller;

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
''', encoding="utf-8")

# 2) 前端路由守卫：统一登录后每次以服务端身份为准
router = ROOT / "subsystem-02-purchase-instock/frontend/src/router/index.js"
text = router.read_text(encoding="utf-8")
old_guard = """router.beforeEach(to => {
  if (to.meta.public) return true
  // 平台端（5180）卡片跳转时携带 uid，等价于统一登录后的身份交接
  if (to.query.uid) localStorage.setItem('mockUserId', String(to.query.uid))
  if (!localStorage.getItem('mockUserId')) return { path: '/login' }
  if (to.path.startsWith('/req/') && !localStorage.getItem('req_token')) return { path: '/login' }
  if (to.path.startsWith('/borrow/') && !localStorage.getItem('borrow_token')) return { path: '/login' }
  return true
})"""
new_guard = """router.beforeEach(async to => {
  if (to.meta.public) return true
  // 平台端（5180）卡片跳转时携带 uid，等价于统一登录后的身份交接
  if (to.query.uid) localStorage.setItem('mockUserId', String(to.query.uid))

  // 统一登录：每次进入都以服务端身份为准，覆盖本地旧缓存，避免切换账号后仍显示上一个人
  try {
    const res = await fetch('/api/v1/profile', { credentials: 'same-origin' })
    if (res.ok) {
      const body = await res.json()
      const info = body?.data?.info
      if (info && info.userId != null) {
        localStorage.setItem('mockUserId', String(info.userId))
        localStorage.setItem('req_token', String(info.userId))
        localStorage.setItem('borrow_token', String(info.userId))
      }
    } else if (res.status === 401 || res.status === 403) {
      localStorage.removeItem('mockUserId')
      return { path: '/login' }
    }
  } catch {
    // 认证服务不可用时保留本地状态，由接口层给出提示
  }

  if (!localStorage.getItem('mockUserId')) return { path: '/login' }
  if (to.path.startsWith('/req/') && !localStorage.getItem('req_token')) localStorage.setItem('req_token', localStorage.getItem('mockUserId'))
  if (to.path.startsWith('/borrow/') && !localStorage.getItem('borrow_token')) localStorage.setItem('borrow_token', localStorage.getItem('mockUserId'))
  return true
})"""
if old_guard in text:
    router.write_text(text.replace(old_guard, new_guard), encoding="utf-8")
else:
    print("router guard pattern MISS")

# 3) 盘点员不再调库管员的近效期接口
dashboard = ROOT / "subsystem-02-purchase-instock/frontend/src/views/Dashboard.vue"
text = dashboard.read_text(encoding="utf-8")
before = "if (isStocktaker && !expiringList.value.length) {"
after = "if (isKeeper && !expiringList.value.length) {"
if before in text:
    dashboard.write_text(text.replace(before, after, 1), encoding="utf-8")
else:
    print("dashboard pattern MISS")

print("done")
