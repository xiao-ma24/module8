package com.school.material.purchase.service;

import com.school.material.purchase.entity.*;
import com.school.material.purchase.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/** 工作台与统计分析 */
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final PurchaseRequestMapper prMapper;
    private final PurchaseOrderMapper poMapper;
    private final PurchaseOrderItemMapper poiMapper;
    private final InboundOrderMapper inMapper;
    private final AcceptanceOrderMapper acptMapper;
    private final StockMapper stockMapper;
    private final ItemMappers itemMappers;

    public Map<String, Object> summary() {
        Map<String, Object> r = new HashMap<>();
        String month = LocalDate.now().toString().substring(0, 7);

        // 四张统计卡
        BigDecimal monthOrderAmt = poMapper.selectList(null).stream()
                .filter(p -> p.getOrderDate() != null && p.getOrderDate().toString().startsWith(month))
                .filter(p -> p.getPoStatus() != 4)
                .map(p -> p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long pendingApprove = prMapper.selectListWithNames().stream().filter(p -> p.getPrStatus() == 1).count();
        long activeOrders = poMapper.selectListWithNames().stream()
                .filter(p -> p.getPoStatus() == 1 || p.getPoStatus() == 2).count();
        BigDecimal monthInboundAmt = inMapper.selectListWithNames().stream()
                .filter(i -> i.getInStatus() == 1)
                .filter(i -> i.getInDate() != null && i.getInDate().toString().startsWith(month))
                .map(i -> i.getTotalAmount() == null ? BigDecimal.ZERO : i.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> cards = new HashMap<>();
        cards.put("monthOrderAmt", monthOrderAmt);
        cards.put("pendingApprove", pendingApprove);
        cards.put("activeOrders", activeOrders);
        cards.put("monthInboundAmt", monthInboundAmt);
        r.put("cards", cards);

        // 月度趋势（近6个月：订单金额 vs 入库金额）
        List<Map<String, Object>> trend = new ArrayList<>();
        DateTimeFormatter fm = DateTimeFormatter.ofPattern("yyyy-MM");
        for (int i = 5; i >= 0; i--) {
            LocalDate m = LocalDate.now().minusMonths(i);
            String key = m.format(fm);
            BigDecimal order = poMapper.selectList(null).stream()
                    .filter(p -> p.getOrderDate() != null && p.getOrderDate().format(fm).equals(key))
                    .filter(p -> p.getPoStatus() != 4)
                    .map(p -> p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            BigDecimal inbound = inMapper.selectList(null).stream()
                    .filter(x -> x.getInStatus() == 1 && x.getInDate() != null && x.getInDate().format(fm).equals(key))
                    .map(x -> x.getTotalAmount() == null ? BigDecimal.ZERO : x.getTotalAmount())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            Map<String, Object> row = new HashMap<>();
            row.put("month", key);
            row.put("order", order);
            row.put("inbound", inbound);
            trend.add(row);
        }
        r.put("trend", trend);

        // 类别占比（订单明细 join 物资类别）
        Map<String, BigDecimal> catMap = new LinkedHashMap<>();
        for (PurchaseOrder p : poMapper.selectList(null)) {
            if (p.getPoStatus() == 4) continue;
            for (PurchaseOrderItem it : itemMappers.selectPoItems(p.getPoId())) {
                // itemMappers.selectPoItems 已联出物资，但未带 category_code，这里直接查物资表
                catMap.merge(catName(it.getMaterialId()), it.getAmount(), BigDecimal::add);
            }
        }
        r.put("category", catMap.entrySet().stream().map(e -> {
            Map<String, Object> m = new HashMap<>();
            m.put("name", e.getKey());
            m.put("value", e.getValue());
            return m;
        }).collect(Collectors.toList()));

        // 供应商 TOP5
        Map<Long, BigDecimal> supMap = new LinkedHashMap<>();
        Map<Long, String> supName = new HashMap<>();
        for (PurchaseOrder p : poMapper.selectListWithNames()) {
            if (p.getPoStatus() == 4 || p.getSupplierId() == null) continue;
            supMap.merge(p.getSupplierId(), p.getTotalAmount(), BigDecimal::add);
            supName.put(p.getSupplierId(), p.getSupplierName());
        }
        r.put("supplierTop", supMap.entrySet().stream()
                .sorted(Map.Entry.<Long, BigDecimal>comparingByValue().reversed())
                .limit(5).map(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", supName.getOrDefault(e.getKey(), "供应商#" + e.getKey()));
                    m.put("value", e.getValue());
                    return m;
                }).collect(Collectors.toList()));

        // 验收合格率
        List<AcceptanceItem> acptItems = new ArrayList<>();
        for (AcceptanceOrder a : acptMapper.selectList(null)) {
            acptItems.addAll(itemMappers.selectAcptItems(a.getAcptId()));
        }
        BigDecimal check = acptItems.stream().map(i -> nz(i.getCheckQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal pass = acptItems.stream().map(i -> nz(i.getPassQty())).reduce(BigDecimal.ZERO, BigDecimal::add);
        r.put("passRate", check.signum() == 0 ? 100 : pass.multiply(BigDecimal.valueOf(100))
                .divide(check, 1, java.math.RoundingMode.HALF_UP).doubleValue());

        // 低库存清单
        r.put("lowStock", stockMapper.selectStockList(null, null, true));

        // 待办：待审批申请 / 待入库单
        r.put("pendingRequests", prMapper.selectListWithNames().stream()
                .filter(p -> p.getPrStatus() == 1).toList());
        r.put("pendingInbounds", inMapper.selectListWithNames().stream()
                .filter(i -> i.getInStatus() == 0).toList());
        return r;
    }

    private final MaterialMapper materialMapper;
    private final Map<Long, String> catCache = new HashMap<>();

    private String catName(Long materialId) {
        String n = catCache.get(materialId);
        if (n != null) return n;
        Material m = materialMapper.selectById(materialId);
        String name = switch (m == null ? "" : m.getCategoryCode()) {
            case "01" -> "办公耗材";
            case "02" -> "实验器材";
            case "03" -> "后勤物料";
            default -> "通用物资";
        };
        catCache.put(materialId, name);
        return name;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
