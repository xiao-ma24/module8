package com.school.material.stocktake.service;

import com.school.material.stocktake.common.BizException;
import com.school.material.stocktake.common.NoUtil;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.dto.SuggestionVO;
import com.school.material.stocktake.mapper.PurchasePlanMapper;
import com.school.material.stocktake.mapper.StockQueryMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * 补货建议与采购计划（报告 2.5 节、子系统⑤ README §2.1）。
 *
 * <p>补货只读库存、不写库存：建议量经包装规格取整后写入共享表 {@code purchase_plan}
 * （{@code plan_type=1} 常规补货），由采购模块继续转采购申请与订单。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ReplenishService {

    private final MetricsService metrics;
    private final PurchasePlanMapper planMapper;
    private final StockQueryMapper queryMapper;
    private final InventoryClient inventoryClient;

    /** 采购计划由采购员审批，管理员可代审 */
    private void requirePurchaser(Long operatorId) {
        String denied = "采购计划须由采购员审批";
        if (operatorId == null) throw new BizException(5065, denied);
        List<String> roles = queryMapper.selectRoleCodes(operatorId);
        boolean ok = roles != null && roles.stream().anyMatch(r -> "PURCHASER".equals(r) || "ADMIN".equals(r));
        if (!ok) throw new BizException(5065, denied);
    }

    /** 补货建议：只对“可用量 ≤ 补货点”且建议量 > 0 的物资给出建议 */
    public List<SuggestionVO> suggestions(String keyword, Integer warningType) {
        MetricsService.Snapshot snap = metrics.snapshot(null);
        List<SuggestionVO> list = new ArrayList<>();
        for (Map<String, Object> m : snap.materialsSorted()) {
            Long materialId = MetricsService.lng(m.get("material_id"));
            String name = MetricsService.str(m.get("material_name"));
            String code = MetricsService.str(m.get("material_code"));
            if (keyword != null && !keyword.isBlank()
                    && !name.contains(keyword.trim()) && !code.contains(keyword.trim())) continue;

            BigDecimal qa = snap.qa(materialId);
            BigDecimal onLoan = snap.onLoan(materialId);
            BigDecimal rop = snap.rop(materialId);
            // 在借数量视同库存，与可用量合并后再与安全库存比较
            if (qa.add(onLoan).compareTo(rop) > 0) continue;

            BigDecimal qs = snap.suggestQty(materialId);
            if (qs.signum() <= 0) continue;

            BigDecimal orderQty = snap.orderQty(materialId, qs);
            BigDecimal refPrice = MetricsService.dec(m.get("ref_price"));

            SuggestionVO vo = new SuggestionVO();
            vo.setMaterialId(materialId);
            vo.setMaterialCode(code);
            vo.setMaterialName(name);
            vo.setUnit(MetricsService.str(m.get("unit")));
            vo.setCategoryCode(MetricsService.str(m.get("category_code")));
            vo.setQa(qa);
            vo.setOnLoan(onLoan);
            vo.setInTransit(snap.inTransit(materialId));
            vo.setSafeStock(snap.safeStock(materialId));
            vo.setDaily(snap.daily(materialId));
            vo.setLeadTimeDays(snap.leadTimeDays(materialId));
            vo.setCoverDays(snap.coverDays(materialId));
            vo.setRop(rop);
            vo.setSuggestQty(qs);
            vo.setPackSize(snap.packSize(materialId));
            vo.setOrderQty(orderQty);
            vo.setRefPrice(refPrice);
            vo.setAmount(orderQty.multiply(refPrice).setScale(2, RoundingMode.HALF_UP));
            list.add(vo);
        }
        list.sort(Comparator.comparing(SuggestionVO::getAmount).reversed());
        return list;
    }

    /** 补货建议汇总：条目数、预计金额、其中紧急条目（可用量为 0） */
    public Map<String, Object> summary() {
        List<SuggestionVO> list = suggestions(null, null);
        BigDecimal amount = list.stream().map(SuggestionVO::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long urgent = list.stream()
                .filter(s -> s.getQa().add(s.getOnLoan() == null ? BigDecimal.ZERO : s.getOnLoan()).signum() <= 0)
                .count();
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("count", list.size());
        m.put("amount", amount.setScale(2, RoundingMode.HALF_UP));
        m.put("urgent", urgent);
        return m;
    }

    /** 由补货建议生成采购计划（复用 purchase_plan，一计划一物资） */
    @Transactional
    public Map<String, Object> createPlans(ReqDTOs.PlanCreate dto) {
        if (dto.getLines() == null || dto.getLines().isEmpty()) {
            throw new BizException(5051, "请至少选择一条补货建议");
        }
        if (dto.getDeptId() == null) throw new BizException(5052, "请选择申请部门");
        List<String> planNos = new ArrayList<>();
        String prefix = "CG" + NoUtil.today();
        // 按当日最大流水号+1 递增，避免因删除/并发导致 count 回退触发 uk_plan_no 重复
        String maxNo = planMapper.selectMaxNoByPrefix(prefix);
        long seq = 1;
        if (maxNo != null && maxNo.length() > prefix.length()) {
            try {
                seq = Long.parseLong(maxNo.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        for (ReqDTOs.PlanLine line : dto.getLines()) {
            if (line.getMaterialId() == null) throw new BizException(5053, "补货建议缺少物资");
            if (line.getPlanQty() == null || line.getPlanQty().signum() <= 0) {
                throw new BizException(5054, "计划数量必须大于 0");
            }
            String planNo = prefix + String.format("%04d", seq++);
            LocalDate expectDate = line.getExpectDate() == null
                    ? LocalDate.now().plusDays(7) : line.getExpectDate();
            planMapper.insertPlan(planNo, dto.getDeptId(), line.getMaterialId(),
                    line.getPlanQty(), line.getStockQty(), line.getSupplierId(), expectDate,
                    0, dto.getOperatorId());
            planNos.add(planNo);
        }
        log.info("生成采购计划 {} 条：{}", planNos.size(), planNos);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("created", planNos.size());
        result.put("planNos", planNos);
        return result;
    }

    public List<Map<String, Object>> plans(Integer status) {
        return planMapper.selectReplenishPlans(status);
    }

    /** 提交计划：0 草稿 → 2 已批准，并直接转② 采购申请，采购员在采购申请里审批下单 */
    @Transactional
    public int submit(List<Long> planIds, Long operatorId) {
        if (planIds == null || planIds.isEmpty()) throw new BizException(5061, "请选择要提交的采购计划");
        int count = 0;
        for (Long id : planIds) {
            count += planMapper.updateStatus(id, 2, 0, null, operatorId);
        }
        if (count == 0) throw new BizException(5062, "选中的计划不是草稿状态，无法提交");
        List<Long> ids = new java.util.ArrayList<>(planIds);
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                ids.forEach(inventoryClient::toPurchaseRequest);
            }
        });
        return count;
    }

    /** 计划审批：1 待审批 → 2 已批准 / 4 已关闭（仅采购员/管理员） */
    @Transactional
    public int approve(ReqDTOs.PlanApprove dto) {
        requirePurchaser(dto.getApproverId());
        if (dto.getPlanIds() == null || dto.getPlanIds().isEmpty()) {
            throw new BizException(5063, "请选择要审批的采购计划");
        }
        boolean pass = !Boolean.FALSE.equals(dto.getPass());
        int target = pass ? 2 : 4;
        int count = 0;
        for (Long id : dto.getPlanIds()) {
            count += planMapper.updateStatus(id, target, 1, dto.getApproverId(), dto.getApproverId());
        }
        if (count == 0) throw new BizException(5064, "选中的计划不是待审批状态");
        if (pass) {
            // 必须等本事务提交后再转，否则② 按 planId 查到的还是“待审批”，会拒绝建申请
            List<Long> ids = new java.util.ArrayList<>(dto.getPlanIds());
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    ids.forEach(inventoryClient::toPurchaseRequest);
                }
            });
        }
        log.info("采购计划审批：{} 张 → 状态 {}", count, target);
        return count;
    }
}
