package com.school.material.purchase.service;

import com.school.material.purchase.common.BizException;
import com.school.material.purchase.entity.Stock;
import com.school.material.purchase.entity.StockFlow;
import com.school.material.purchase.entity.Warehouse;
import com.school.material.purchase.mapper.StockFlowMapper;
import com.school.material.purchase.mapper.StockMapper;
import com.school.material.purchase.mapper.WarehouseMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 库存统一写入口：其他模块只能调用本服务，不得直接更新 stock。
 * 数量规则（q 为本次数量，均指公共库存；个人保管库存可用量恒为 0）：
 * <pre>
 *   业务动作              qty_on_hand  qty_available  qty_locked
 *   合格采购入库             +q            +q            不变
 *   审批通过并预占          不变           -q             +q
 *   取消/关闭释放预占       不变           +q             -q
 *   领用出库（消耗预占）     -q           不变            -q
 *   借出（消耗预占）        不变          不变            -q
 *   借出（未提前预占）      不变           -q            不变
 *   完好归还                不变           +q            不变
 *   已借出物资报损核销       -q           不变           不变
 *   库内可用物资报废核销     -q            -q            不变
 *   科研自购进入个人仓       +q            保持 0         保持 0
 * </pre>
 * 流水与余额在同一事务提交；流水先写入以占用唯一业务操作标识，
 * 重复提交会因 uk_flow_biz_op 冲突而整体回滚。
 */
@Service
@RequiredArgsConstructor
public class InventoryService {

    private final StockMapper stockMapper;
    private final StockFlowMapper flowMapper;
    private final WarehouseMapper warehouseMapper;

    /** 库存业务类型，取值与 stock_flow.biz_type 一致（报告口径 7 类） */
    public enum Biz {
        PURCHASE_IN(1),
        OUT_USE(2),
        BORROW_OUT(3),
        RETURN_GOOD(4),
        STOCKTAKE_PROFIT(5),
        STOCKTAKE_LOSS(6),
        SCRAP(7);

        private final int code;

        Biz(int code) {
            this.code = code;
        }

        public int code() {
            return code;
        }
    }

    /** 盘亏差异归属：可领 / 预占 / 在借，三类物料扣减口径不同 */
    public enum LossScope {
        AVAILABLE, LOCKED, BORROWED, AUTO
    }

    /** 入库指令：bizItemId 为入库明细 ID，同单多明细互不覆盖 */
    public record InboundCmd(Long materialId, Long warehouseId, Long holderId, String batchNo,
                             LocalDate expireDate, LocalDate inboundDate, BigDecimal qty,
                             boolean personal, Long bizId, Long bizItemId, Long operatorId) {
    }

    /** 库存变动指令：bizId + bizItemId 构成可区分的业务操作标识 */
    public record MoveCmd(Long materialId, Long warehouseId, String batchNo, BigDecimal qty,
                          Long bizId, Long bizItemId, Long operatorId) {
    }

    /** 合格入库 / 科研自购登记：科研自购只进个人保管仓，可用量与预占量保持 0。 */
    @Transactional
    public void purchaseInbound(InboundCmd cmd) {
        requirePositive(cmd.qty(), "入库数量");
        if (cmd.personal()) {
            requirePersonalWarehouse(cmd.warehouseId());
            if (cmd.holderId() == null)
                throw new BizException(2321, "科研经费自购必须登记实际保管教师");
        } else {
            requirePublicWarehouse(cmd.warehouseId());
        }
        stockMapper.ensureRow(cmd.materialId(), cmd.warehouseId(),
                cmd.personal() ? cmd.holderId() : null, cmd.batchNo(), cmd.expireDate(), cmd.inboundDate());
        writeFlow(Biz.PURCHASE_IN, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        int done = cmd.personal()
                ? stockMapper.increasePersonal(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                        cmd.holderId(), cmd.qty())
                : stockMapper.increasePublic(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty());
        if (done != 1)
            throw new BizException(2322, "库存写入失败，请重试");
    }

    /** 审批通过并预占：可用量 -q，预占量 +q。 */
    @Transactional
    public void reserve(MoveCmd cmd) {
        requirePositive(cmd.qty(), "预占数量");
        Stock stock = lockPublicBatch(cmd, true);
        if (stock.getQtyAvailable().compareTo(cmd.qty()) < 0)
            throw new BizException(2323, "可用库存不足，预占失败");
        if (stockMapper.reserve(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2323, "可用库存不足，预占失败");
    }

    /** 取消或关闭单据释放预占：可用量 +q，预占量 -q。 */
    @Transactional
    public void releaseReserve(MoveCmd cmd) {
        requirePositive(cmd.qty(), "释放数量");
        Stock stock = lockPublicBatch(cmd, false);
        if (stock.getQtyLocked().compareTo(cmd.qty()) < 0)
            throw new BizException(2324, "释放数量超过当前预占量");
        if (stockMapper.releaseReserve(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2324, "释放数量超过当前预占量");
    }

    /** 领用出库消耗已有预占：在库 -q，预占 -q；可用量不得再扣一次。 */
    @Transactional
    public void issueReserved(MoveCmd cmd) {
        requirePositive(cmd.qty(), "出库数量");
        Stock stock = lockPublicBatch(cmd, true);
        if (stock.getQtyLocked().compareTo(cmd.qty()) < 0)
            throw new BizException(2325, "出库数量超过已预占数量，请先补足预占");
        writeFlow(Biz.OUT_USE, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty().negate(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.consumeReserved(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2325, "出库失败：预占量或在库总量不足");
    }

    /**
     * 领用出库（未提前预占）：在库 -q、可用 -q，写 2 号领用流水。
     * 用于领用单没有仓库/批次字段、审批阶段无法定位批次而无法预占的场景；
     * 出库时由库管员按先到期先出选定批次，一次性扣减在库与可用。
     */
    @Transactional
    public void issueDirect(MoveCmd cmd) {
        requirePositive(cmd.qty(), "出库数量");
        Stock stock = lockPublicBatch(cmd, true);
        if (stock.getQtyAvailable().compareTo(cmd.qty()) < 0)
            throw new BizException(2320, "可用库存不足，不能出库");
        writeFlow(Biz.OUT_USE, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty().negate(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.issueDirect(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2320, "出库失败：该批次可用量或在库量不足");
    }

    /**
     * 借出：在库总量不变。
     * reserved=true 表示已在审批阶段预占，只扣预占量；否则直接扣可用量。
     */
    @Transactional
    public void borrowOut(MoveCmd cmd, boolean reserved) {
        requirePositive(cmd.qty(), "借用数量");
        Stock stock = lockPublicBatch(cmd, true);
        if (reserved && stock.getQtyLocked().compareTo(cmd.qty()) < 0)
            throw new BizException(2326, "预占量不足，不能借出");
        if (!reserved && stock.getQtyAvailable().compareTo(cmd.qty()) < 0)
            throw new BizException(2326, "可用库存不足，不能借出");
        writeFlow(Biz.BORROW_OUT, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                BigDecimal.ZERO, cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        int done = reserved
                ? stockMapper.borrowReserved(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty())
                : stockMapper.borrowUnreserved(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty());
        if (done != 1)
            throw new BizException(2326, reserved ? "预占量不足，不能借出" : "可用库存不足，不能借出");
    }

    /** 完好归还：在库不变，可用量 +q。 */
    @Transactional
    public void returnGood(MoveCmd cmd) {
        requirePositive(cmd.qty(), "归还数量");
        lockPublicBatch(cmd, false);
        writeFlow(Biz.RETURN_GOOD, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                BigDecimal.ZERO, cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.returnGood(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2327, "归还数量超过实际借出数量");
    }

    /** 已借出物资报损核销：在库 -q，可用与预占不动。 */
    @Transactional
    public void writeOffBorrowed(MoveCmd cmd) {
        requirePositive(cmd.qty(), "核销数量");
        lockPublicBatch(cmd, false);
        writeFlow(Biz.SCRAP, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty().negate(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.writeOffBorrowed(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2328, "核销数量超过实际借出数量或库存不足");
    }

    /** 库内可用物资报废或盘亏核销：在库 -q，可用 -q。 */
    @Transactional
    public void scrapAvailable(MoveCmd cmd) {
        requirePositive(cmd.qty(), "核销数量");
        Stock stock = lockPublicBatch(cmd, false);
        if (stock.getQtyAvailable().compareTo(cmd.qty()) < 0)
            throw new BizException(2329, "可用库存不足，不能核销");
        writeFlow(Biz.SCRAP, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty().negate(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.scrapAvailable(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2329, "可用库存不足，不能核销");
    }

    /** 盘盈入库：按合格入库口径增加在库与可用，流水记盘盈。 */
    @Transactional
    public void stocktakeProfit(InboundCmd cmd) {
        requirePositive(cmd.qty(), "盘盈数量");
        requirePublicWarehouse(cmd.warehouseId());
        // 调用方未传入库日期时按当天记账，避免写入零日期导致后续查询报错
        LocalDate inboundDate = cmd.inboundDate() == null ? LocalDate.now() : cmd.inboundDate();
        stockMapper.ensureRow(cmd.materialId(), cmd.warehouseId(), null,
                cmd.batchNo(), cmd.expireDate(), inboundDate);
        writeFlow(Biz.STOCKTAKE_PROFIT, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        if (stockMapper.increasePublic(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty()) != 1)
            throw new BizException(2330, "盘盈入账失败，请重试");
    }

    /**
     * 盘亏核销：按差异所属状态分别处理。
     * AVAILABLE 扣在库与可用，LOCKED 扣在库与预占，BORROWED 只扣在库（在借物资）。
     * AUTO（盘点默认）：可用 → 预占 → 在借 依次扣减，覆盖“领用未出库”等混合差异。
     */
    @Transactional
    public void stocktakeLoss(MoveCmd cmd, LossScope scope) {
        requirePositive(cmd.qty(), "盘亏数量");
        Stock stock = lockPublicBatch(cmd, false);
        writeFlow(Biz.STOCKTAKE_LOSS, cmd.materialId(), cmd.warehouseId(), cmd.batchNo(),
                cmd.qty().negate(), cmd.bizId(), cmd.bizItemId(), cmd.operatorId());
        int done = switch (scope) {
            case AVAILABLE -> stockMapper.scrapAvailable(
                    cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty());
            case LOCKED -> stockMapper.consumeReserved(
                    cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty());
            case BORROWED -> stockMapper.writeOffBorrowed(
                    cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), cmd.qty());
            case AUTO -> deductAcrossScopes(cmd, stock);
        };
        if (done != 1)
            throw new BizException(2331, "盘亏核销失败：该批次" + lossScopeName(scope) + "数量不足");
    }

    /** 可用 → 预占 → 在借 混合扣减；任一环节数量不足则整体失败（事务回滚） */
    private int deductAcrossScopes(MoveCmd cmd, Stock stock) {
        BigDecimal remaining = cmd.qty();
        BigDecimal available = min(stock.getQtyAvailable(), remaining);
        if (available.signum() > 0) {
            if (stockMapper.scrapAvailable(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), available) != 1)
                return 0;
            remaining = remaining.subtract(available);
        }
        if (remaining.signum() > 0) {
            BigDecimal locked = min(stock.getQtyLocked(), remaining);
            if (locked.signum() > 0) {
                if (stockMapper.consumeReserved(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), locked) != 1)
                    return 0;
                remaining = remaining.subtract(locked);
            }
        }
        if (remaining.signum() > 0) {
            BigDecimal borrowed = stock.getQtyOnHand().subtract(stock.getQtyAvailable())
                    .subtract(stock.getQtyLocked());
            if (borrowed.compareTo(remaining) < 0) return 0;
            if (stockMapper.writeOffBorrowed(cmd.materialId(), cmd.warehouseId(), cmd.batchNo(), remaining) != 1)
                return 0;
        }
        return 1;
    }

    private static BigDecimal min(BigDecimal a, BigDecimal b) {
        if (a == null) return BigDecimal.ZERO;
        return a.min(b);
    }

    /** 出库候选批次：排除个人保管仓与过期批次，按先到期先出返回。 */
    public List<Stock> fifoBatchesForOutbound(Long materialId, Long warehouseId) {
        if (warehouseId != null) requirePublicWarehouse(warehouseId);
        return stockMapper.selectFifoBatches(materialId, warehouseId);
    }

    // ---------------- 内部校验 ----------------

    /** 锁定并校验公共库存批次：必须是实体仓、非个人保管库存，出库类操作还要校验到期日。 */
    private Stock lockPublicBatch(MoveCmd cmd, boolean checkExpire) {
        requirePublicWarehouse(cmd.warehouseId());
        Stock stock = stockMapper.lockOne(cmd.materialId(), cmd.warehouseId(), cmd.batchNo());
        if (stock == null)
            throw new BizException(2332, "库存批次不存在：" + cmd.batchNo());
        if (stock.getHolderId() != null)
            throw new BizException(2333, "个人保管库存不参与公共领用与借出");
        if (checkExpire && stock.getExpireDate() != null && stock.getExpireDate().isBefore(LocalDate.now()))
            throw new BizException(2334, "批次「" + cmd.batchNo() + "」已过期，不得出库，请先办理到期报废");
        return stock;
    }

    private void requirePublicWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null)
            throw new BizException(2335, "仓库不存在：" + warehouseId);
        if (warehouse.getWarehouseType() != null && warehouse.getWarehouseType() == 2)
            throw new BizException(2336, "个人保管仓不参与公共库存操作");
    }

    private void requirePersonalWarehouse(Long warehouseId) {
        Warehouse warehouse = warehouseMapper.selectById(warehouseId);
        if (warehouse == null)
            throw new BizException(2335, "仓库不存在：" + warehouseId);
        if (warehouse.getWarehouseType() == null || warehouse.getWarehouseType() != 2)
            throw new BizException(2337, "科研经费自购必须登记到虚拟个人保管仓");
    }

    private void requirePositive(BigDecimal qty, String label) {
        if (qty == null || qty.compareTo(BigDecimal.ZERO) <= 0)
            throw new BizException(2338, label + "必须大于 0");
    }

    private static String lossScopeName(LossScope scope) {
        return switch (scope) {
            case AVAILABLE -> "可用";
            case LOCKED -> "预占";
            case BORROWED -> "在借";
            case AUTO -> "库存（自动）";
        };
    }

    /** 库存流水查询（物资目录导出）：只读，最多返回 5000 条 */
    public java.util.List<java.util.Map<String, Object>> queryFlows(String keyword, LocalDateTime from, LocalDateTime to) {
        return flowMapper.selectFlows(keyword, from, to);
    }

    /** 先写流水占用业务操作标识；重复提交在这里被唯一键拦下，余额不再变动。 */
    private void writeFlow(Biz biz, Long materialId, Long warehouseId, String batchNo,
                           BigDecimal changeQty, Long bizId, Long bizItemId, Long operatorId) {
        StockFlow flow = new StockFlow();
        flow.setMaterialId(materialId);
        flow.setWarehouseId(warehouseId);
        flow.setBatchNo(batchNo);
        flow.setBizType(biz.code());
        flow.setBizId(bizId);
        flow.setBizItemId(bizItemId == null ? 0L : bizItemId);
        flow.setChangeQty(changeQty);
        flow.setOperatorId(operatorId);
        flow.setFlowTime(LocalDateTime.now());
        try {
            flowMapper.insert(flow);
        } catch (DuplicateKeyException e) {
            throw new BizException(2339, "该业务操作已入账，请勿重复提交");
        }
    }
}
