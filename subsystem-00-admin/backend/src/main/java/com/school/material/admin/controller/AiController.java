package com.school.material.admin.controller;

import com.school.material.admin.common.Result;
import com.school.material.admin.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 运维助手：基于平台实时数据回答管理员问题 */
@RestController
@RequestMapping("/api/v1/admin/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.chat(body.getOrDefault("question", "")));
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(aiService.status());
    }
}
