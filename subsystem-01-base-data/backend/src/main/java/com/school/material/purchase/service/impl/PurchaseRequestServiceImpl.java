package com.school.material.purchase.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.entity.Material;
import com.school.material.mapper.MaterialMapper;
import com.school.material.mapper.WarehouseMapper;
import com.school.material.purchase.dto.PurchaseAuditReq;
import com.school.material.purchase.dto.PurchaseRecordVO;
import com.school.material.purchase.dto.PurchaseRegisterReq;
import com.school.material.purchase.dto.PurchaseRegisterResp;
import com.school.material.purchase.entity.PurchaseRequest;
import com.school.material.purchase.entity.PurchaseRequestItem;
import com.school.material.purchase.mapper.PurchaseRequestItemMapper;
import com.school.material.purchase.mapper.PurchaseRequestMapper;
import com.school.material.purchase.service.PurchaseRequestService;
import com.school.material.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PurchaseRequestServiceImpl implements PurchaseRequestService {

    /** 接口层审核状态：0待验收 1已入库 2已驳回 */
    private static final int JSON_PENDING = 0;
    private static final int JSON_STORED = 1;
    private static final int JSON_REJECTED = 2;

    /** purchase_request.pr_status：1待验收 2已入库 3已驳回 */
    private static final int DB_PENDING = 1;
    private static final int DB_STORED = 2;
    private static final int DB_REJECTED = 3;

    private static final int PURCHASE_MODE_SELF = 2;
    private static final int SOURCE_TYPE_LAB = 4;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final PurchaseRequestMapper purchaseRequestMapper;
    private final PurchaseRequestItemMapper purchaseRequestItemMapper;
    private final StockService stockService;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseRegisterResp register(PurchaseRegisterReq req) {
        Material material = materialMapper.selectById(req.getMaterialId());
        if (material == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "物资不存在");
        }
        if (warehouseMapper.selectById(req.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "仓库不存在");
        }

        // 发票号即唯一幂等键：一发票一物资一行
        PurchaseRequest existing = purchaseRequestMapper.selectOne(
                new LambdaQueryWrapper<PurchaseRequest>()
                        .eq(PurchaseRequest::getReimburseVoucherNo, req.getInvoiceNo().trim()));
        if (existing != null) {
            PurchaseRequestItem item = firstItem(existing.getPrId());
            Long materialId = item == null ? req.getMaterialId() : item.getMaterialId();
            Long warehouseId = existing.getWarehouseId() == null ? req.getWarehouseId() : existing.getWarehouseId();
            return toResp(existing, item, stockService.currentQtyOnHand(materialId, warehouseId), true);
        }

        LocalDateTime now = LocalDateTime.now();
        String registrarUserNo = req.getRegistrarUserId().trim();
        Long applicantId = purchaseRequestMapper.selectUserIdByNo(registrarUserNo);
        Long applicant = applicantId == null ? 0L : applicantId;
        // 补齐申请人归属部门与用途，避免采购工作台展示为空（purchase_request.dept_id 为 BIGINT，按组织编码数值存入）
        String orgId = applicantId == null ? null : purchaseRequestMapper.selectDeptIdByUserId(applicantId);
        Long deptId = orgId == null || orgId.isBlank() ? null : Long.valueOf(orgId);
        String purpose = req.getRemark() == null || req.getRemark().isBlank()
                ? "实验室自备（个人发票登记，个人保管）"
                : req.getRemark().trim();

        BigDecimal refPrice = material.getReferencePrice() == null ? BigDecimal.ZERO : material.getReferencePrice();
        BigDecimal amount = refPrice.multiply(req.getQtyApply());

        PurchaseRequest pr = new PurchaseRequest();
        pr.setPrNo(nextPrNo());
        pr.setSourceType(SOURCE_TYPE_LAB);
        pr.setPurchaseMode(PURCHASE_MODE_SELF);
        pr.setApplicantId(applicant);
        pr.setDeptId(deptId == null ? 0L : deptId);
        pr.setUsagePurpose(purpose);
        pr.setHolderId(applicantId);
        pr.setReimburseVoucherNo(req.getInvoiceNo().trim());
        pr.setBudgetNo("N/A");
        pr.setTotalAmount(amount);
        pr.setPriority(1);
        pr.setPrStatus(DB_PENDING);
        pr.setRemark(req.getRemark());
        pr.setWarehouseId(req.getWarehouseId());
        pr.setSupplierName(req.getSupplierName());
        pr.setRegistrarUserNo(registrarUserNo);
        pr.setCreatedBy(registrarUserNo);
        pr.setCreatedAt(now);
        pr.setUpdatedBy(registrarUserNo);
        pr.setUpdatedAt(now);
        purchaseRequestMapper.insert(pr);

        PurchaseRequestItem item = new PurchaseRequestItem();
        item.setPrId(pr.getPrId());
        item.setMaterialId(req.getMaterialId());
        item.setQtyApply(req.getQtyApply());
        item.setRefPrice(refPrice);
        item.setAmount(amount);
        item.setCreatedBy(applicant);
        item.setCreatedAt(now);
        item.setUpdatedBy(applicant);
        item.setUpdatedAt(now);
        purchaseRequestItemMapper.insert(item);

        // 登记阶段不变更库存，等待管理员审核通过后入库
        return toResp(pr, item, stockService.currentQtyOnHand(req.getMaterialId(), req.getWarehouseId()), false);
    }

    @Override
    public List<PurchaseRecordVO> listRecords(String registrarUserId, Integer auditStatus) {
        Integer dbStatus = toDbStatus(auditStatus);
        List<PurchaseRequest> records = purchaseRequestMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequest>()
                        .isNotNull(PurchaseRequest::getReimburseVoucherNo)
                        .eq(registrarUserId != null && !registrarUserId.isBlank(),
                                PurchaseRequest::getRegistrarUserNo, registrarUserId)
                        .eq(dbStatus != null, PurchaseRequest::getPrStatus, dbStatus)
                        .orderByDesc(PurchaseRequest::getPrId));
        return toVOList(records);
    }

    @Override
    public List<PurchaseRecordVO> listPending() {
        List<PurchaseRequest> records = purchaseRequestMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequest>()
                        .isNotNull(PurchaseRequest::getReimburseVoucherNo)
                        .eq(PurchaseRequest::getPrStatus, DB_PENDING)
                        .orderByAsc(PurchaseRequest::getPrId));
        return toVOList(records);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PurchaseRecordVO audit(PurchaseAuditReq req) {
        if (req.getAuditStatus() == null
                || (req.getAuditStatus() != JSON_STORED && req.getAuditStatus() != JSON_REJECTED)) {
            throw new BusinessException(ErrorCode.PURCHASE_AUDIT_STATUS_INVALID,
                    "审核状态只能为 1（通过入库）或 2（驳回）");
        }

        PurchaseRequest pr = purchaseRequestMapper.selectById(req.getId());
        if (pr == null || pr.getReimburseVoucherNo() == null) {
            throw new BusinessException(ErrorCode.PURCHASE_RECORD_NOT_FOUND, "登记记录不存在");
        }
        if (!Integer.valueOf(DB_PENDING).equals(pr.getPrStatus())) {
            throw new BusinessException(ErrorCode.PURCHASE_RECORD_HANDLED, "该记录已处理，不能重复审核");
        }

        PurchaseRequestItem item = firstItem(pr.getPrId());
        if (item == null) {
            throw new BusinessException(ErrorCode.PURCHASE_RECORD_NOT_FOUND, "登记明细不存在");
        }
        if (pr.getWarehouseId() == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "验收仓库不存在");
        }

        Long auditorId = req.getAuditorUserId() == null ? null
                : purchaseRequestMapper.selectUserIdByNo(req.getAuditorUserId().trim());
        Long operatorId = auditorId == null ? 0L : auditorId;

        if (req.getAuditStatus() == JSON_STORED) {
            // 通过：同一事务内入库，库存变更失败则审核状态一并回滚
            // 个人经费自购（科研发票登记）只进个人保管口径：记录保管人，可用量保持 0
            if (Integer.valueOf(SOURCE_TYPE_LAB).equals(pr.getSourceType())) {
                Long holderId = pr.getHolderId() == null ? pr.getApplicantId() : pr.getHolderId();
                stockService.personalInbound(item.getMaterialId(), pr.getWarehouseId(), holderId,
                        item.getQtyApply(), 1, pr.getPrId(), item.getPriId(), operatorId);
            } else {
                stockService.applyChange(item.getMaterialId(), pr.getWarehouseId(), null, "IN",
                        item.getQtyApply(), 1, pr.getPrId(), item.getPriId(), operatorId);
            }
        }

        PurchaseRequest update = new PurchaseRequest();
        update.setPrId(pr.getPrId());
        update.setPrStatus(req.getAuditStatus() == JSON_STORED ? DB_STORED : DB_REJECTED);
        update.setUpdatedBy(req.getAuditorUserId() == null || req.getAuditorUserId().isBlank()
                ? "system" : req.getAuditorUserId().trim());
        update.setUpdatedAt(LocalDateTime.now());
        if (req.getAuditRemark() != null && !req.getAuditRemark().isBlank()) {
            update.setAuditRemark(req.getAuditRemark().trim());
        }
        purchaseRequestMapper.updateById(update);

        pr.setPrStatus(update.getPrStatus());
        if (update.getAuditRemark() != null) {
            pr.setAuditRemark(update.getAuditRemark());
        }
        return toVO(pr, item);
    }

    private List<PurchaseRecordVO> toVOList(List<PurchaseRequest> records) {
        if (records.isEmpty()) {
            return List.of();
        }
        List<Long> prIds = records.stream().map(PurchaseRequest::getPrId).toList();
        Map<Long, PurchaseRequestItem> itemMap = purchaseRequestItemMapper.selectList(
                        new LambdaQueryWrapper<PurchaseRequestItem>().in(PurchaseRequestItem::getPrId, prIds))
                .stream()
                .collect(Collectors.toMap(PurchaseRequestItem::getPrId, Function.identity(), (a, b) -> a));
        return records.stream().map(pr -> toVO(pr, itemMap.get(pr.getPrId()))).toList();
    }

    private PurchaseRequestItem firstItem(Long prId) {
        List<PurchaseRequestItem> items = purchaseRequestItemMapper.selectList(
                new LambdaQueryWrapper<PurchaseRequestItem>()
                        .eq(PurchaseRequestItem::getPrId, prId)
                        .orderByAsc(PurchaseRequestItem::getPriId));
        return items.isEmpty() ? null : items.get(0);
    }

    private String nextPrNo() {
        String prefix = "CG" + LocalDate.now().format(DATE_FORMATTER);
        String max = purchaseRequestMapper.selectMaxPrNo(prefix);
        int seq = 1;
        if (max != null && max.length() > prefix.length()) {
            try {
                seq = Integer.parseInt(max.substring(prefix.length())) + 1;
            } catch (NumberFormatException ignored) {
                seq = 1;
            }
        }
        return prefix + String.format("%04d", seq);
    }

    private Integer toDbStatus(Integer auditStatus) {
        if (auditStatus == null) {
            return null;
        }
        return switch (auditStatus) {
            case JSON_PENDING -> DB_PENDING;
            case JSON_STORED -> DB_STORED;
            case JSON_REJECTED -> DB_REJECTED;
            default -> throw new BusinessException(ErrorCode.PURCHASE_AUDIT_STATUS_INVALID, "审核状态非法");
        };
    }

    private Integer toJsonStatus(Integer prStatus) {
        if (prStatus == null) {
            return null;
        }
        return switch (prStatus) {
            case DB_PENDING -> JSON_PENDING;
            case DB_STORED -> JSON_STORED;
            case DB_REJECTED -> JSON_REJECTED;
            default -> null;
        };
    }

    private PurchaseRegisterResp toResp(PurchaseRequest pr, PurchaseRequestItem item,
                                        BigDecimal qtyOnHand, boolean duplicated) {
        PurchaseRegisterResp resp = new PurchaseRegisterResp();
        resp.setId(pr.getPrId());
        resp.setInvoiceNo(pr.getReimburseVoucherNo());
        resp.setMaterialId(item == null ? null : item.getMaterialId());
        resp.setWarehouseId(pr.getWarehouseId());
        resp.setQtyApply(item == null ? null : item.getQtyApply());
        resp.setSupplierName(pr.getSupplierName());
        resp.setRegistrarUserId(pr.getRegistrarUserNo());
        resp.setRegisterTime(pr.getCreatedAt());
        resp.setQtyOnHand(qtyOnHand);
        resp.setAuditStatus(toJsonStatus(pr.getPrStatus()));
        resp.setDuplicated(duplicated);
        return resp;
    }

    private PurchaseRecordVO toVO(PurchaseRequest pr, PurchaseRequestItem item) {
        PurchaseRecordVO vo = new PurchaseRecordVO();
        vo.setId(pr.getPrId());
        vo.setInvoiceNo(pr.getReimburseVoucherNo());
        vo.setMaterialId(item == null ? null : item.getMaterialId());
        vo.setWarehouseId(pr.getWarehouseId());
        vo.setQtyApply(item == null ? null : item.getQtyApply());
        vo.setSupplierName(pr.getSupplierName());
        vo.setRegistrarUserId(pr.getRegistrarUserNo());
        vo.setRegisterTime(pr.getCreatedAt());
        vo.setRemark(pr.getRemark());
        Integer status = toJsonStatus(pr.getPrStatus());
        vo.setAuditStatus(status);
        vo.setAuditStatusText(statusText(status));
        vo.setAuditRemark(pr.getAuditRemark());
        return vo;
    }

    private String statusText(Integer auditStatus) {
        if (auditStatus == null) {
            return "";
        }
        return switch (auditStatus) {
            case JSON_PENDING -> "待验收";
            case JSON_STORED -> "已入库";
            case JSON_REJECTED -> "已驳回";
            default -> "";
        };
    }
}
