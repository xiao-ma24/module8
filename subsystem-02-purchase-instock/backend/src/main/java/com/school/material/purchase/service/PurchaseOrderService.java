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
import java.util.ArrayList;
import java.util.List;

/** 采购订单：申请转订单（含比价）→确认→（交由验收/入库推进） */
@Service
@RequiredArgsConstructor
public class PurchaseOrderService {

    private final PurchaseRequestMapper prMapper;
    private final PurchaseRequestItemMapper priMapper;
    private final PurchaseRequestItemMapper prItemMapper;
    private final PurchaseOrderMapper poMapper;
    private final PurchaseOrderItemMapper poiMapper;
    private final PurchasePlanMapper planMapper;
    private final ItemMappers itemMappers;
    private final SupplierMapper supplierMapper;
    private final UserMapper userMapper;

    public List<PurchaseOrder> list() {
        return poMapper.selectListWithNames();
    }

    public PurchaseOrder detail(Long id) {
        PurchaseOrder po = poMapper.selectDetail(id);
        if (po == null) throw new BizException(2101, "采购订单不存在");
        return po;
    }

    public List<PurchaseOrderItem> items(Long poId) {
        List<PurchaseOrderItem> items = itemMappers.selectPoItems(poId);
        for (PurchaseOrderItem it : items) enrich(it);
        return items;
    }

    private PurchaseOrderItem enrich(PurchaseOrderItem it) {
        if (it.getArrivedQty() == null) it.setArrivedQty(BigDecimal.ZERO);
        if (it.getInboundQty() == null) it.setInboundQty(BigDecimal.ZERO);
        it.setHistoryAvgPrice(poMapper.historyAvgPrice(it.getMaterialId()));
        return it;
    }

    /** 审批通过的申请单 → 采购订单。返回订单（含比价信息在明细里） */
    @Transactional
    public PurchaseOrder toOrder(Long prId, ReqDTOs.ToOrder dto) {
        PurchaseRequest pr = prMapper.selectDetail(prId);
        if (pr == null) throw new BizException(2102, "采购申请不存在");
        if (pr.getPrStatus() != 2) throw new BizException(2103, "只有已批准的申请才能生成订单");
        Supplier s = supplierMapper.selectById(dto.getSupplierId());
        if (s == null) throw new BizException(2104, "供应商不存在");
        if (s.getSupplierStatus() != 1) throw new BizException(2105, "该供应商已停止合作");
        // 下单权归采购员/管理员，部门负责人等其他角色只能发起与审批申请
        List<String> buyerRoles = userMapper.selectRoleCodes(dto.getBuyerId());
        if (buyerRoles == null || buyerRoles.stream().noneMatch(r -> "PURCHASER".equals(r) || "ADMIN".equals(r))) {
            throw new BizException(2106, "只有采购员或管理员可以生成采购订单并下单");
        }

        PurchaseOrder po = new PurchaseOrder();
        po.setPrId(prId);
        po.setSupplierId(dto.getSupplierId());
        po.setOrderDate(java.time.LocalDate.now());
        po.setPoDate(java.time.LocalDate.now());
        po.setBuyerId(dto.getBuyerId());
        po.setAcceptQty(BigDecimal.ZERO);
        po.setExpectDate(dto.getExpectDate() == null ? pr.getExpectDate() : dto.getExpectDate());
        po.setContactPerson(s.getContactPerson());
        po.setContactPhone(s.getContactPhone());
        po.setPayType(dto.getPayType() == null ? 1 : dto.getPayType());
        po.setInvoiceType(dto.getInvoiceType() == null ? 1 : dto.getInvoiceType());
        po.setPoStatus(0);
        po.setRemark(dto.getRemark());
        long c = poMapper.selectCount(Wrappers.<PurchaseOrder>lambdaQuery()
                .likeRight(PurchaseOrder::getPoNo, "PO" + NoUtil.today()));
        po.setPoNo(NoUtil.next("PO", c));

        BigDecimal total = BigDecimal.ZERO;
        BigDecimal qtyTotal = BigDecimal.ZERO;
        Long firstPlanId = null;
        Long firstMaterialId = null;
        List<PurchaseRequestItem> prItems = prItemMapper.selectList(
                Wrappers.<PurchaseRequestItem>lambdaQuery().eq(PurchaseRequestItem::getPrId, prId));
        poMapper.insert(po);
        for (PurchaseRequestItem pi : prItems) {
            BigDecimal price = pi.getRefPrice();
            if (dto.getPrices() != null) {
                price = dto.getPrices().stream().filter(p -> p.getPriId().equals(pi.getPriId()))
                        .findFirst().map(ReqDTOs.ToOrder.PriceItem::getUnitPrice).orElse(pi.getRefPrice());
            }
            // 一计划一物资：转订单时为每条明细生成已下单的采购计划，保证订单 plan_id 可追溯
            PurchasePlan plan = new PurchasePlan();
            plan.setPlanNo(NoUtil.next("CG", planMapper.maxSeq("CG" + NoUtil.today())));
            plan.setPlanType(1);
            plan.setPurchaseMode(pr.getPurchaseMode() == null ? 1 : pr.getPurchaseMode());
            plan.setDeptId(pr.getDeptId() == null ? null : String.format("%06d", pr.getDeptId()));
            plan.setHolderId(pr.getHolderId());
            plan.setMaterialId(pi.getMaterialId());
            plan.setPlanQty(pi.getQtyConfirmed());
            plan.setStockQty(BigDecimal.ZERO);
            plan.setSupplierId(dto.getSupplierId());
            plan.setExpectDate(po.getExpectDate());
            plan.setPlanStatus(3);
            plan.setCreatedBy(pr.getApplicantId());
            planMapper.insert(plan);
            if (firstPlanId == null) {
                firstPlanId = plan.getPlanId();
                firstMaterialId = pi.getMaterialId();
            }

            PurchaseOrderItem oi = new PurchaseOrderItem();
            oi.setPoId(po.getPoId());
            oi.setPlanId(plan.getPlanId());
            oi.setMaterialId(pi.getMaterialId());
            oi.setOrderQty(pi.getQtyConfirmed());
            oi.setUnitPrice(price);
            oi.setAmount(price.multiply(pi.getQtyConfirmed()).setScale(2, RoundingMode.HALF_UP));
            oi.setArrivedQty(BigDecimal.ZERO);
            oi.setInboundQty(BigDecimal.ZERO);
            poiMapper.insert(oi);
            total = total.add(oi.getAmount());
            qtyTotal = qtyTotal.add(pi.getQtyConfirmed());
        }
        PurchaseOrder up = new PurchaseOrder();
        up.setPoId(po.getPoId());
        up.setPlanId(firstPlanId);
        up.setMaterialId(firstMaterialId);
        up.setPoQty(qtyTotal);
        up.setUnitPrice(qtyTotal.signum() > 0
                ? total.divide(qtyTotal, 2, RoundingMode.HALF_UP) : BigDecimal.ZERO);
        up.setAmount(total);
        up.setTotalAmount(total);
        poMapper.updateById(up);

        PurchaseRequest pru = new PurchaseRequest();
        pru.setPrId(prId);
        pru.setPrStatus(4);
        prMapper.updateById(pru);
        return detail(po.getPoId());
    }

    /** 确认订单（通知供应商发货） */
    @Transactional
    public void confirm(Long id) {
        PurchaseOrder po = detail(id);
        if (po.getPoStatus() != 0) throw new BizException(2106, "只有待确认订单可以确认");
        PurchaseOrder up = new PurchaseOrder();
        up.setPoId(id);
        up.setPoStatus(1);
        poMapper.updateById(up);
    }

    /** 取消订单 */
    @Transactional
    public void cancel(Long id) {
        PurchaseOrder po = detail(id);
        if (po.getPoStatus() != 0 && po.getPoStatus() != 1)
            throw new BizException(2107, "已进入到货环节的订单不能取消");
        PurchaseOrder up = new PurchaseOrder();
        up.setPoId(id);
        up.setPoStatus(4);
        poMapper.updateById(up);
    }

    /** 可验收订单：已确认/部分到货，且仍有未到货数量（全部到货的订单不再出现在到货登记中） */
    public List<PurchaseOrder> receivable() {
        List<Long> receivableIds = poiMapper.selectReceivablePoIds();
        return poMapper.selectListWithNames().stream()
                .filter(p -> (p.getPoStatus() == 1 || p.getPoStatus() == 2)
                        && receivableIds.contains(p.getPoId()))
                .toList();
    }
}
