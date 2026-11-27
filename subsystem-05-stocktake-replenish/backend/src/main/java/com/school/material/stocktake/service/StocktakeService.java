package com.school.material.stocktake.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.stocktake.common.BizException;
import com.school.material.stocktake.common.NoUtil;
import com.school.material.stocktake.dto.ReqDTOs;
import com.school.material.stocktake.entity.StocktakeItem;
import com.school.material.stocktake.entity.StocktakeOrder;
import com.school.material.stocktake.mapper.StockQueryMapper;
import com.school.material.stocktake.mapper.StocktakeItemMapper;
import com.school.material.stocktake.mapper.StocktakeOrderMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * 盘点业务：建单（账面快照）→ 录入实盘（差异计算与二次确认）→ 完成并调账。
 *
 * <p>关键控制（报告表3-44）：</p>
 * <ol>
 *   <li>账面数量建单时快照，之后出库不影响本次差异；</li>
 *   <li>录入只产生差异，不写流水；</li>
 *   <li>完成盘点时才调用② 库存服务写盘盈 / 盘亏流水，幂等键为 (bizType=5/6, checkId, itemId)。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class StocktakeService {

    private static final BigDecimal TEN_PERCENT = new BigDecimal("0.1");

    private final StocktakeOrderMapper orderMapper;
    private final StocktakeItemMapper itemMapper;
    private final MetricsService metrics;
    private final InventoryClient inventoryClient;
    private final StockQueryMapper queryMapper;

    /** 岗位校验：盘点单由库管员生成，盘点操作由盘点员执行（管理员两者皆可） */
    private void requireRole(Long operatorId, String deniedMsg, String... allowedRoles) {
        if (operatorId == null) throw new BizException(5040, deniedMsg);
        List<String> roles = queryMapper.selectRoleCodes(operatorId);
        boolean ok = roles != null && roles.stream().anyMatch(r -> {
            for (String allowed : allowedRoles) if (allowed.equals(r)) return true;
            return "ADMIN".equals(r);
        });
        if (!ok) throw new BizException(5041, deniedMsg);
    }

    public List<Map<String, Object>> list(Integer status, String keyword) {
        return orderMapper.selectOrderList(status, keyword);
    }

    public Map<String, Object> detail(Long id) {
        StocktakeOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(5004, "盘点单不存在：" + id);
        List<Map<String, Object>> items = itemMapper.selectByOrder(id);
        long diffLines = items.stream()
                .filter(i -> MetricsService.dec(i.get("diffQuantity")).signum() != 0).count();
        BigDecimal diffTotal = items.stream()
                .map(i -> MetricsService.dec(i.get("diffQuantity")))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("order", order);
        result.put("items", items);
        result.put("diffLines", diffLines);
        result.put("diffTotal", diffTotal);
        return result;
    }

    /**
     * 创建盘点单：按仓库 + 物资集合生成明细，账面数量取当前 stock.qty_on_hand 快照。
     */
    @Transactional
    public StocktakeOrder create(ReqDTOs.CreateOrder dto) {
        requireRole(dto.getOperatorId(), "盘点单须由库管员生成", "STORE_KEEPER");
        if (dto.getWarehouseId() == null) throw new BizException(5001, "请选择盘点仓库");
        MetricsService.Snapshot snap = metrics.snapshot(dto.getWarehouseId());
        boolean warehouseExists = snap.warehouses.stream()
                .anyMatch(w -> MetricsService.lng(w.get("warehouse_id")).equals(dto.getWarehouseId()));
        if (!warehouseExists) throw new BizException(5002, "仓库不存在：" + dto.getWarehouseId());

        Set<Long> wanted = dto.getMaterialIds() == null ? Set.of() : new HashSet<>(dto.getMaterialIds());
        List<Map<String, Object>> lines = new ArrayList<>();
        for (Map<String, Object> b : snap.batches) {
            if (!MetricsService.lng(b.get("warehouse_id")).equals(dto.getWarehouseId())) continue;
            Long materialId = MetricsService.lng(b.get("material_id"));
            if (!wanted.isEmpty() && !wanted.contains(materialId)) continue;
            lines.add(b);
        }
        if (lines.isEmpty()) {
            // 允许无在库批次时盘点：为所选物资生成账面为 0 的明细，实盘大于 0 即按盘盈入账
            if (wanted.isEmpty()) throw new BizException(5003, "该仓库暂无在库批次，请选择具体物资后盘点");
            for (Long mid : wanted) {
                Map<String, Object> virtual = new HashMap<>();
                virtual.put("material_id", mid);
                virtual.put("batch_no", "PD" + NoUtil.today() + "-" + mid);
                virtual.put("qty_on_hand", BigDecimal.ZERO);
                lines.add(virtual);
            }
        }

        String prefix = "PD" + NoUtil.today();
        long todayCount = orderMapper.selectCount(Wrappers.<StocktakeOrder>lambdaQuery()
                .likeRight(StocktakeOrder::getStocktakeNo, prefix));

        StocktakeOrder order = new StocktakeOrder();
        order.setStocktakeNo(NoUtil.next("PD", todayCount, 4));
        order.setWarehouseId(dto.getWarehouseId());
        order.setStocktakeDate(dto.getStocktakeDate() == null ? LocalDate.now() : dto.getStocktakeDate());
        order.setAuditorId(dto.getAuditorId());
        order.setStatus(0);
        order.setRemark(dto.getRemark());
        order.setCreatedBy(dto.getOperatorId());
        orderMapper.insert(order);

        for (Map<String, Object> b : lines) {
            StocktakeItem item = new StocktakeItem();
            item.setStocktakeId(order.getStocktakeId());
            item.setMaterialId(MetricsService.lng(b.get("material_id")));
            item.setBatchNo(MetricsService.str(b.get("batch_no")));
            item.setBookQuantity(MetricsService.dec(b.get("qty_on_hand")));
            item.setCreatedBy(dto.getOperatorId());
            itemMapper.insert(item);
        }
        log.info("创建盘点单 {}，仓库 {}，明细 {} 条", order.getStocktakeNo(), dto.getWarehouseId(), lines.size());
        return order;
    }

    /** 开始盘点：0 待盘点 → 1 进行中 */
    @Transactional
    public void start(Long id, Long operatorId) {
        requireRole(operatorId, "盘点操作须由盘点员执行", "STOCKTAKER");
        StocktakeOrder order = requireOrder(id);
        if (order.getStatus() != 0) throw new BizException(5012, "只有待盘点的单据可以开始盘点");
        order.setStatus(1);
        order.setUpdatedBy(operatorId);
        orderMapper.updateById(order);
    }

    /**
     * 录入实盘数量：差异 = 实盘 − 账面；|差异| 超过账面 10% 时要求二次录入确认。
     */
    @Transactional
    public Map<String, Object> entry(ReqDTOs.Entry dto) {
        requireRole(dto.getOperatorId(), "盘点操作须由盘点员执行", "STOCKTAKER");
        if (dto.getItemId() == null) throw new BizException(5020, "请选择盘点明细");
        StocktakeItem item = itemMapper.selectById(dto.getItemId());
        if (item == null) throw new BizException(5021, "盘点明细不存在：" + dto.getItemId());
        StocktakeOrder order = requireOrder(item.getStocktakeId());
        if (order.getStatus() != 1) throw new BizException(5022, "请先开始盘点再录入实盘数量");

        BigDecimal actual = dto.getActualQuantity();
        if (actual == null || actual.signum() < 0) throw new BizException(5023, "实盘数量必须为非负数字");
        BigDecimal book = item.getBookQuantity() == null ? BigDecimal.ZERO : item.getBookQuantity();
        BigDecimal diff = actual.subtract(book).setScale(3, RoundingMode.HALF_UP);

        boolean over = diff.signum() != 0
                && diff.abs().compareTo(book.multiply(TEN_PERCENT)) > 0;
        if (over && !Boolean.TRUE.equals(dto.getConfirmOver10())) {
            throw new BizException(5024, "差异 " + diff.stripTrailingZeros().toPlainString()
                    + " 超过账面数量 10%，请二次录入确认后再提交");
        }

        item.setActualQuantity(actual);
        item.setDiffQuantity(diff);
        item.setDiffReason(dto.getDiffReason());
        item.setCounted(1);
        item.setUpdatedBy(dto.getOperatorId());
        itemMapper.updateById(item);

        Map<String, Object> result = new HashMap<>();
        result.put("itemId", item.getItemId());
        result.put("bookQuantity", book);
        result.put("actualQuantity", actual);
        result.put("diffQuantity", diff);
        result.put("diffType", diff.signum() > 0 ? "盘盈" : diff.signum() < 0 ? "盘亏" : "相符");
        return result;
    }

    /**
     * 完成盘点并调账：逐条调用子系统② 库存服务写盘盈 / 盘亏流水。
     *
     * <p>跨服务调用无法与本地事务原子提交，因此按幂等键 (bizType, stocktakeId, itemId) 设计：
     * 中途失败可重复提交，已入账条目由② 的 stock_flow 唯一键拦下并返回“已入账”，不会重复扣加。</p>
     */
    public Map<String, Object> finish(Long id, Long operatorId, String opinion) {
        requireRole(operatorId, "盘点操作须由盘点员执行", "STOCKTAKER");
        StocktakeOrder order = requireOrder(id);
        if (order.getStatus() != 1) throw new BizException(5031, "只有进行中的盘点单可以完成");
        List<Map<String, Object>> items = itemMapper.selectByOrder(id);
        if (items.isEmpty()) throw new BizException(5032, "盘点单没有明细");

        List<Map<String, Object>> missing = items.stream()
                .filter(i -> MetricsService.intOf(i.get("counted")) != 1).toList();
        if (!missing.isEmpty()) {
            throw new BizException(5033, "还有 " + missing.size() + " 条明细未录入实盘数量，不能完成盘点");
        }

        MetricsService.Snapshot snap = metrics.snapshot(order.getWarehouseId());
        int done = 0, profit = 0, loss = 0, skip = 0;
        for (Map<String, Object> row : items) {
            BigDecimal diff = MetricsService.dec(row.get("diffQuantity"));
            if (diff.signum() == 0) {
                skip++;
                continue;
            }
            Long materialId = MetricsService.lng(row.get("materialId"));
            String batchNo = MetricsService.str(row.get("batchNo"));
            Long itemId = MetricsService.lng(row.get("itemId"));
            BigDecimal qty = diff.abs();
            try {
                if (diff.signum() > 0) {
                    inventoryClient.profit(materialId, order.getWarehouseId(), batchNo, qty,
                            order.getStocktakeId(), itemId, operatorId);
                    profit++;
                } else {
                    String scope = resolveLossScope(snap, materialId, batchNo, qty);
                    inventoryClient.loss(materialId, order.getWarehouseId(), batchNo, qty,
                            order.getStocktakeId(), itemId, operatorId, scope);
                    loss++;
                }
                done++;
            } catch (BizException e) {
                throw new BizException(5034, "第 " + done + " 条差异调账失败：" + e.getMessage()
                        + "（已成功 " + done + " 条，可修正后重新提交，幂等键保证不重复记账）");
            }
        }

        order.setStatus(2);
        order.setUpdatedBy(operatorId);
        String remark = order.getRemark() == null ? "" : order.getRemark();
        if (opinion != null && !opinion.isBlank()) {
            remark = (remark + "｜审批意见：" + opinion).trim();
        }
        order.setRemark(remark.length() > 200 ? remark.substring(0, 200) : remark);
        orderMapper.updateById(order);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("stocktakeNo", order.getStocktakeNo());
        result.put("adjusted", done);
        result.put("profit", profit);
        result.put("loss", loss);
        result.put("same", skip);
        return result;
    }

    /**
     * 盘亏差异归属：可用够就直接扣可用；不够则交给② 按「可用 → 预占 → 在借」自动分摊，
     * 避免像“领用未出库”这种跨状态差异被单一口径卡住。
     */
    private String resolveLossScope(MetricsService.Snapshot snap, Long materialId, String batchNo, BigDecimal qty) {
        for (Map<String, Object> b : snap.batchesOf(materialId)) {
            if (!MetricsService.str(b.get("batch_no")).equals(batchNo)) continue;
            BigDecimal available = MetricsService.dec(b.get("qty_available"));
            return available.compareTo(qty) >= 0 ? "AVAILABLE" : "AUTO";
        }
        return "AUTO";
    }

    private StocktakeOrder requireOrder(Long id) {
        if (id == null) throw new BizException(5004, "盘点单主键不能为空");
        StocktakeOrder order = orderMapper.selectById(id);
        if (order == null) throw new BizException(5004, "盘点单不存在：" + id);
        return order;
    }
}
