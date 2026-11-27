package com.school.material.purchase.service;

import com.school.material.purchase.entity.*;
import com.school.material.purchase.mapper.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.util.*;
import java.util.stream.Collectors;

/** AI 智能辅助：对话查询 / 补货建议 / 验收报告润色。
 *  配置 ai.api-key 后走真实大模型（OpenAI 兼容接口），未配置自动降级为规则引擎（演示模式）。 */
@Slf4j
@Service
public class AiService {

    private final RestTemplate http;
    private final String baseUrl;
    private final String apiKey;
    private final String model;
    private final StockMapper stockMapper;
    private final PurchaseOrderMapper poMapper;
    private final SupplierMapper supplierMapper;
    private final BudgetMapper budgetMapper;
    private final MaterialMapper materialMapper;
    private final PurchaseRequestMapper prMapper;

    public AiService(@Value("${ai.base-url}") String baseUrl,
                     @Value("${ai.api-key:}") String apiKey,
                     @Value("${ai.model}") String model,
                     @Value("${ai.timeout:15000}") int readTimeoutMs,
                     StockMapper stockMapper, PurchaseOrderMapper poMapper,
                     SupplierMapper supplierMapper, BudgetMapper budgetMapper,
                     MaterialMapper materialMapper, PurchaseRequestMapper prMapper) {
        this.baseUrl = baseUrl;
        this.apiKey = apiKey == null ? "" : apiKey.trim();
        this.model = model;
        this.stockMapper = stockMapper;
        this.poMapper = poMapper;
        this.supplierMapper = supplierMapper;
        this.budgetMapper = budgetMapper;
        this.materialMapper = materialMapper;
        this.prMapper = prMapper;
        org.springframework.http.client.SimpleClientHttpRequestFactory f =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        f.setConnectTimeout(5000);
        f.setReadTimeout(readTimeoutMs);
        this.http = new RestTemplate(f);
    }

    public boolean llmEnabled() {
        return !apiKey.isEmpty();
    }

    /** 对话入口 */
    public Map<String, Object> chat(String question) {
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("mode", llmEnabled() ? "LLM" : "规则引擎（未配置 ai.api-key）");
        String q = question == null ? "" : question.trim();
        String demo = route(q);
        if (llmEnabled()) {
            try {
                r.put("answer", callLlm("你是学校物资采购管理系统的智能助手。根据以下实时数据回答用户问题，"
                        + "数字要准确，回答简洁，用中文。\n实时数据：\n" + context() + "\n用户问题：" + q));
            } catch (Exception e) {
                log.warn("LLM 调用失败，降级规则引擎: {}", e.getMessage());
                r.put("answer", demo + "\n\n（大模型调用失败，已降级为规则引擎：" + e.getMessage() + "）");
            }
        } else {
            r.put("answer", demo);
        }
        return r;
    }

    /** 规则引擎：按关键词路由到真实数据库查询 */
    private String route(String q) {
        if (q.isEmpty()) return "您好，我是采购助手。可以问我：\n· 查库存（如\"A4纸还有多少\"）\n· 补货建议\n· 订单情况\n· 供应商信息\n· 预算余额";
        if (q.contains("补货") || q.contains("建议") || q.contains("缺")) return replenishText(computeAdvice());
        if (q.contains("预算")) {
            StringBuilder sb = new StringBuilder("各预算科目余额：\n");
            budgetMapper.selectList(null).forEach(b -> sb.append("· ")
                    .append(b.getSubjectName()).append("（").append(b.getBudgetNo()).append("）")
                    .append(" 余额 ").append(b.getBalance()).append(" 元，冻结 ")
                    .append(b.getFrozen() == null ? 0 : b.getFrozen()).append(" 元\n"));
            return sb.toString();
        }
        if (q.contains("供应商")) {
            StringBuilder sb = new StringBuilder("合作供应商：\n");
            supplierMapper.selectList(null).forEach(s -> sb.append("· ")
                    .append(s.getSupplierName()).append("　评级 ").append(s.getStarLevel()).append("星　状态 ")
                    .append(s.getSupplierStatus() == 1 ? "合作中" : "已停用").append("\n"));
            return sb.toString();
        }
        if (q.contains("订单")) {
            StringBuilder sb = new StringBuilder("最近采购订单：\n");
            poMapper.selectListWithNames().stream().limit(5).forEach(p -> sb.append("· ")
                    .append(p.getPoNo()).append("　").append(p.getSupplierName())
                    .append("　").append(p.getTotalAmount()).append(" 元　状态 ")
                    .append(statusName(p.getPoStatus())).append("\n"));
            return sb.toString();
        }
        if (q.contains("申请")) {
            StringBuilder sb = new StringBuilder("最近的采购申请：\n");
            prMapper.selectListWithNames().stream().limit(5).forEach(p -> sb.append("· ")
                    .append(p.getPrNo()).append("　").append(p.getApplicantName()).append("　")
                    .append(p.getTotalAmount()).append(" 元　")
                    .append(prStatusName(p.getPrStatus())).append("\n"));
            return sb.toString();
        }
        if (q.contains("库存") || q.contains("剩") || q.contains("有多少")) {
            String kw = extractKeyword(q);
            List<Stock> stocks = stockMapper.selectStockList(kw.isEmpty() ? null : kw, null, false);
            if (stocks.isEmpty()) return "没有找到相关库存记录" + (kw.isEmpty() ? "" : "（关键词：" + kw + "）");
            StringBuilder sb = new StringBuilder("库存查询结果：\n");
            for (Stock s : stocks.stream().limit(8).toList()) {
                sb.append("· ").append(s.getMaterialName()).append("（").append(s.getMaterialCode()).append("）　")
                  .append(s.getWarehouseName()).append("：可用 ").append(s.getQtyAvailable().stripTrailingZeros().toPlainString())
                  .append(" ").append(s.getUnit());
                if (s.getQtyAvailable().compareTo(s.getSafeStock()) < 0) sb.append("　⚠已低于安全库存");
                sb.append("\n");
            }
            return sb.toString();
        }
        return "我暂时没听懂。可以试试：\n· \"A4纸还有多少库存\"\n· \"给出补货建议\"\n· \"查一下最近的订单\"";
    }

    /** 从问句中提取物资关键词（去掉常见虚词后按物资名匹配） */
    private String extractKeyword(String q) {
        for (Material m : materialMapper.selectList(null)) {
            if (q.contains(m.getMaterialName())) return m.getMaterialName();
        }
        String cleaned = q.replaceAll("[还有多少库存查一下请问一下还剩吗？?\\s]", "");
        return cleaned.length() >= 2 ? cleaned : "";
    }

    /** 智能补货建议：可用量低于安全库存时，按 max(安全库存×2 - 可用量, 近30天消耗) 推荐采购量 */
    public Map<String, Object> replenishAdvice() {
        List<Map<String, Object>> advice = computeAdvice();
        Map<String, Object> r = new LinkedHashMap<>();
        r.put("advice", advice);
        r.put("text", replenishText(advice));
        return r;
    }

    private List<Map<String, Object>> computeAdvice() {
        List<Stock> low = stockMapper.selectStockList(null, null, true);
        List<Map<String, Object>> advice = new ArrayList<>();
        Map<Long, BigDecimal> avail = new HashMap<>();
        Map<Long, BigDecimal> safe = new HashMap<>();
        Map<Long, BigDecimal> consumed = new HashMap<>();
        Map<Long, Stock> rep = new HashMap<>();
        for (Stock s : low) {
            avail.merge(s.getMaterialId(), nz(s.getQtyAvailable()), BigDecimal::add);
            safe.merge(s.getMaterialId(), nz(s.getSafeStock()), BigDecimal::add);
            consumed.merge(s.getMaterialId(), nz(stockMapper.sumMonthConsumed(s.getMaterialId())), BigDecimal::add);
            rep.putIfAbsent(s.getMaterialId(), s);
        }
        for (Map.Entry<Long, BigDecimal> e : avail.entrySet()) {
            Stock s = rep.get(e.getKey());
            BigDecimal suggest = safe.get(e.getKey()).multiply(BigDecimal.valueOf(2)).subtract(e.getValue())
                    .max(consumed.get(e.getKey())).max(BigDecimal.ONE).setScale(0, RoundingMode.UP);
            Map<String, Object> a = new LinkedHashMap<>();
            a.put("materialId", e.getKey());
            a.put("materialCode", s.getMaterialCode());
            a.put("materialName", s.getMaterialName());
            a.put("unit", s.getUnit());
            a.put("available", e.getValue());
            a.put("safeStock", safe.get(e.getKey()));
            a.put("monthConsumed", consumed.get(e.getKey()));
            a.put("suggestQty", suggest);
            a.put("refPrice", s.getSafeStock() == null ? null : s.getMaterialCode() == null ? null : priceOf(e.getKey(), suggest));
            a.put("reason", "可用量 " + e.getValue().stripTrailingZeros().toPlainString()
                    + " 已低于安全库存 " + safe.get(e.getKey()).stripTrailingZeros().toPlainString()
                    + "；近30天消耗 " + consumed.get(e.getKey()).stripTrailingZeros().toPlainString());
            advice.add(a);
        }
        return advice;
    }

    private BigDecimal priceOf(Long materialId, BigDecimal qty) {
        Material m = materialMapper.selectById(materialId);
        return m == null || m.getRefPrice() == null ? null : m.getRefPrice().multiply(qty);
    }

    private String replenishText(List<Map<String, Object>> advice) {
        if (advice.isEmpty()) return "目前没有低于安全库存的物资，库存状况良好。";
        StringBuilder sb = new StringBuilder("根据安全库存与近30天消耗速率，建议采购：\n");
        for (Map<?, ?> m : advice) {
            sb.append("· ").append(m.get("materialName")).append("：建议采购 ")
              .append(((BigDecimal) m.get("suggestQty")).stripTrailingZeros().toPlainString())
              .append(" ").append(m.get("unit")).append("（").append(m.get("reason")).append("）\n");
        }
        sb.append("\n是否需要我帮你一键生成采购申请？可在【采购申请】页选择来源=安全库存预警创建。");
        return sb.toString();
    }

    /** 供 LLM 使用的实时数据上下文 */
    private String context() {
        StringBuilder sb = new StringBuilder();
        sb.append("低库存物资：");
        List<Stock> low = stockMapper.selectStockList(null, null, true);
        if (low.isEmpty()) sb.append("无\n"); else {
            low.forEach(s -> sb.append(s.getMaterialName()).append("(可用").append(s.getQtyAvailable()).append(")"));
            sb.append("\n");
        }
        sb.append("在途订单：");
        List<PurchaseOrder> active = poMapper.selectListWithNames().stream()
                .filter(p -> p.getPoStatus() == 1 || p.getPoStatus() == 2).toList();
        if (active.isEmpty()) sb.append("无\n"); else {
            active.forEach(p -> sb.append(p.getPoNo()).append("/").append(p.getSupplierName())
                    .append("/").append(p.getTotalAmount()).append("元；"));
            sb.append("\n");
        }
        sb.append("预算余额：");
        budgetMapper.selectList(null).forEach(b -> sb.append(b.getSubjectName())
                .append(" ").append(b.getBalance()).append("元；"));
        sb.append("\n");
        List<PurchaseRequest> prs = prMapper.selectListWithNames();
        sb.append("采购申请：共").append(prs.size()).append("条，");
        prs.stream().collect(Collectors.groupingBy(p -> prStatusName(p.getPrStatus()),
                        LinkedHashMap::new, Collectors.counting()))
                .forEach((k, v) -> sb.append(k).append(v).append("条，"));
        if (!prs.isEmpty()) {
            sb.append("最近5条：");
            prs.stream().limit(5).forEach(p -> sb.append(p.getPrNo()).append("/")
                    .append(p.getApplicantName()).append("/").append(p.getTotalAmount()).append("元/")
                    .append(prStatusName(p.getPrStatus())).append("；"));
        }
        return sb.toString();
    }

    /** 验收报告润色 */
    public String polish(String rawReport) {
        if (llmEnabled()) {
            try {
                return callLlm("你是仓管主管，请把以下验收报告改写得更专业、条理清晰（保留全部数字事实，可分点），直接输出改写结果：\n" + rawReport);
            } catch (Exception e) {
                log.warn("LLM 润色失败: {}", e.getMessage());
            }
        }
        return "【系统生成】" + rawReport + "\n\n（演示模式：配置 ai.api-key 后可由大模型润色为更专业的报告）";
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
        ResponseEntity<Map> resp = http.postForEntity(baseUrl + "/chat/completions",
                new HttpEntity<>(body, h), Map.class);
        Map<String, Object> respBody = resp.getBody();
        if (respBody != null && respBody.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
            Map<?, ?> c = (Map<?, ?>) choices.get(0);
            if (c.get("message") instanceof Map<?, ?> msg) {
                Object content = msg.get("content");
                if (content != null) return content.toString();
            }
        }
        throw new IllegalStateException("大模型响应格式异常");
    }

    private String statusName(Integer s) {
        return switch (s == null ? -1 : s) {
            case 0 -> "待确认"; case 1 -> "已确认"; case 2 -> "部分到货"; case 3 -> "已完成"; case 4 -> "已取消";
            default -> "未知";
        };
    }

    private String prStatusName(Integer s) {
        return switch (s == null ? -1 : s) {
            case 0 -> "草稿"; case 1 -> "待审批"; case 2 -> "已批准"; case 3 -> "已驳回"; case 4 -> "已转订单";
            default -> "未知";
        };
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
