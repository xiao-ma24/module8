package com.school.material.stocktake.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.stocktake.common.BizException;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.dto.WarningVO;
import com.school.material.stocktake.entity.WarningRule;
import com.school.material.stocktake.mapper.StocktakeItemMapper;
import com.school.material.stocktake.mapper.WarningRuleMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

/**
 * 库存预警：按 warning_rule 三档规则（安全库存 / 临期 / 到期报废）扫描实时库存。
 *
 * <p>预警只提示、不修改库存。扫描结果缓存在内存中，供前端查询与工作台统计；
 * 定时任务每天按 stocktake.scan-cron 刷新一次，也支持手动触发。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WarningService {

    private final WarningRuleMapper ruleMapper;
    private final StocktakeItemMapper stocktakeItemMapper;
    private final MetricsService metrics;

    private final AtomicReference<List<WarningVO>> cache = new AtomicReference<>(List.of());
    private final AtomicReference<LocalDateTime> scanTime = new AtomicReference<>();

    public LocalDateTime scanTime() {
        return scanTime.get();
    }

    /** 手动触发扫描 */
    public List<WarningVO> scan() {
        List<WarningVO> list = compute();
        cache.set(list);
        scanTime.set(LocalDateTime.now());
        log.info("预警扫描完成：{} 条", list.size());
        return list;
    }

    /** 查询预警；首次访问或缓存为空时自动扫描一次 */
    public List<WarningVO> list(Integer warningType, String keyword) {
        List<WarningVO> list = cache.get();
        if (list.isEmpty()) list = scan();
        List<WarningVO> result = new ArrayList<>(list);
        if (warningType != null) result.removeIf(w -> !warningType.equals(w.getWarningType()));
        if (keyword != null && !keyword.isBlank()) {
            String k = keyword.trim();
            result.removeIf(w -> !(w.getMaterialName().contains(k) || w.getMaterialCode().contains(k)));
        }
        return result;
    }

    /** 预警统计：总数 / 低于补货点 / 临期 / 已过期 / 到期报废 */
    public Map<String, Object> summary() {
        List<WarningVO> list = list(null, null);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("total", list.size());
        m.put("low", count(list, 1));
        m.put("nearExpire", count(list, 2));
        m.put("expired", count(list, 3));
        m.put("lifeDue", count(list, 4));
        m.put("scanTime", scanTime.get());
        return m;
    }

    private long count(List<WarningVO> list, int type) {
        return list.stream().filter(w -> w.getWarningType() == type).count();
    }

    /* ------------------------------------------------------------------ */

    private List<WarningVO> compute() {
        MetricsService.Snapshot snap = metrics.snapshot(null);
        LocalDate today = LocalDate.now();
        int nearDays = metrics.nearExpireDays();
        List<WarningRule> rules = ruleMapper.selectList(Wrappers.<WarningRule>lambdaQuery()
                .eq(WarningRule::getEnable, 1));

        // 按物资归集规则：type -> rule
        Map<Long, Map<Integer, WarningRule>> ruleMap = new LinkedHashMap<>();
        for (WarningRule r : rules) {
            ruleMap.computeIfAbsent(r.getMaterialId(), k -> new LinkedHashMap<>()).put(r.getWarningType(), r);
        }

        // 已完成盘点的批次：同批次不再重复提示“去盘点”
        Map<String, String> handled = new LinkedHashMap<>();
        for (Map<String, Object> h : stocktakeItemMapper.selectCompletedBatchKeys()) {
            handled.putIfAbsent(MetricsService.lng(h.get("materialId")) + "#"
                    + MetricsService.str(h.get("batchNo")), MetricsService.str(h.get("stocktakeNo")));
        }

        List<WarningVO> result = new ArrayList<>();
        for (Map<String, Object> material : snap.materialsSorted()) {
            Long materialId = MetricsService.lng(material.get("material_id"));
            String code = MetricsService.str(material.get("material_code"));
            String name = MetricsService.str(material.get("material_name"));
            String unit = MetricsService.str(material.get("unit"));
            int materialType = MetricsService.intOf(material.get("material_type"));
            BigDecimal safeStock = MetricsService.dec(material.get("safe_stock"));
            Map<Integer, WarningRule> mine = ruleMap.getOrDefault(materialId, Map.of());

            // ---- 1. 低于补货点（安全库存）：有规则用规则下限，否则用 material.safe_stock 兜底 ----
            WarningRule lowRule = mine.get(1);
            BigDecimal threshold = lowRule != null && lowRule.getMinQuantity() != null
                    ? lowRule.getMinQuantity() : safeStock;
            if (threshold.signum() > 0) {
                BigDecimal qa = snap.qa(materialId, today);
                BigDecimal loan = snap.onLoan(materialId);
                // 在借数量视同库存，合并后再与安全库存比较
                BigDecimal stock = qa.add(loan);
                BigDecimal rop = snap.rop(materialId);
                BigDecimal compareTo = rop.max(threshold);
                if (stock.compareTo(compareTo) <= 0) {
                    WarningVO w = base(materialId, code, name, unit, 1, "低");
                    w.setCurrentQty(stock);
                    w.setThreshold(compareTo);
                    w.setMessage("可用量 " + trim(qa) + " + 在借 " + trim(loan)
                            + " 仍低于安全库存 " + trim(compareTo));
                    result.add(w);
                }
            }

            // ---- 2. 批次维度：已过期（永久高优先级）与临期 ----
            WarningRule nearRule = mine.get(2);
            int expireDays = nearRule != null && nearRule.getExpireDays() != null
                    ? nearRule.getExpireDays() : nearDays;

            for (Map<String, Object> b : snap.batchesOf(materialId)) {
                LocalDate expire = MetricsService.date(b.get("expire_date"));
                if (expire == null) continue;
                long left = ChronoUnit.DAYS.between(today, expire);
                if (left <= 0) {
                    WarningVO w = base(materialId, code, name, unit, 3, "高");
                    w.setWarehouseId(MetricsService.lng(b.get("warehouse_id")));
                    w.setBatchNo(MetricsService.str(b.get("batch_no")));
                    markHandled(w, handled);
                    w.setExpireDate(expire);
                    w.setCurrentQty(MetricsService.dec(b.get("qty_available")));
                    w.setThreshold(BigDecimal.ZERO);
                    w.setMessage("批次 " + w.getBatchNo() + " 已过期 " + Math.abs(left)
                            + " 天，可用量已冻结，请由库管员报废核销");
                    result.add(w);
                } else if (left <= expireDays) {
                    WarningVO w = base(materialId, code, name, unit, 2, "中");
                    w.setWarehouseId(MetricsService.lng(b.get("warehouse_id")));
                    w.setBatchNo(MetricsService.str(b.get("batch_no")));
                    markHandled(w, handled);
                    w.setExpireDate(expire);
                    w.setCurrentQty(BigDecimal.valueOf(left));
                    w.setThreshold(BigDecimal.valueOf(expireDays));
                    w.setMessage("批次 " + w.getBatchNo() + " 将在 " + left + " 天后到期，请优先领用");
                    result.add(w);
                }
            }

            // ---- 3. 到期报废（低值耐用品：入账日期 + 限用年限）----
            WarningRule lifeRule = mine.get(3);
            Integer years = lifeRule != null && lifeRule.getServiceLifeYears() != null
                    ? lifeRule.getServiceLifeYears()
                    : (materialType == 2 ? MetricsService.intOf(material.get("service_life_years")) : null);
            if (materialType == 2 && years != null && years > 0) {
                for (Map<String, Object> b : snap.batchesOf(materialId)) {
                    LocalDate inbound = MetricsService.date(b.get("inbound_date"));
                    if (inbound == null) continue;
                    LocalDate due = inbound.plusYears(years);
                    if (!today.isBefore(due)) {
                        WarningVO w = base(materialId, code, name, unit, 4, "中");
                        w.setWarehouseId(MetricsService.lng(b.get("warehouse_id")));
                        w.setBatchNo(MetricsService.str(b.get("batch_no")));
                        markHandled(w, handled);
                        w.setExpireDate(due);
                        w.setCurrentQty(MetricsService.dec(b.get("qty_on_hand")));
                        w.setThreshold(BigDecimal.ZERO);
                        w.setMessage("批次 " + w.getBatchNo() + " 已达限用年限 " + years
                                + " 年（" + due + "），请核验后报废核销");
                        result.add(w);
                    }
                }
            }
        }

        result.sort(Comparator.comparing(WarningVO::getWarningType)
                .thenComparing(WarningVO::getMaterialCode));
        return result;
    }

    private WarningVO base(Long materialId, String code, String name, String unit, int type, String level) {
        WarningVO w = new WarningVO();
        w.setWarningType(type);
        w.setWarningLabel(label(type));
        w.setLevel(level);
        w.setMaterialId(materialId);
        w.setMaterialCode(code);
        w.setMaterialName(name);
        w.setUnit(unit);
        return w;
    }

    /** 标记该批次已完成盘点，前端据此显示“已盘点”而不是“去盘点” */
    private void markHandled(WarningVO w, Map<String, String> handled) {
        String no = handled.get(w.getMaterialId() + "#" + w.getBatchNo());
        if (no != null && !no.isBlank()) {
            w.setStocktakeHandled(true);
            w.setStocktakeNo(no);
        }
    }

    private String label(int type) {
        return switch (type) {
            case 1 -> "低于补货点";
            case 2 -> "临期";
            case 3 -> "已过期";
            default -> "到期报废";
        };
    }

    private String trim(BigDecimal v) {
        return v == null ? "0" : v.stripTrailingZeros().toPlainString();
    }

    /* ------------------------------ 规则维护 ------------------------------ */

    public List<Map<String, Object>> rules() {
        MetricsService.Snapshot snap = metrics.snapshot(null);
        List<Map<String, Object>> list = new ArrayList<>();
        for (WarningRule r : ruleMapper.selectList(Wrappers.<WarningRule>lambdaQuery()
                .orderByAsc(WarningRule::getMaterialId))) {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("rule", r);
            Map<String, Object> material = snap.materials.get(r.getMaterialId());
            if (material != null) {
                m.put("materialName", material.get("material_name"));
                m.put("materialCode", material.get("material_code"));
                m.put("unit", material.get("unit"));
            }
            list.add(m);
        }
        return list;
    }

    @Transactional
    public WarningRule saveRule(ReqDTOs.RuleSave dto) {
        if (dto.getMaterialId() == null) throw new BizException(5041, "请选择物资");
        if (dto.getWarningType() == null || dto.getWarningType() < 1 || dto.getWarningType() > 3) {
            throw new BizException(5042, "预警类型只能是 1 安全库存 / 2 临期 / 3 到期报废");
        }
        long warehouseId = dto.getWarehouseId() == null ? 0L : dto.getWarehouseId();
        WarningRule rule = dto.getRuleId() == null ? null : ruleMapper.selectById(dto.getRuleId());
        if (rule == null) {
            rule = ruleMapper.selectOne(Wrappers.<WarningRule>lambdaQuery()
                    .eq(WarningRule::getMaterialId, dto.getMaterialId())
                    .eq(WarningRule::getWarehouseId, warehouseId)
                    .eq(WarningRule::getWarningType, dto.getWarningType()));
        }
        boolean insert = rule == null;
        if (insert) {
            rule = new WarningRule();
            rule.setMaterialId(dto.getMaterialId());
            rule.setWarehouseId(warehouseId);
            rule.setWarningType(dto.getWarningType());
            rule.setCreatedBy(dto.getOperatorId());
        }
        rule.setMinQuantity(dto.getMinQuantity());
        rule.setMaxQuantity(dto.getMaxQuantity());
        rule.setExpireDays(dto.getExpireDays());
        rule.setServiceLifeYears(dto.getServiceLifeYears());
        rule.setEnable(dto.getEnable() == null ? 1 : dto.getEnable());
        rule.setRemark(dto.getRemark());
        rule.setUpdatedBy(dto.getOperatorId());
        if (insert) ruleMapper.insert(rule);
        else ruleMapper.updateById(rule);
        scan();
        return rule;
    }

    @Transactional
    public void deleteRule(Long ruleId) {
        if (ruleId == null) throw new BizException(5043, "规则主键不能为空");
        ruleMapper.deleteById(ruleId);
        scan();
    }
}
