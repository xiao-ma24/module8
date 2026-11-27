package com.school.material.admin.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/** 平台管理端 AI 运维助手：配置 ai.api-key 后走大模型，未配置时用真实数据 + 规则引擎兜底。 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final OverviewService overviewService;
    private final JdbcTemplate jdbc;

    @Value("${ai.base-url:https://api.deepseek.com/v1}")
    private String baseUrl;

    @Value("${ai.api-key:}")
    private String apiKey;

    @Value("${ai.model:deepseek-chat}")
    private String model;

    public boolean llmEnabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public Map<String, Object> status() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("llmEnabled", llmEnabled());
        m.put("desc", llmEnabled() ? "已接入大模型" : "规则引擎（未配置 ai.api-key，按实时数据回答）");
        return m;
    }

    /** 对话入口 */
    public Map<String, Object> chat(String question) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mode", llmEnabled() ? "LLM" : "规则引擎");
        String q = question == null ? "" : question.trim();
        if (q.isEmpty()) {
            r.put("answer", "请描述你想排查的问题，例如「平台现在有多少用户」「最近有哪些登录失败」「角色权限怎么配」。");
            return r;
        }
        if (llmEnabled()) {
            try {
                r.put("answer", callLlm("你是学校物资管理平台的运维助手，服务于系统管理员。"
                        + "请严格依据下面的实时数据回答，数字必须来自数据，不要编造；回答简洁、分点、用中文。\n"
                        + "实时数据：\n" + context() + "\n管理员问题：" + q));
                return r;
            } catch (Exception e) {
                log.warn("AI 调用失败，降级规则引擎: {}", e.getMessage());
                r.put("mode", "规则引擎（大模型调用失败）");
            }
        }
        r.put("answer", rules(q));
        return r;
    }

    /** 供大模型使用的实时数据上下文 */
    private String context() {
        Map<String, Object> s = overviewService.summary();
        StringBuilder sb = new StringBuilder();
        sb.append("平台用户：共").append(s.get("userTotal")).append("人，启用").append(s.get("userActive"))
          .append("人，停用").append(s.get("userDisabled")).append("人，近7天新增").append(s.get("userNew7d")).append("人\n");
        sb.append("组织与权限：部门").append(s.get("deptTotal")).append("个，启用角色").append(s.get("roleTotal"))
          .append("个，字典项类型").append(s.get("dictTotal")).append("类，系统参数").append(s.get("configTotal")).append("项\n");
        sb.append("业务概况：物资").append(s.get("materialTotal")).append("种，供应商").append(s.get("supplierTotal"))
          .append("家，低库存预警").append(s.get("lowStock")).append("条，采购申请待审批").append(s.get("prPending"))
          .append("条，在途订单").append(s.get("poInTransit")).append("单，待验收入库").append(s.get("inboundTodo")).append("单\n");
        sb.append("登录与操作：今日登录").append(s.get("todayLogin")).append("次，近7天登录失败").append(s.get("loginFail7d"))
          .append("次，今日操作").append(s.get("todayOps")).append("条\n");
        try {
            List<Map<String, Object>> fails = jdbc.queryForList("""
                    SELECT user_no, user_name, message, DATE_FORMAT(created_at, '%m-%d %H:%i') AS t
                    FROM sys_login_log WHERE result = 0 ORDER BY created_at DESC LIMIT 5""");
            if (!fails.isEmpty()) {
                sb.append("最近登录失败：");
                fails.forEach(f -> sb.append(f.get("user_no")).append("/").append(f.get("user_name"))
                        .append("/").append(f.get("message")).append("/").append(f.get("t")).append("；"));
                sb.append("\n");
            }
            List<Map<String, Object>> locked = jdbc.queryForList("""
                    SELECT user_no, COUNT(*) AS cnt FROM sys_login_log
                    WHERE result = 0 AND created_at >= NOW() - INTERVAL 7 DAY
                    GROUP BY user_no HAVING COUNT(*) >= 3 ORDER BY cnt DESC LIMIT 5""");
            if (!locked.isEmpty()) {
                sb.append("近7天多次登录失败账号：");
                locked.forEach(l -> sb.append(l.get("user_no")).append("失败").append(l.get("cnt")).append("次；"));
                sb.append("\n");
            }
        } catch (Exception e) {
            log.warn("读取登录日志失败: {}", e.getMessage());
        }
        return sb.toString();
    }

    /** 未配置大模型时的规则引擎：仍使用真实数据回答 */
    private String rules(String q) {
        Map<String, Object> s = overviewService.summary();
        if (q.contains("用户") || q.contains("账号") || q.contains("多少人")) {
            return "平台现有用户 " + s.get("userTotal") + " 人：启用 " + s.get("userActive") + " 人、停用 "
                    + s.get("userDisabled") + " 人，近 7 天新增 " + s.get("userNew7d") + " 人。"
                    + "（今日登录 " + s.get("todayLogin") + " 次，近 7 天登录失败 " + s.get("loginFail7d") + " 次）";
        }
        if (q.contains("登录失败") || q.contains("登录异常") || q.contains("锁定")) {
            return "近 7 天登录失败 " + s.get("loginFail7d") + " 次。排查：①账号不存在→核对工号是否已建档；"
                    + "②密码连续错误→在用户管理中重置密码；③账号停用→在用户管理中启用。明细见「安全审计 → 登录日志」。";
        }
        if (q.contains("部门")) {
            return "当前共有 " + s.get("deptTotal") + " 个部门。新增部门需：编码唯一、指定负责人、填写月度预算额度；"
                    + "部门下仍有成员时无法删除，请先做人员调岗。";
        }
        if (q.contains("角色") || q.contains("权限") || q.contains("开权限")) {
            return "现有启用角色 " + s.get("roleTotal") + " 个。授权流程：①用户管理新建账号并选角色；"
                    + "②在角色权限中给角色勾选菜单；③用户用初始密码 123456 登录后修改。改完重新登录即生效。";
        }
        if (q.contains("字典")) {
            return "当前字典共 " + s.get("dictTotal") + " 类枚举值（如物资分类、单据状态），供各子系统下拉框与标签使用。";
        }
        if (q.contains("参数")) {
            return "当前系统参数 " + s.get("configTotal") + " 项，用于平台级开关与阈值，例如初始密码、自动审批金额、AI 开关。"
                    + "内置参数不可删除，只能改值。";
        }
        if (q.contains("审计") || q.contains("日志")) {
            return "今日操作日志 " + s.get("todayOps") + " 条，今日登录 " + s.get("todayLogin") + " 次。"
                    + "操作日志记录「谁在哪个模块对哪个对象做了什么」，登录日志记录成功/失败、IP 与设备，默认保留 180 天。";
        }
        if (q.contains("物资") || q.contains("库存") || q.contains("供应商")) {
            return "物资 " + s.get("materialTotal") + " 种、供应商 " + s.get("supplierTotal") + " 家，低库存预警 "
                    + s.get("lowStock") + " 条；采购申请待审批 " + s.get("prPending") + " 条，在途订单 "
                    + s.get("poInTransit") + " 单，待验收入库 " + s.get("inboundTodo") + " 单。";
        }
        return "我可以基于实时数据回答：用户与账号、部门、角色权限、字典、系统参数、登录与审计日志、物资与采购概况。"
                + "当前：用户 " + s.get("userTotal") + " 人、部门 " + s.get("deptTotal") + " 个、角色 "
                + s.get("roleTotal") + " 个、今日登录 " + s.get("todayLogin") + " 次。";
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private String callLlm(String prompt) {
        Map<String, Object> body = new HashMap<>();
        body.put("model", model);
        body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
        body.put("temperature", 0.3);
        HttpHeaders h = new HttpHeaders();
        h.setContentType(MediaType.APPLICATION_JSON);
        h.setBearerAuth(apiKey);
        RestTemplate rt = new RestTemplate();
        ResponseEntity<Map> resp = rt.postForEntity(baseUrl + "/chat/completions", new HttpEntity<>(body, h), Map.class);
        Map<String, Object> respBody = resp.getBody();
        if (respBody != null && respBody.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Map<?, ?> c = (Map<?, ?>) choices.get(0);
            if (c.get("message") instanceof Map<?, ?> msg && msg.get("content") != null) {
                return msg.get("content").toString();
            }
        }
        throw new IllegalStateException("大模型响应格式异常");
    }
}
