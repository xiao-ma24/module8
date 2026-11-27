package com.school.material.stocktake.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.material.stocktake.common.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 跨子系统库存客户端：调用子系统② 的 {@code /api/v1/purchase/inventory/**}。
 *
 * <p>按《开发约束与规范》§5，`stock` / `stock_flow` 的唯一写入口是② 的 `InventoryService`，
 * 本模块只读库存、不直连改表；盘盈、盘亏、报废核销一律经此客户端发起。</p>
 *
 * <p>幂等：请求带 bizId（盘点单主键）+ bizItemId（盘点明细主键），
 * 重复提交会被② 的 `stock_flow` 唯一键拦下并返回 2339，不会重复记账。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${material.purchase-service-url}")
    private String purchaseServiceUrl;

    /** 补货计划批准后转采购申请（② 按 planId 幂等生成；失败记录日志，不影响已提交的审批） */
    public void toPurchaseRequest(Long planId) {
        String url = purchaseServiceUrl + "/api/v1/purchase/requests/from-plan/" + planId;
        try {
            String raw = restTemplate.postForObject(url, null, String.class);
            Map<?, ?> resp = objectMapper.readValue(raw == null ? "{}" : raw, Map.class);
            Object code = resp.get("code");
            if (code != null && ((Number) code).intValue() == 0) {
                log.info("补货计划 {} 已转采购申请", planId);
            } else {
                log.error("补货计划 {} 转采购申请失败：{}", planId, resp.get("msg"));
            }
        } catch (Exception e) {
            log.error("补货计划 {} 转采购申请失败：{}", planId, e.getMessage());
        }
    }

    /** 盘盈：在库 +q、可用 +q，写 5 号盘盈流水 */
    public void profit(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                       Long bizId, Long bizItemId, Long operatorId) {
        call("/api/v1/purchase/inventory/profit",
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    /** 盘亏：scope 取 AVAILABLE / LOCKED / BORROWED，写 6 号盘亏流水 */
    public void loss(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                     Long bizId, Long bizItemId, Long operatorId, String scope) {
        call("/api/v1/purchase/inventory/loss?scope=" + (scope == null ? "AVAILABLE" : scope),
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    /** 库内可用物资报废核销（到期 / 超限报废）：在库 -q、可用 -q，写 7 号流水 */
    public void scrap(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                      Long bizId, Long bizItemId, Long operatorId) {
        call("/api/v1/purchase/inventory/scrap",
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    private void call(String path, Map<String, Object> body) {
        String url = purchaseServiceUrl + path;
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            String raw = restTemplate.postForObject(url, new HttpEntity<>(body, headers), String.class);
            Map<?, ?> resp = objectMapper.readValue(raw == null ? "{}" : raw, Map.class);
            Object code = resp.get("code");
            if (code == null || ((Number) code).intValue() != 0) {
                String msg = resp.get("msg") == null ? "库存操作失败" : String.valueOf(resp.get("msg"));
                throw new BizException(5011, "库存服务返回失败：" + msg);
            }
        } catch (BizException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("库存服务 HTTP 异常：{} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(5011, "库存服务调用失败（HTTP " + e.getStatusCode().value() + "）");
        } catch (Exception e) {
            log.error("库存服务不可用：{}", url, e);
            throw new BizException(5011, "库存服务不可用，请确认子系统② 已在 8082 启动");
        }
    }

    private Map<String, Object> move(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                                     Long bizId, Long bizItemId, Long operatorId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("warehouseId", warehouseId);
        m.put("batchNo", batchNo);
        m.put("qty", qty);
        m.put("bizId", bizId);
        m.put("bizItemId", bizItemId == null ? 0 : bizItemId);
        m.put("operatorId", operatorId);
        return m;
    }
}
