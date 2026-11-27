package com.school.material.requisition.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.school.material.common.BusinessException;
import com.school.material.common.ErrorCode;
import com.school.material.mapper.MaterialMapper;
import com.school.material.mapper.WarehouseMapper;
import com.school.material.requisition.dto.ReqOrderAuditReq;
import com.school.material.requisition.dto.ReqOrderSubmitReq;
import com.school.material.requisition.dto.ReqOrderVO;
import com.school.material.requisition.entity.ReqOrder;
import com.school.material.requisition.mapper.ReqOrderMapper;
import com.school.material.requisition.service.ReqOrderService;
import com.school.material.service.StockService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class ReqOrderServiceImpl implements ReqOrderService {

    /** 接口层审核状态：0待审核 1已通过 2已驳回 */
    private static final int JSON_PENDING = 0;
    private static final int JSON_APPROVED = 1;
    private static final int JSON_REJECTED = 2;

    /** req_order.req_status：1待审核 2已通过 3已驳回 */
    private static final int DB_PENDING = 1;
    private static final int DB_APPROVED = 2;
    private static final int DB_REJECTED = 3;

    private static final String DIRECTION_IN = "IN";
    private static final String DIRECTION_OUT = "OUT";
    private static final String DEFAULT_BATCH = "DEFAULT";

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    private final ReqOrderMapper reqOrderMapper;
    private final StockService stockService;
    private final MaterialMapper materialMapper;
    private final WarehouseMapper warehouseMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReqOrderVO submit(ReqOrderSubmitReq req) {
        String direction = normalizeDirection(req.getDirection());
        BigDecimal qtyApply = req.getQtyApply();
        if (qtyApply == null || qtyApply.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BusinessException(ErrorCode.STOCK_QUANTITY_INVALID, "申请数量必须大于 0");
        }
        if (materialMapper.selectById(req.getMaterialId()) == null) {
            throw new BusinessException(ErrorCode.MATERIAL_NOT_FOUND, "物资不存在");
        }
        if (warehouseMapper.selectById(req.getWarehouseId()) == null) {
            throw new BusinessException(ErrorCode.WAREHOUSE_NOT_FOUND, "仓库不存在");
        }

        LocalDateTime now = LocalDateTime.now();
        Long applicantId = reqOrderMapper.selectUserIdByNo(req.getApplicantUserId().trim());

        ReqOrder order = new ReqOrder();
        order.setReqNo(nextReqNo());
        order.setApplicantId(applicantId == null ? 0L : applicantId);
        order.setApplicantUserNo(req.getApplicantUserId().trim());
        order.setDeptId("000000");
        order.setMaterialId(req.getMaterialId());
        order.setQtyApply(qtyApply);
        order.setAmount(BigDecimal.ZERO);
        order.setReqStatus(DB_PENDING);
        order.setWarehouseId(req.getWarehouseId());
        order.setBatchNo(req.getBatchNo() == null || req.getBatchNo().isBlank()
                ? DEFAULT_BATCH : req.getBatchNo().trim());
        order.setDirection(direction);
        order.setCreatedBy(applicantId == null ? 0L : applicantId);
        order.setCreatedAt(now);
        order.setUpdatedBy(applicantId == null ? 0L : applicantId);
        order.setUpdatedAt(now);
        reqOrderMapper.insert(order);

        // 提交阶段不变更库存，等待审核通过
        return toVO(order, trimToNull(req.getSourceNo()));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReqOrderVO audit(ReqOrderAuditReq req) {
        if (req.getAuditStatus() == null
                || (req.getAuditStatus() != JSON_APPROVED && req.getAuditStatus() != JSON_REJECTED)) {
            throw new BusinessException(ErrorCode.STOCK_APPLY_AUDIT_STATUS_INVALID,
                    "审核状态只能为 1（通过）或 2（驳回）");
        }

        ReqOrder order = reqOrderMapper.selectById(req.getId());
        if (order == null) {
            throw new BusinessException(ErrorCode.STOCK_APPLY_NOT_FOUND, "申请单不存在");
        }
        if (!Integer.valueOf(DB_PENDING).equals(order.getReqStatus())) {
            throw new BusinessException(ErrorCode.STOCK_APPLY_HANDLED, "该申请单已处理，不能重复审核");
        }

        Long auditorId = req.getAuditorUserId() == null ? null
                : reqOrderMapper.selectUserIdByNo(req.getAuditorUserId().trim());
        Long operatorId = auditorId == null ? 0L : auditorId;

        if (req.getAuditStatus() == JSON_APPROVED) {
            // 同一事务内完成出/入库，失败则审核状态一并回滚
            String direction = normalizeDirection(order.getDirection());
            int bizType = DIRECTION_IN.equals(direction) ? 1 : 2;
            stockService.applyChange(order.getMaterialId(), order.getWarehouseId(), order.getBatchNo(),
                    direction, order.getQtyApply(), bizType, order.getReqId(), 0L, operatorId);
        }

        ReqOrder update = new ReqOrder();
        update.setReqId(order.getReqId());
        update.setReqStatus(req.getAuditStatus() == JSON_APPROVED ? DB_APPROVED : DB_REJECTED);
        update.setUpdatedBy(operatorId);
        update.setUpdatedAt(LocalDateTime.now());
        if (req.getAuditStatus() == JSON_APPROVED) {
            update.setQtyApprove(order.getQtyApply());
        }
        String remark = trimToNull(req.getAuditRemark());
        if (remark != null) {
            update.setAuditRemark(remark);
        }
        reqOrderMapper.updateById(update);

        order.setReqStatus(update.getReqStatus());
        if (update.getQtyApprove() != null) {
            order.setQtyApprove(update.getQtyApprove());
        }
        if (remark != null) {
            order.setAuditRemark(remark);
        }
        return toVO(order, null);
    }

    @Override
    public List<ReqOrderVO> list(Integer auditStatus) {
        Integer dbStatus = toDbStatus(auditStatus);
        // req_order 与 ③领用子系统共用表：只展示 ① 自己的库存变动申请（有方向），
        // ③ 的领用单由其领用审批/出库流程处理，避免重复审批和方向为空报错
        return reqOrderMapper.selectList(new LambdaQueryWrapper<ReqOrder>()
                        .isNotNull(ReqOrder::getDirection)
                        .eq(dbStatus != null, ReqOrder::getReqStatus, dbStatus)
                        .orderByDesc(ReqOrder::getReqId))
                .stream().map(order -> toVO(order, null)).toList();
    }

    private String nextReqNo() {
        String prefix = "LY" + LocalDate.now().format(DATE_FORMATTER);
        String max = reqOrderMapper.selectMaxReqNo(prefix);
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

    private String normalizeDirection(String direction) {
        String dir = direction == null ? "" : direction.trim().toUpperCase(Locale.ROOT);
        if (!DIRECTION_IN.equals(dir) && !DIRECTION_OUT.equals(dir)) {
            throw new BusinessException(ErrorCode.STOCK_DIRECTION_INVALID, "库存变更方向只能是 IN 或 OUT");
        }
        return dir;
    }

    private Integer toDbStatus(Integer auditStatus) {
        if (auditStatus == null) {
            return null;
        }
        return switch (auditStatus) {
            case JSON_PENDING -> DB_PENDING;
            case JSON_APPROVED -> DB_APPROVED;
            case JSON_REJECTED -> DB_REJECTED;
            default -> throw new BusinessException(ErrorCode.STOCK_APPLY_AUDIT_STATUS_INVALID, "审核状态非法");
        };
    }

    private ReqOrderVO toVO(ReqOrder order, String sourceNo) {
        ReqOrderVO vo = new ReqOrderVO();
        vo.setId(order.getReqId());
        vo.setApplyNo(order.getReqNo());
        vo.setMaterialId(order.getMaterialId());
        vo.setWarehouseId(order.getWarehouseId());
        vo.setDirection(order.getDirection());
        vo.setQtyApply(order.getQtyApply());
        vo.setSourceNo(sourceNo);
        vo.setApplicantUserId(order.getApplicantUserNo());
        Integer status = toJsonStatus(order.getReqStatus());
        vo.setAuditStatus(status);
        vo.setAuditStatusText(statusText(status));
        vo.setAuditRemark(order.getAuditRemark());
        vo.setApplyTime(order.getCreatedAt());
        return vo;
    }

    private Integer toJsonStatus(Integer reqStatus) {
        if (reqStatus == null) {
            return null;
        }
        return switch (reqStatus) {
            case DB_PENDING -> JSON_PENDING;
            case DB_APPROVED -> JSON_APPROVED;
            case DB_REJECTED -> JSON_REJECTED;
            default -> null;
        };
    }

    private String statusText(Integer auditStatus) {
        if (auditStatus == null) {
            return "";
        }
        return switch (auditStatus) {
            case JSON_PENDING -> "待审核";
            case JSON_APPROVED -> "已通过";
            case JSON_REJECTED -> "已驳回";
            default -> "";
        };
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
