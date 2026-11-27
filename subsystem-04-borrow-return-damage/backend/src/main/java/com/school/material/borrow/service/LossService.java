package com.school.material.borrow.service;

import com.school.material.borrow.auth.AuthContext;
import com.school.material.borrow.auth.LoginUser;
import com.school.material.borrow.common.BizException;
import com.school.material.borrow.common.Role;
import com.school.material.borrow.dto.BorrowDTOs;
import com.school.material.borrow.entity.ApproveRecord;
import com.school.material.borrow.entity.BorrowReturn;
import com.school.material.borrow.entity.LossOrder;
import com.school.material.borrow.entity.Material;
import com.school.material.borrow.mapper.ApproveRecordMapper;
import com.school.material.borrow.mapper.BorrowReturnMapper;
import com.school.material.borrow.mapper.LossOrderMapper;
import com.school.material.borrow.mapper.MaterialMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * 报损流程：登记 → 审批（部门负责人/管理者）→ 核销执行（库管员）。
 *
 * <p>库存口径：</p>
 * <ul>
 *   <li>source_type=1 借用转报损：物资已借出，核销时只扣「在库总量」（可用量早已扣过）；</li>
 *   <li>source_type=2 盘亏报损、3 到期/超限报废：物资仍在库内，核销时同时扣「在库总量」与「可用量」。</li>
 * </ul>
 *
 * <p>幂等：核销以 {@code executed=0} 为前置条件更新，重复提交不会重复扣减库存。
 * 赔偿（dispose_result=1）金额按「报损数量 × 参考单价」计算并返回，作为推送财务系统的依据。</p>
 */
@Service
@RequiredArgsConstructor
public class LossService {

    /** 审批记录 biz_type：3 报损 */
    private static final int BIZ_LOSS = 3;

    private final LossOrderMapper lossOrderMapper;
    private final BorrowReturnMapper borrowMapper;
    private final BorrowService borrowService;
    private final MaterialMapper materialMapper;
    private final ApproveRecordMapper approveRecordMapper;
    private final InventoryClient inventoryClient;

    public List<LossOrder> list(Integer status, Integer sourceType, String keyword) {
        LoginUser me = AuthContext.require();
        String r = me.getRoleCode() == null ? "" : me.getRoleCode();
        String deptId = null;
        Long borrowerId = null;
        if (Role.APPLICANT.equals(r) || Role.STUDENT.equals(r)) borrowerId = me.getUserId();
        else if (Role.DEPT_HEAD.equals(r)) deptId = me.getDeptId();
        else if (Role.PURCHASER.equals(r) || Role.CHECKER.equals(r))
            throw new BizException(4003, "当前角色不参与报损业务");
        return lossOrderMapper.selectLosses(status, deptId, borrowerId, sourceType, keyword);
    }

    public List<ApproveRecord> logs(Long lossId) {
        LossOrder l = mustGet(lossId);
        return approveRecordMapper.selectByBiz(BIZ_LOSS, l.getLossId());
    }

    /** 报损审批：通过需给出处置结论（1 赔偿 / 2 免赔 / 3 报废核销） */
    @Transactional
    public void approve(Long lossId, BorrowDTOs.LossApprove dto) {
        LoginUser me = AuthContext.require();
        LossOrder l = mustGet(lossId);
        if (l.getApproveStatus() == null || l.getApproveStatus() != 0)
            throw new BizException(4020, "该报损单已处理，请勿重复审批");
        if (!me.isAdmin()) {
            if (!me.hasRole(Role.DEPT_HEAD)) throw new BizException(4003, "只有部门负责人可以审批报损");
            String deptId = deptIdOf(l);
            if (deptId != null && !Objects.equals(me.getDeptId(), deptId))
                throw new BizException(4021, "只能审批本部门的报损单");
        }

        Integer result = dto.result();
        if (result == null || (result != 1 && result != 2))
            throw new BizException(4061, "审批结论必须为通过（1）或驳回（2）");
        if (result == 1) {
            Integer dispose = dto.disposeResult();
            if (dispose == null || dispose < 1 || dispose > 3)
                throw new BizException(4062, "通过必须给出处置结论：1 赔偿、2 免赔、3 报废核销");
            if (lossOrderMapper.markApproved(lossId, 1, dispose, me.getUserId()) != 1)
                throw new BizException(4020, "该报损单已处理，请勿重复审批");
            writeRecord(lossId, me.getUserId(), 1, dto.opinion());
        } else {
            if (dto.opinion() == null || dto.opinion().isBlank())
                throw new BizException(4063, "驳回必须填写审批意见");
            lossOrderMapper.markApproved(lossId, 2, null, me.getUserId());
            writeRecord(lossId, me.getUserId(), 0, dto.opinion());
        }
        if (l.getSourceType() != null && l.getSourceType() == 1) syncBorrow(l.getSourceId(), me.getUserId());
    }

    /**
     * 核销执行：库管员按处置结论扣减库存并置 executed=1。
     * 借用报损只扣在库总量；盘亏/报废同时扣可用量。
     */
    @Transactional
    public Map<String, Object> execute(Long lossId, BorrowDTOs.LossExecute dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(4003, "只有仓库管理员可以执行核销");

        LossOrder l = mustGet(lossId);
        if (l.getApproveStatus() != null && l.getApproveStatus() == 2)
            throw new BizException(4022, "该报损单已驳回，不能核销");
        if (l.getExecuted() != null && l.getExecuted() == 1)
            throw new BizException(4023, "该报损单已核销，请勿重复操作");
        if (l.getWarehouseId() == null || l.getBatchNo() == null)
            throw new BizException(4064, "报损单缺少仓库或批次，无法核销");

        Integer dispose = dto != null && dto.disposeResult() != null ? dto.disposeResult() : l.getDisposeResult();
        if (dispose == null || dispose < 1 || dispose > 3)
            throw new BizException(4062, "请先确定处置结论：1 赔偿、2 免赔、3 报废核销");

        boolean fromBorrow = l.getSourceType() != null && l.getSourceType() == 1;

        // 待审批的报损单由库管员直接处理：先补一条通过记录，再进入核销
        if (l.getApproveStatus() == null || l.getApproveStatus() == 0) {
            if (lossOrderMapper.markApproved(lossId, 1, dispose, me.getUserId()) != 1)
                throw new BizException(4020, "该报损单已处理，请勿重复操作");
            writeRecord(lossId, me.getUserId(), 1, "库管员直接报损处理");
            if (fromBorrow) syncBorrow(l.getSourceId(), me.getUserId());
        }

        // 先置执行标记占用幂等位，再做库存扣减；库存失败时事务回滚，标记一并撤销
        if (lossOrderMapper.markExecuted(lossId, me.getUserId(), dispose) != 1)
            throw new BizException(4023, "该报损单已核销，请勿重复操作");

        if (fromBorrow) {
            inventoryClient.writeOff(l.getMaterialId(), l.getWarehouseId(), l.getBatchNo(),
                    l.getLossQty(), l.getLossId(), 0L, me.getUserId());
        } else {
            inventoryClient.scrap(l.getMaterialId(), l.getWarehouseId(), l.getBatchNo(),
                    l.getLossQty(), l.getLossId(), 0L, me.getUserId());
        }

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("lossNo", l.getLossNo());
        data.put("disposeResult", dispose);
        if (fromBorrow) {
            BorrowReturn b = borrowMapper.selectById(l.getSourceId());
            if (b != null) {
                BorrowReturn upd = new BorrowReturn();
                upd.setBorrowId(b.getBorrowId());
                upd.setWrittenOffQty(nz(b.getWrittenOffQty()).add(l.getLossQty()));
                upd.setUpdatedBy(me.getUserId());
                borrowMapper.updateById(upd);
                borrowService.recomputeStatus(b.getBorrowId(), me.getUserId());
            }
        }
        // 赔偿金额：报损数量 × 物资参考单价，作为推送财务系统的依据（课程环境不接真实接口）
        if (dispose == 1) {
            Material m = materialMapper.selectById(l.getMaterialId());
            BigDecimal price = m == null || m.getRefPrice() == null ? BigDecimal.ZERO : m.getRefPrice();
            data.put("compensateAmount", l.getLossQty().multiply(price).setScale(2, RoundingMode.HALF_UP));
        }
        data.put("loss", lossOrderMapper.selectById(lossId));
        return data;
    }

    // ------------------------------------------------------------------ 内部

    private void syncBorrow(Long borrowId, Long operatorId) {
        if (borrowId == null) return;
        borrowService.recomputeStatus(borrowId, operatorId);
    }

    private String deptIdOf(LossOrder l) {
        if (l.getSourceType() != null && l.getSourceType() == 1 && l.getSourceId() != null) {
            BorrowReturn b = borrowMapper.selectDetail(l.getSourceId());
            return b == null ? null : b.getDeptId();
        }
        return l.getDeptId();
    }

    private void writeRecord(Long lossId, Long approverId, int result, String opinion) {
        ApproveRecord r = new ApproveRecord();
        r.setBizType(BIZ_LOSS);
        r.setBizId(lossId);
        r.setApproverId(approverId);
        r.setApproveResult(result);
        r.setApproveTime(LocalDateTime.now());
        r.setOpinion(opinion);
        r.setCreatedBy(approverId);
        r.setUpdatedBy(approverId);
        approveRecordMapper.insert(r);
    }

    private LossOrder mustGet(Long lossId) {
        LossOrder l = lossOrderMapper.selectById(lossId);
        if (l == null) throw new BizException(4005, "报损单不存在");
        return l;
    }

    private static BigDecimal nz(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }
}
