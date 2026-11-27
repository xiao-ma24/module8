package com.school.material.purchase.controller;

import com.school.material.purchase.security.RequireRole;
import com.school.material.purchase.common.Result;
import com.school.material.purchase.service.AiService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/** AI 智能助手：对话查询 / 补货建议 */
@RestController
@RequestMapping("/api/v1/purchase/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;

    @RequireRole({"PURCHASER", "ADMIN"})
    @PostMapping("/chat")
    public Result<Map<String, Object>> chat(@RequestBody Map<String, String> body) {
        return Result.ok(aiService.chat(body.getOrDefault("question", "")));
    }

    @RequireRole({"PURCHASER", "ADMIN"})
    @GetMapping("/replenish-advice")
    public Result<Map<String, Object>> replenishAdvice() {
        return Result.ok(aiService.replenishAdvice());
    }

    @GetMapping("/status")
    public Result<Map<String, Object>> status() {
        return Result.ok(Map.of("llmEnabled", aiService.llmEnabled(),
                "desc", aiService.llmEnabled() ? "已接入大模型" : "演示模式（规则引擎），配置 ai.api-key 启用大模型"));
    }
}
