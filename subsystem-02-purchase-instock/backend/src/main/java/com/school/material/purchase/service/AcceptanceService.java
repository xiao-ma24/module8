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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 到货验收：登记→逐项核验→完成（生成入库单 + 供应商评级 + 验收报告） */
@Service
@RequiredArgsConstructor
public class AcceptanceService {

    private final AcceptanceOrderMapper acptMapper;
    private final AcceptanceItemMapper aiMapper;
    private final PurchaseOrderMapper poMapper;
    private final PurchaseOrderItemMapper poiMapper;
    private final InboundOrderMapper inMapper;
    private final InboundItemMapper iiMapper;
    private final ItemMappers itemMappers;
    private final SupplierMapper supplierMapper;
    private final PurchaseRequestMapper prMapper;
    private final WarehouseMapper warehouseMapper;
    private final UserMapper userMapper;
    private final AiService aiService;

    public List<AcceptanceOrder> list() {
        return acptMapper.selectListWithNames();
    }

    public AcceptanceOrder detail(Long id) {
        AcceptanceOrder a = acptMapper.selectDetail(id);
        if (a == null) throw new BizException(2201, "验收单不存在");
        return a;
    }

    public List<AcceptanceItem> items(Long acptId) {
        return itemMappers.selectAcptItems(acptId);
    }

    /** 到货登记：核验数量不能超过订单剩余应到数量 */
    @Transactional
    public AcceptanceOrder create(ReqDTOs.AcptCreate dto) {
        PurchaseOrder po = poMapper.selectDetail(dto.getPoId());
        if (po == null) throw new BizException(2202, "订单不存在");
        checkSupplierActive(po.getSupplierId());
        if (po.getPoStatus() != 1 && po.getPoStatus() != 2)
            throw new BizException(2203, "订单未确认或已完结，不能验收");
        if (dto.getItems() == null || dto.getItems().isEmpty())
            throw new BizException(2204, "验收明细不能为空");

        AcceptanceOrder a = new AcceptanceOrder();
        a.setPoId(dto.getPoId());
        a.setAcptDate(LocalDate.now());
        a.setInspectorId(dto.getInspectorId());
        a.setDeliverer(dto.getDeliverer());
        a.setPhotoUrls(dto.getPhotoUrls());
        a.setAcptStatus(1);
        long c = acptMapper.selectCount(Wrappers.<AcceptanceOrder>lambdaQuery()
                .likeRight(AcceptanceOrder::getAcptNo, "YS" + NoUtil.today()));
        a.setAcptNo(NoUtil.next("YS", c));
        acptMapper.insert(a);

        for (ReqDTOs.AcptCreate.AcptItem i : dto.getItems()) {
            PurchaseOrderItem poi = poiMapper.selectById(i.getPoiId());
            if (poi == null) throw new BizException(2205, "订单明细不存在");
            BigDecimal remaining = poi.getOrderQty()
                    .subtract(poi.getArrivedQty() == null ? BigDecimal.ZERO : poi.getArrivedQty())
                    // 已登记但未完成验收的批次也占用剩余额度，避免重复登记造成超发
                    .subtract(itemMappers.selectPendingCheckQty(poi.getPoiId()));
            if (i.getCheckQty() == null || i.getCheckQty().signum() <= 0)
                throw new BizException(2206, "送检数量必须大于0");
            if (i.getCheckQty().compareTo(remaining) > 0)
                throw new BizException(2207, "送检数量超过订单剩余应到数量（剩余 " + remaining.stripTrailingZeros().toPlainString() + "）");
            if (i.getPassQty() == null) i.setPassQty(BigDecimal.ZERO);
            BigDecimal fail = i.getCheckQty().subtract(i.getPassQty());
            if (fail.signum() < 0) throw new BizException(2208, "合格数量不能超过送检数量");
            if (fail.signum() > 0 && (i.getHandleType() == null || i.getHandleType() == 1))
                throw new BizException(2209, "存在不合格数量，必须选择退货或换货");

            AcceptanceItem ai = new AcceptanceItem();
            ai.setAcptId(a.getAcptId());
            ai.setPoiId(i.getPoiId());
            ai.setMaterialId(poi.getMaterialId());
            ai.setCheckQty(i.getCheckQty());
            ai.setPassQty(i.getPassQty());
            ai.setFailQty(fail);
            ai.setFailReason(i.getFailReason());
            ai.setHandleType(i.getHandleType() == null ? 1 : i.getHandleType());
            // 合格入库项无需后续处理；退货/换货项进入待处理，由库管员跟踪闭环
            ai.setHandleStatus(ai.getHandleType() == 1 ? 1 : 0);
            aiMapper.insert(ai);
        }
        return detail(a.getAcptId());
    }

    /** 完成验收：回写到货量、刷新供应商评级、生成验收报告、自动生成待入库单 */
    @Transactional
    public InboundOrder complete(Long acptId, ReqDTOs.AcptComplete dto) {
        AcceptanceOrder a = detail(acptId);
        if (a.getAcptStatus() != 1) throw new BizException(2210, "验收单不在验收中状态");
        if (dto.getAcptScore() == null || dto.getAcptScore() < 1 || dto.getAcptScore() > 5)
            throw new BizException(2211, "验收评分必须为 1~5 分");
        List<AcceptanceItem> items = itemMappers.selectAcptItems(acptId);
        if (items.isEmpty()) throw new BizException(2212, "验收单没有明细");

        // 岗位分离（文档 2.1）：同一笔采购验收业务中，验收人与库管员原则上分离
        // 以当前执行人（operatorId）为准，避免验收单登记人与实际操作人不一致时误判
        Long operatorId = dto.getOperatorId() != null ? dto.getOperatorId() : a.getInspectorId();
        // 岗位权限：只有验收人（或管理员）可执行验收确认，采购员仅负责到货登记
        if (operatorId != null) {
            List<String> roles = userMapper.selectRoleCodes(operatorId);
            boolean canAccept = roles != null && roles.stream()
                    .anyMatch(r -> "CHECKER".equals(r) || "ADMIN".equals(r));
            if (!canAccept)
                throw new BizException(2224, "仅验收人可执行验收确认，采购员请完成到货登记后交由验收人核验");
        }
        if (dto.getKeeperId() != null && dto.getKeeperId().equals(operatorId))
            throw new BizException(2221, "同一笔验收业务中库管员与验收人不能为同一人，请选择其他库管员");

        PurchaseOrder po = poMapper.selectDetail(a.getPoId());
        PurchaseRequest pr = po.getPrId() == null ? null : prMapper.selectById(po.getPrId());
        int purchaseMode = pr == null || pr.getPurchaseMode() == null ? 1 : pr.getPurchaseMode();
        Long holderId = pr == null ? null : pr.getHolderId();
        if (purchaseMode == 2 && holderId == null)
            throw new BizException(2222, "科研经费自购必须指定实际保管教师，不能进入公共库存");
        Long targetWarehouseId = purchaseMode == 2 ? ensurePersonalWarehouse(holderId) : dto.getWarehouseId();
        if (targetWarehouseId == null)
            throw new BizException(2223, "请选择入库仓库");
        // 先写验收文字，再确认验收：验收情况说明为必填
        if (dto.getReport() == null || dto.getReport().isBlank())
            throw new BizException(2225, "请先填写验收报告（验收情况说明）再确认验收");
        // 验收人核验：更新送检/合格/不合格数量（采购员到货登记时只登记到货数量）
        if (dto.getItems() != null && !dto.getItems().isEmpty()) {
            Map<Long, AcceptanceItem> byId = items.stream()
                    .collect(Collectors.toMap(AcceptanceItem::getAiId, i -> i));
            for (ReqDTOs.AcptComplete.AcptItemCheck c : dto.getItems()) {
                AcceptanceItem ai = byId.get(c.getItemId());
                if (ai == null) throw new BizException(2212, "验收明细不存在，请刷新后重试");
                if (c.getCheckQty() == null || c.getCheckQty().signum() <= 0)
                    throw new BizException(2206, "送检数量必须大于0");
                BigDecimal pass = c.getPassQty() == null ? c.getCheckQty() : c.getPassQty();
                if (pass.signum() < 0 || pass.compareTo(c.getCheckQty()) > 0)
                    throw new BizException(2208, "合格数量必须在 0 ~ 送检数量之间");
                BigDecimal fail = c.getCheckQty().subtract(pass);
                if (fail.signum() > 0 && (c.getHandleType() == null || c.getHandleType() == 1))
                    throw new BizException(2209, "存在不合格数量，必须选择退货或换货");
                ai.setCheckQty(c.getCheckQty());
                ai.setPassQty(pass);
                ai.setFailQty(fail);
                ai.setHandleType(c.getHandleType() == null ? 1 : c.getHandleType());
                ai.setFailReason(c.getFailReason());
                ai.setHandleStatus(ai.getHandleType() == 1 ? 1 : 0);
                aiMapper.updateById(ai);
            }
        }
        BigDecimal passTotal = BigDecimal.ZERO;
        BigDecimal acceptTotal = BigDecimal.ZERO;
        for (AcceptanceItem ai : items) {
            PurchaseOrderItem poi = poiMapper.selectById(ai.getPoiId());
            BigDecimal newArrived = nz(poi.getArrivedQty()).add(ai.getCheckQty());
            if (newArrived.compareTo(poi.getOrderQty()) > 0)
                throw new BizException(2226, "累计到货数量将超过订单订购数量（订购 "
                        + poi.getOrderQty().stripTrailingZeros().toPlainString() + "），请核对验收单");
            poi.setArrivedQty(newArrived);
            poiMapper.updateById(poi);
            acceptTotal = acceptTotal.add(nz(poi.getArrivedQty()));
            // 合格数量无论整批处置方式如何都要入库，不合格部分另行退货/换货
            if (nz(ai.getPassQty()).signum() > 0) {
                passTotal = passTotal.add(ai.getPassQty().multiply(poi.getUnitPrice()));
            }
        }
        // 报告表2-10：accept_qty 累计验收数量由到货验收回写
        PurchaseOrder poAccept = new PurchaseOrder();
        poAccept.setPoId(po.getPoId());
        poAccept.setAcceptQty(acceptTotal);
        poMapper.updateById(poAccept);

        // 刷新供应商评级（评分6 : 历史星级4 加权）
        poMapper.refreshStar(po.getSupplierId(), dto.getAcptScore());

        // 验收报告：保存验收人填写的文字，AI 生成的报告仅作辅助草稿
        a.setAcptScore(dto.getAcptScore());
        a.setQualityReport(dto.getReport().trim());
        a.setAcptStatus(items.stream().anyMatch(i -> nz(i.getFailQty()).signum() > 0) ? 3 : 2);
        acptMapper.updateById(a);

        // 生成待入库单：仅合格数量（passQty > 0）入库，全部不合格则不生成空入库单
        boolean hasStock = items.stream().anyMatch(i -> nz(i.getPassQty()).signum() > 0);
        InboundOrder io = null;
        if (hasStock) {
            io = new InboundOrder();
            io.setAcptId(acptId);
            io.setPoId(a.getPoId());
            io.setWarehouseId(targetWarehouseId);
            io.setKeeperId(dto.getKeeperId());
            io.setPurchaseMode(purchaseMode);
            io.setHolderId(holderId);
            io.setTotalAmount(passTotal);
            io.setInStatus(0);
            long c = inMapper.selectCount(Wrappers.<InboundOrder>lambdaQuery()
                    .likeRight(InboundOrder::getInNo, "RK" + NoUtil.today()));
            io.setInNo(NoUtil.next("RK", c));
            inMapper.insert(io);

            int seq = 0;
            for (AcceptanceItem ai : items) {
                if (nz(ai.getPassQty()).signum() <= 0) continue;
                PurchaseOrderItem poi = poiMapper.selectById(ai.getPoiId());
                seq++;
                InboundItem ii = new InboundItem();
                ii.setInId(io.getInId());
                ii.setMaterialId(ai.getMaterialId());
                ii.setBatchNo(io.getInNo() + "-" + seq);                       // 默认批次号，入库前可修改
                ii.setInQty(ai.getPassQty());
                ii.setUnitPrice(poi.getUnitPrice());
                ii.setAmount(poi.getUnitPrice().multiply(ai.getPassQty()));
                ii.setQrCode(po.getPoNo() + "-" + ai.getMaterialId() + "-" + io.getInNo() + seq);
                iiMapper.insert(ii);
            }
        }

        // 订单状态：全部到货 → 部分到货/全部到货仍标记2，完成交给入库
        boolean allArrived = items.stream().allMatch(i -> {
            PurchaseOrderItem poi = poiMapper.selectById(i.getPoiId());
            return nz(poi.getArrivedQty()).compareTo(poi.getOrderQty()) >= 0;
        });
        if (allArrived && po.getPoStatus() == 1) {
            PurchaseOrder up = new PurchaseOrder();
            up.setPoId(po.getPoId());
            up.setPoStatus(2);
            poMapper.updateById(up);
        }
        return io;
    }

    /**
     * 科研经费自购物资不进入公共仓库。系统为保管教师创建（或复用）虚拟个人保管仓，
     * 这样库存流水仍有仓库维度，同时公共领用接口不会误把该物资当作可领库存。
     */
    private Long ensurePersonalWarehouse(Long holderId) {
        String name = "个人保管仓-" + holderId;
        Warehouse existing = warehouseMapper.selectOne(Wrappers.<Warehouse>lambdaQuery()
                .eq(Warehouse::getWarehouseName, name));
        if (existing != null) return existing.getWarehouseId();
        Warehouse warehouse = new Warehouse();
        warehouse.setWarehouseName(name);
        warehouse.setWarehouseType(2);
        warehouse.setLocation("虚拟个人保管库");
        warehouseMapper.insert(warehouse);
        return warehouse.getWarehouseId();
    }

    private String buildReport(AcceptanceOrder a, PurchaseOrder po, List<AcceptanceItem> items) {
        StringBuilder sb = new StringBuilder();
        sb.append("验收报告（").append(a.getAcptNo()).append("）\n")
          .append("关联订单：").append(po.getPoNo()).append("　验收人：").append(a.getInspectorName())
          .append("　验收日期：").append(a.getAcptDate()).append("\n")
          .append("验收评分：").append(a.getAcptScore() == null ? "待评" : a.getAcptScore() + " 分").append("\n");
        BigDecimal check = BigDecimal.ZERO, pass = BigDecimal.ZERO;
        for (AcceptanceItem i : items) {
            check = check.add(i.getCheckQty());
            pass = pass.add(i.getPassQty());
            if (nz(i.getFailQty()).signum() > 0) {
                sb.append("异常项：").append(i.getMaterialName())
                  .append("，不合格 ").append(i.getFailQty().stripTrailingZeros().toPlainString())
                  .append("，原因：").append(i.getFailReason() == null ? "未填写" : i.getFailReason())
                  .append("，处置：").append(i.getHandleType() == 2 ? "退货" : "换货").append("\n");
            }
        }
        sb.append("合计送检 ").append(check.stripTrailingZeros().toPlainString())
          .append("，合格 ").append(pass.stripTrailingZeros().toPlainString())
          .append("，合格率 ").append(check.signum() == 0 ? "—"
                  : pass.multiply(BigDecimal.valueOf(100)).divide(check, 1, java.math.RoundingMode.HALF_UP) + "%");
        return sb.toString();
    }

    /** AI 润色验收报告；验收中尚未填写报告时，先按当前核验明细生成草稿 */
    public String aiReport(Long acptId, ReqDTOs.AcptComplete dto) {
        AcceptanceOrder a = detail(acptId);
        PurchaseOrder po = poMapper.selectDetail(a.getPoId());
        List<AcceptanceItem> items = items(acptId);
        String base;
        boolean hasDraft = dto != null && dto.getItems() != null && !dto.getItems().isEmpty();
        if (hasDraft) {
            // 按页面当前填写的送检/合格/不合格数据生成草稿，避免沿用登记时的“全合格”默认值
            java.util.Map<Long, ReqDTOs.AcptComplete.AcptItemCheck> byId = new java.util.HashMap<>();
            for (ReqDTOs.AcptComplete.AcptItemCheck c : dto.getItems()) {
                if (c.getItemId() != null) byId.put(c.getItemId(), c);
            }
            for (AcceptanceItem ai : items) {
                ReqDTOs.AcptComplete.AcptItemCheck c = byId.get(ai.getAiId());
                if (c == null) continue;
                BigDecimal check = c.getCheckQty() == null ? nz(ai.getCheckQty()) : c.getCheckQty();
                BigDecimal pass = c.getPassQty() == null ? check : c.getPassQty();
                if (pass.compareTo(check) > 0) pass = check;
                ai.setCheckQty(check);
                ai.setPassQty(pass);
                ai.setFailQty(check.subtract(pass));
                if (c.getHandleType() != null) ai.setHandleType(c.getHandleType());
                ai.setFailReason(c.getFailReason());
            }
            if (dto.getAcptScore() != null) a.setAcptScore(dto.getAcptScore());
            base = buildReport(a, po, items);
        } else {
            base = a.getQualityReport();
            if (base == null || base.isBlank()) base = buildReport(a, po, items);
        }
        String r = aiService.polish(base);
        AcceptanceOrder up = new AcceptanceOrder();
        up.setAcptId(acptId);
        up.setQualityReport(r);
        acptMapper.updateById(up);
        return r;
    }

    /** 退换货跟踪：不合格且处置为退货/换货的明细 */
    public List<AcceptanceItem> returns() {
        return itemMappers.selectReturns();
    }

    /** 标记退换货已处理完成（退货已退回供应商 / 换货已完成补货） */
    @Transactional
    public void finishReturn(Long aiId, String handler) {
        AcceptanceItem ai = aiMapper.selectById(aiId);
        if (ai == null) throw new BizException(2214, "验收明细不存在");
        if (ai.getHandleType() == null || ai.getHandleType() == 1)
            throw new BizException(2215, "该明细为合格入库项，无需退换货处理");
        if (nz(ai.getFailQty()).signum() <= 0)
            throw new BizException(2216, "该明细没有不合格数量，无需退换货处理");
        if (ai.getHandleStatus() != null && ai.getHandleStatus() == 1)
            throw new BizException(2217, "该退换货已处理完成，请勿重复操作");
        AcceptanceItem up = new AcceptanceItem();
        up.setAiId(aiId);
        up.setHandleStatus(1);
        up.setHandledAt(java.time.LocalDateTime.now());
        up.setHandledBy(handler == null || handler.isBlank() ? "库管员" : handler);
        aiMapper.updateById(up);

        // 换货完成：不合格数量退回订单待处理，允许重新登记到货并走验收/入库，避免流程卡死
        if (ai.getHandleType() != null && ai.getHandleType() == 3 && nz(ai.getFailQty()).signum() > 0) {
            AcceptanceOrder ao = acptMapper.selectById(ai.getAcptId());
            PurchaseOrderItem poi = poiMapper.selectById(ai.getPoiId());
            if (poi != null) {
                BigDecimal arrived = nz(poi.getArrivedQty()).subtract(nz(ai.getFailQty()));
                if (arrived.signum() < 0) arrived = BigDecimal.ZERO;
                PurchaseOrderItem pu = new PurchaseOrderItem();
                pu.setPoiId(poi.getPoiId());
                pu.setArrivedQty(arrived);
                poiMapper.updateById(pu);
                if (ao != null) {
                    PurchaseOrder po = poMapper.selectById(ao.getPoId());
                    if (po != null && po.getPoStatus() != null && po.getPoStatus() == 3) {
                        PurchaseOrder pou = new PurchaseOrder();
                        pou.setPoId(po.getPoId());
                        pou.setPoStatus(2);   // 回到部分到货，可继续登记换货到货
                        poMapper.updateById(pou);
                    }
                }
            }
        }
    }

    /** 供应商合作状态校验：仅「合作中」(supplier_status=1) 可进入正常到货验收 */
    private void checkSupplierActive(Long supplierId) {
        if (supplierId == null) throw new BizException(2218, "订单未关联供应商，不能验收");
        Supplier s = supplierMapper.selectById(supplierId);
        if (s == null) throw new BizException(2219, "供应商不存在，不能验收");
        if (s.getSupplierStatus() == null || s.getSupplierStatus() != 1)
            throw new BizException(2220, "供应商「" + s.getSupplierName() + "」已停止合作，不能进入正常到货验收");
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
