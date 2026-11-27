package com.school.material.stocktake.service;

import com.school.material.stocktake.mapper.StockQueryMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 库存指标快照：一次性读取物资、批次、流水、在途量与系统参数，供预警与补货复用。
 *
 * <p>计算口径（报告 2.5 节）：</p>
 * <pre>
 *   d  = 最近 90 天领用出库量 / 有效天数（新物资按实际天数）
 *   L  = 最近入库间隔均值；不足两次入库时取 stocktake.lead-time-days
 *   ROP= d × L + S
 *   QS = (Dc + L) × d + S − qa − qt
 *   QO = ⌈QS ÷ P⌉ × P
 * </pre>
 */
@Service
@RequiredArgsConstructor
public class MetricsService {

    private final StockQueryMapper query;

    @Value("${stocktake.cover-days:30}")
    private int coverDaysDefault;

    @Value("${stocktake.lead-time-days:7}")
    private int leadTimeDefault;

    @Value("${stocktake.near-expire-days:30}")
    private int nearExpireDefault;

    @Value("${stocktake.pack-size:1}")
    private BigDecimal packSizeDefault;

    public int nearExpireDays() {
        return nearExpireDefault;
    }

    /** 读取一次快照，避免每条规则都全表查询 */
    public Snapshot snapshot(Long warehouseId) {
        Snapshot s = new Snapshot();
        for (Map<String, Object> m : query.selectMaterials()) {
            s.materials.put(lng(m.get("material_id")), m);
        }
        s.batches = query.selectBatches(warehouseId);
        for (Map<String, Object> b : s.batches) {
            s.batchesByMaterial.computeIfAbsent(lng(b.get("material_id")), k -> new ArrayList<>()).add(b);
        }
        LocalDateTime from = LocalDate.now().minusDays(90).atStartOfDay();
        for (Map<String, Object> c : query.selectConsumed(from)) {
            s.consumed.put(lng(c.get("material_id")), dec(c.get("consumed")));
        }
        for (Map<String, Object> i : query.selectInboundStats()) {
            s.inboundStats.put(lng(i.get("material_id")), i);
        }
        for (Map<String, Object> t : query.selectInTransit()) {
            s.inTransit.put(lng(t.get("material_id")), dec(t.get("in_transit")));
        }
        for (Map<String, Object> cfg : query.selectStocktakeConfigs()) {
            s.config.put(String.valueOf(cfg.get("config_key")), String.valueOf(cfg.get("config_value")));
        }
        s.warehouses = query.selectWarehouses();
        return s;
    }

    /** 库存指标快照，纯内存计算，不含任何写操作 */
    public class Snapshot {
        public final Map<Long, Map<String, Object>> materials = new LinkedHashMap<>();
        public final Map<Long, List<Map<String, Object>>> batchesByMaterial = new HashMap<>();
        public final Map<Long, BigDecimal> consumed = new HashMap<>();
        public final Map<Long, Map<String, Object>> inboundStats = new HashMap<>();
        public final Map<Long, BigDecimal> inTransit = new HashMap<>();
        public final Map<String, String> config = new HashMap<>();
        public List<Map<String, Object>> batches = new ArrayList<>();
        public List<Map<String, Object>> warehouses = new ArrayList<>();

        public String warehouseName(Long warehouseId) {
            for (Map<String, Object> w : warehouses) {
                if (lng(w.get("warehouse_id")).equals(warehouseId)) return str(w.get("warehouse_name"));
            }
            return "";
        }

        public BigDecimal qa(Long materialId) {
            return qa(materialId, LocalDate.now());
        }

        /** 可用量：只统计未过期批次的 qty_available（已到期、个人保管仓不计入公共补货） */
        public BigDecimal qa(Long materialId, LocalDate today) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, Object> b : batchesByMaterial.getOrDefault(materialId, List.of())) {
                LocalDate expire = date(b.get("expire_date"));
                if (expire != null && expire.isBefore(today)) continue;
                sum = sum.add(dec(b.get("qty_available")));
            }
            return sum;
        }

        public BigDecimal qaAll(Long materialId) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, Object> b : batchesByMaterial.getOrDefault(materialId, List.of())) {
                sum = sum.add(dec(b.get("qty_available")));
            }
            return sum;
        }

        public List<Map<String, Object>> batchesOf(Long materialId) {
            return batchesByMaterial.getOrDefault(materialId, List.of());
        }

        /** 日均消耗 d（最近 90 天，新物资按实际有效天数） */
        public BigDecimal daily(Long materialId) {
            Map<String, Object> m = materials.get(materialId);
            if (m == null) return BigDecimal.ZERO;
            long days = 90;
            Map<String, Object> stat = inboundStats.get(materialId);
            if (stat != null && stat.get("first_in") != null) {
                LocalDate firstIn = date(stat.get("first_in"));
                long real = ChronoUnit.DAYS.between(firstIn, LocalDate.now());
                days = Math.max(1, Math.min(90, real));
            }
            BigDecimal total = consumed.getOrDefault(materialId, BigDecimal.ZERO);
            if (total.signum() <= 0) return BigDecimal.ZERO;
            return total.divide(BigDecimal.valueOf(days), 3, RoundingMode.HALF_UP);
        }

        /** 采购提前期 L：最近两次入库间隔均值，不足两次取配置默认值 */
        public int leadTimeDays(Long materialId) {
            Map<String, Object> stat = inboundStats.get(materialId);
            if (stat != null) {
                long cnt = lng(stat.get("cnt"));
                if (cnt >= 2 && stat.get("first_in") != null && stat.get("last_in") != null) {
                    long span = ChronoUnit.DAYS.between(date(stat.get("first_in")), date(stat.get("last_in")));
                    long avg = Math.round((double) span / (cnt - 1));
                    if (avg >= 1) return (int) Math.min(60, avg);
                }
            }
            return intConfig("stocktake.lead_time." + materialId, leadTimeDefault);
        }

        public int coverDays(Long materialId) {
            return intConfig("stocktake.cover_days." + materialId,
                    intConfig("stocktake.cover_days", coverDaysDefault));
        }

        public BigDecimal packSize(Long materialId) {
            String v = config.get("stocktake.pack_size." + materialId);
            BigDecimal p = v == null || v.isBlank() ? packSizeDefault : new BigDecimal(v.trim());
            return p.signum() <= 0 ? BigDecimal.ONE : p;
        }

        public BigDecimal inTransit(Long materialId) {
            return inTransit.getOrDefault(materialId, BigDecimal.ZERO);
        }

        public BigDecimal safeStock(Long materialId) {
            Map<String, Object> m = materials.get(materialId);
            return m == null ? BigDecimal.ZERO : dec(m.get("safe_stock"));
        }

        /** 在借量：个人保管仓中尚未归还的物资，补货时视同库存 */
        public BigDecimal onLoan(Long materialId) {
            BigDecimal sum = BigDecimal.ZERO;
            for (Map<String, Object> b : batchesByMaterial.getOrDefault(materialId, List.of())) {
                Long holder = b.get("holder_id") == null ? null : lng(b.get("holder_id"));
                if (holder != null && holder > 0) {
                    sum = sum.add(dec(b.get("qty_on_hand")));
                }
            }
            return sum;
        }

        /** 补货点直接取安全库存，保证补货建议与真实库存口径一致 */
        public BigDecimal rop(Long materialId) {
            return safeStock(materialId).setScale(3, RoundingMode.HALF_UP);
        }

        /** 建议补货量 QS = S − qa − 在借 − qt（安全库存缺口），负数取 0；按最小包装向上取整为整数 */
        public BigDecimal suggestQty(Long materialId) {
            BigDecimal qs = safeStock(materialId)
                    .subtract(qa(materialId))
                    .subtract(onLoan(materialId))
                    .subtract(inTransit(materialId));
            return qs.signum() <= 0 ? BigDecimal.ZERO : qs.setScale(0, RoundingMode.CEILING);
        }

        /** 包装规格取整 QO = ⌈QS ÷ P⌉ × P */
        public BigDecimal orderQty(Long materialId, BigDecimal suggestQty) {
            BigDecimal p = packSize(materialId);
            if (p.compareTo(BigDecimal.ONE) <= 0) return suggestQty.setScale(0, RoundingMode.CEILING);
            return suggestQty.divide(p, 0, RoundingMode.CEILING).multiply(p).setScale(0, RoundingMode.CEILING);
        }

        public List<Map<String, Object>> materialsSorted() {
            List<Map<String, Object>> list = new ArrayList<>(materials.values());
            list.sort(Comparator.comparing(m -> str(m.get("material_code"))));
            return list;
        }

        private int intConfig(String key, int def) {
            String v = config.get(key);
            if (v == null || v.isBlank()) return def;
            try {
                return Integer.parseInt(v.trim());
            } catch (NumberFormatException e) {
                return def;
            }
        }
    }

    public static BigDecimal dec(Object v) {
        if (v == null) return BigDecimal.ZERO;
        if (v instanceof BigDecimal b) return b;
        if (v instanceof Number n) return BigDecimal.valueOf(n.doubleValue());
        return new BigDecimal(v.toString());
    }

    public static Long lng(Object v) {
        return v == null ? Long.valueOf(0L) : Long.valueOf(((Number) v).longValue());
    }

    public static int intOf(Object v) {
        return v == null ? 0 : ((Number) v).intValue();
    }

    public static String str(Object v) {
        return v == null ? "" : v.toString();
    }

    public static LocalDate date(Object v) {
        if (v == null) return null;
        if (v instanceof LocalDate d) return d;
        if (v instanceof java.sql.Date d) return d.toLocalDate();
        if (v instanceof LocalDateTime t) return t.toLocalDate();
        if (v instanceof java.sql.Timestamp t) return t.toLocalDateTime().toLocalDate();
        String s = v.toString();
        return LocalDate.parse(s.length() > 10 ? s.substring(0, 10) : s);
    }
}
