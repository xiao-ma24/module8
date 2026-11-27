package com.school.material.requisition.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.school.material.requisition.common.BizException;
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
 * <p>按规范，`stock` 的唯一写入口是子系统②，本子系统**不得直连改表**，
 * 所有库存变动都经此客户端发起；调用失败统一抛 3011 并提示库存服务不可用。</p>
 *
 * <p>幂等：每次调用都带上本子系统单据主键作为 {@code bizId}，
 * ②侧以 (biz_type, biz_id, biz_item_id) 唯一键拦截重复入账。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class InventoryClient {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${material.purchase-service-url:http://localhost:8082}")
    private String purchaseServiceUrl;

    /** 领用出库（无预占，一次性扣在库与可用），写 2 号领用流水 */
    public void issueDirect(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                            Long bizId, Long operatorId) {
        call("/api/v1/purchase/inventory/issue-direct",
                move(materialId, warehouseId, batchNo, qty, bizId, operatorId));
    }

    /** 通用：POST 到②库存服务并校验返回码 */
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
                throw new BizException(3011, "库存服务返回失败：" + msg);
            }
        } catch (BizException e) {
            throw e;
        } catch (HttpStatusCodeException e) {
            log.error("库存服务 HTTP 异常：{} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new BizException(3011, "库存服务调用失败（HTTP " + e.getStatusCode().value() + "）");
        } catch (Exception e) {
            log.error("库存服务不可用：{}", url, e);
            throw new BizException(3011, "库存服务不可用，请确认子系统② 已在 8082 启动");
        }
    }

    private Map<String, Object> move(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                                     Long bizId, Long operatorId) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("materialId", materialId);
        m.put("warehouseId", warehouseId);
        m.put("batchNo", batchNo);
        m.put("qty", qty);
        m.put("bizId", bizId);
        m.put("bizItemId", 0);
        m.put("operatorId", operatorId);
        return m;
    }
}
