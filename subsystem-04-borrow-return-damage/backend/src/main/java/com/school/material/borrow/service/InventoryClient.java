package com.school.material.borrow.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.material.borrow.common.BizException;
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
 * <p>借用业务的库存口径（报告表1-3、§3.1）：<b>借出只减少可用量、不减少在库总量</b>；
 * 完好归还恢复可用量；损坏/缺失核销才扣减在库总量。</p>
 *
 * <p>本子系统不得直连改表，所有库存变动经此客户端发起；失败统一抛 4011。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${material.purchase-service-url:http://localhost:8082}")
    private String purchaseServiceUrl;

    /** 借出：在库总量不变，可用量 -q（未提前预占），写 3 号借用流水 */
    public void borrowOut(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                          Long bizId, Long bizItemId, Long operatorId) {
        call("/api/v1/purchase/inventory/borrow-out?reserved=false",
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    /** 完好归还：在库不变，可用量 +q，写 4 号归还流水 */
    public void returnGood(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                           Long bizId, Long bizItemId, Long operatorId) {
        call("/api/v1/purchase/inventory/return-good",
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    /** 已借出物资报损核销：在库 -q，写 7 号报废核销流水 */
    public void writeOff(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                         Long bizId, Long bizItemId, Long operatorId) {
        call("/api/v1/purchase/inventory/write-off",
                move(materialId, warehouseId, batchNo, qty, bizId, bizItemId, operatorId));
    }

    /** 库内可用物资报废核销（到期/超限报废）：在库 -q、可用 -q，写 7 号流水 */
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
            Map<String, Object> resp = objectMapper.readValue(raw == null ? "{}" : raw, Map.class);
            Object code = resp.get("code");
            if (code == null || ((Number) code).intValue() != 0) {
                String msg = resp.get("msg") == null ? "库存操作失败" : String.valueOf(resp.get("msg"));
                throw new BizException(4011, "库存服务返回失败：" + msg);
            }
        } catch (BizException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("库存服务 HTTP 异常：{} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(4011, "库存服务调用失败（HTTP " + e.getStatusCode().value() + "）");
        } catch (Exception e) {
            log.error("库存服务不可用：{}", url, e);
            throw new BizException(4011, "库存服务不可用，请确认子系统② 已在 8082 启动");
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
