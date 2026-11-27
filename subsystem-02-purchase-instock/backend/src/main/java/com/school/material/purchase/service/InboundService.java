package com.school.material.purchase.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.purchase.common.BizException;
import com.school.material.purchase.common.NoUtil;
import com.school.material.purchase.dto.ReqDTOs;
import com.school.material.purchase.entity.*;
import com.school.material.purchase.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/** 入库：批次/质保期维护→确认入库（联动库存+流水+订单进度+预算扣减）→财务凭证回填 */
@Service
@RequiredArgsConstructor
public class InboundService {

    private final InboundOrderMapper inMapper;
    private final InboundItemMapper iiMapper;
    private final ItemMappers itemMappers;
    private final PurchaseOrderMapper poMapper;
    private final PurchaseOrderItemMapper poiMapper;
    private final BudgetMapper budgetMapper;
    private final PurchaseRequestMapper prMapper;
    private final SupplierMapper supplierMapper;
    private final MaterialMapper materialMapper;
    private final InventoryService inventoryService;
    private final MaterialInboundInterfaceMapper costInterfaceMapper;

    public List<InboundOrder> list() {
        return inMapper.selectListWithNames();
    }

    public InboundOrder detail(Long id) {
        InboundOrder io = inMapper.selectDetail(id);
        if (io == null) throw new BizException(2301, "入库单不存在");
        return io;
    }

    public List<InboundItem> items(Long inId) {
        return itemMappers.selectInItems(inId);
    }

    /** 入库前补充批次号/生产日期/质保期 */
    @Transactional
    public void fillExtras(Long inId, List<ReqDTOs.InboundExtra> extras) {
        detail(inId);
        if (extras == null) return;
        for (ReqDTOs.InboundExtra e : extras) {
            InboundItem up = new InboundItem();
            up.setIiId(e.getIiId());
            up.setBatchNo(e.getBatchNo());
            up.setProduceDate(e.getProduceDate());
            up.setExpireDate(e.getExpireDate());
            iiMapper.updateById(up);
        }
    }

    /** 确认入库：库存增量 + 流水 + 订单累计 + 预算扣减 + 状态推进 */
    @Transactional
    public void confirm(Long inId) {
        InboundOrder io = detail(inId);
        if (io.getInStatus() != 0) throw new BizException(2302, "该入库单已入库，请勿重复操作");

        List<InboundItem> items = itemMappers.selectInItems(inId);
        if (items.isEmpty()) throw new BizException(2303, "入库单没有明细");

        // 供应商合作状态校验：停止合作的供应商不得进入正常入库
        PurchaseOrder linkedPo = io.getPoId() == null ? null : poMapper.selectDetail(io.getPoId());
        if (linkedPo == null) throw new BizException(2307, "入库单未关联采购订单，不能入库");
        Supplier supplier = supplierMapper.selectById(linkedPo.getSupplierId());
        if (supplier == null)
            throw new BizException(2308, "采购订单关联的供应商不存在，不能入库");
        if (supplier.getSupplierStatus() == null || supplier.getSupplierStatus() != 1)
            throw new BizException(2309, "供应商「" + supplier.getSupplierName() + "」已停止合作，不能确认入库");

        PurchaseRequest sourceRequest = linkedPo.getPrId() == null ? null : prMapper.selectById(linkedPo.getPrId());
        int purchaseMode = io.getPurchaseMode() == null
                ? (sourceRequest == null || sourceRequest.getPurchaseMode() == null ? 1 : sourceRequest.getPurchaseMode())
                : io.getPurchaseMode();
        Long holderId = io.getHolderId() == null && sourceRequest != null ? sourceRequest.getHolderId() : io.getHolderId();
        if (purchaseMode == 2 && holderId == null)
            throw new BizException(2310, "科研经费自购必须登记实际保管教师");
        LocalDate inboundDate = LocalDate.now();

        for (InboundItem ii : items) {
            if (ii.getBatchNo() == null || ii.getBatchNo().isBlank())
                throw new BizException(2304, "物资「" + ii.getMaterialName() + "」未登记批次号");
            Material material = materialMapper.selectById(ii.getMaterialId());
            if (material == null) throw new BizException(2311, "入库物资目录不存在：" + ii.getMaterialId());
            validateAndFillLifecycle(ii, material, inboundDate);

            // 统一库存入口：同一入库明细只入账一次，余额、流水与单据状态同事务提交。
            inventoryService.purchaseInbound(new InventoryService.InboundCmd(
                    ii.getMaterialId(), io.getWarehouseId(),
                    purchaseMode == 2 ? holderId : null, ii.getBatchNo(),
                    ii.getExpireDate(), inboundDate, ii.getInQty(),
                    purchaseMode == 2, inId, ii.getIiId(), io.getKeeperId()));

            // 成本接口（报告表2-11）：与库存、流水同事务输出实际入库单价，供第5组财务成本结转
            writeCostInterface(io, ii, inboundDate);

            PurchaseOrderItem poi = poiMapper.selectOne(Wrappers.<PurchaseOrderItem>lambdaQuery()
                    .eq(PurchaseOrderItem::getPoId, io.getPoId())
                    .eq(PurchaseOrderItem::getMaterialId, ii.getMaterialId()));
            if (poi != null) {
                poi.setInboundQty(nz(poi.getInboundQty()).add(ii.getInQty()));
                poiMapper.updateById(poi);
            }
        }

        // 预算扣减（来源申请单带预算科目时）
        if (io.getPoId() != null) {
            PurchaseOrder po = poMapper.selectDetail(io.getPoId());
            if (po.getPrId() != null) {
                PurchaseRequest pr = prMapper.selectById(po.getPrId());
                if (pr != null && pr.getBudgetNo() != null && io.getTotalAmount() != null) {
                    budgetMapper.consume(pr.getBudgetNo(), io.getTotalAmount());
                }
                // 订单完成判定：全部明细入库量 达到 订购量-退货量
                List<PurchaseOrderItem> pois = poiMapper.selectList(Wrappers.<PurchaseOrderItem>lambdaQuery()
                        .eq(PurchaseOrderItem::getPoId, io.getPoId()));
                boolean done = pois.stream().allMatch(p ->
                        nz(p.getInboundQty()).add(returnedQty()).compareTo(p.getOrderQty()) >= 0);
                if (done) {
                    PurchaseOrder up = new PurchaseOrder();
                    up.setPoId(io.getPoId());
                    up.setPoStatus(3);
                    poMapper.updateById(up);
                }
            }
        }

        String voucherNo = null;
        if (purchaseMode == 2 && (io.getVoucherNo() == null || io.getVoucherNo().isBlank())) {
            long count = inMapper.selectCount(Wrappers.<InboundOrder>lambdaQuery()
                    .likeRight(InboundOrder::getVoucherNo, "BX" + NoUtil.today()));
            voucherNo = NoUtil.next("BX", count);
            if (sourceRequest != null) {
                PurchaseRequest requestUp = new PurchaseRequest();
                requestUp.setPrId(sourceRequest.getPrId());
                requestUp.setReimburseVoucherNo(voucherNo);
                prMapper.updateById(requestUp);
            }
        }

        // 状态推进以 in_status=0 为条件：并发确认时只有一个事务能成功，另一个整体回滚。
        int done = inMapper.update(null, Wrappers.<InboundOrder>lambdaUpdate()
                .eq(InboundOrder::getInId, inId)
                .eq(InboundOrder::getInStatus, 0)
                .set(InboundOrder::getInStatus, 1)
                .set(InboundOrder::getInDate, LocalDateTime.now())
                .set(InboundOrder::getPurchaseMode, purchaseMode)
                .set(InboundOrder::getHolderId, purchaseMode == 2 ? holderId : null)
                .set(voucherNo != null, InboundOrder::getVoucherNo, voucherNo));
        if (done != 1)
            throw new BizException(2313, "该入库单已被其他操作入库，请刷新后重试");
    }

    /** 该明细被退货的数量（验收处置=退货 2 的部分视为终态）。演示版返回 0；正式版按验收明细累计 */
    private BigDecimal returnedQty() {
        return BigDecimal.ZERO;
    }

    /** 财务回填报销凭证号（模拟第5组财务系统回调） */
    @Transactional
    public void fillVoucher(Long inId, ReqDTOs.VoucherFill dto) {
        InboundOrder io = detail(inId);
        if (io.getInStatus() != 1) throw new BizException(2306, "入库完成后才能回填凭证号");
        InboundOrder up = new InboundOrder();
        up.setInId(inId);
        up.setVoucherNo(dto.getVoucherNo());
        inMapper.updateById(up);
    }

    /** 临期预警：30天内到期的入库批次 */
    public List<InboundItem> expiring() {
        List<InboundItem> all = new java.util.ArrayList<>();
        for (InboundOrder io : list()) {
            if (io.getInStatus() == 1) all.addAll(itemMappers.selectInItems(io.getInId()));
        }
        LocalDate limit = LocalDate.now().plusDays(30);
        return all.stream().filter(i -> i.getExpireDate() != null && !i.getExpireDate().isAfter(limit)).toList();
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    /** 写成本接口表：一行对应「入库单 + 物资」，金额=实际入库单价×入库数量，保留两位小数。 */
    private void writeCostInterface(InboundOrder io, InboundItem ii, LocalDate inboundDate) {
        BigDecimal price = nz(ii.getUnitPrice()).setScale(2, RoundingMode.HALF_UP);
        BigDecimal amount = ii.getAmount() == null
                ? price.multiply(ii.getInQty()).setScale(2, RoundingMode.HALF_UP)
                : ii.getAmount().setScale(2, RoundingMode.HALF_UP);
        MaterialInboundInterface row = new MaterialInboundInterface();
        row.setInNo(io.getInNo());
        row.setMaterialId(ii.getMaterialId());
        row.setInPrice(price);
        row.setInQty(ii.getInQty());
        row.setInAmount(amount);
        row.setInDate(inboundDate);
        row.setWarehouseId(io.getWarehouseId());
        costInterfaceMapper.upsert(row);
    }

    /** 按数据字典补齐批次生命周期信息；预警模块据此扫描到期和限用年限。 */
    private void validateAndFillLifecycle(InboundItem item, Material material, LocalDate inboundDate) {
        if (Integer.valueOf(2).equals(material.getMaterialType())
                && (material.getServiceLifeYears() == null || material.getServiceLifeYears() < 1)) {
            throw new BizException(2312, "低值耐用品「" + material.getMaterialName() + "」必须维护限用年限");
        }
        if (material.getShelfLifeDays() != null && material.getShelfLifeDays() > 0 && item.getExpireDate() == null) {
            LocalDate baseDate = item.getProduceDate() == null ? inboundDate : item.getProduceDate();
            item.setExpireDate(baseDate.plusDays(material.getShelfLifeDays()));
            iiMapper.updateById(item);
        }
    }
}
