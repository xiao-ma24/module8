package com.school.material.borrow.service;

import com.school.material.borrow.auth.AuthContext;
import com.school.material.borrow.auth.LoginUser;
import com.school.material.borrow.common.BizException;
import com.school.material.borrow.common.NoUtil;
import com.school.material.borrow.common.Role;
import com.school.material.borrow.dto.BorrowDTOs;
import com.school.material.borrow.entity.*;
import com.school.material.borrow.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 子系统④ 借用归还报损主流程。
 *
 * <p>状态机（borrow_status）：0 待审批 → 1 待借出 → 2 在借 → 3 已归还 / 4 已报损 → 5 已核销。
 * 逾期不落库，由 due_date 与当前日期实时计算。</p>
 *
 * <p><b>借用与领用的本质区别（报告 §1.4.4）</b>：借出只减少「可用量」，不减少「在库总量」，
 * 因为物资仍在学校的账上、只是被个人占用；完好归还恢复可用量；损坏或缺失经报损审批后
 * 才以「报废核销」扣减在库总量。库存变动一律调用子系统②库存服务，本子系统不直连改表。</p>
 */
@Service
@RequiredArgsConstructor
public class BorrowService {

    public static final int PENDING = 0, WAIT_LEND = 1, BORROWED = 2, RETURNED = 3, LOSS_DAMAGED = 4, WRITTEN_OFF = 5;

    /** 审批记录 biz_type：2 借用、3 报损 */
    private static final int BIZ_BORROW = 2;

    private final BorrowReturnMapper borrowMapper;
    private final BorrowReturnItemMapper itemMapper;
    private final OutOrderMapper outOrderMapper;
    private final ApproveRecordMapper approveRecordMapper;
    private final LossOrderMapper lossOrderMapper;
    private final MaterialMapper materialMapper;
    private final MetaService metaService;
    private final InventoryClient inventoryClient;

    // ------------------------------------------------------------------ 查询

    public List<BorrowReturn> list(Integer status, Boolean overdueOnly, String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        return borrowMapper.selectBorrows(s.borrowerId, s.deptId, status, null, overdueOnly, keyword);
    }

    public BorrowReturn detail(Long borrowId) {
        BorrowReturn b = borrowMapper.selectDetail(borrowId);
        if (b == null) throw new BizException(4004, "借用单不存在");
        assertVisible(AuthContext.require(), b);
        return b;
    }

    /** 分次归还明细 */
    public List<BorrowReturnItem> returnItems(Long borrowId) {
        detail(borrowId);
        return itemMapper.selectByBorrow(borrowId);
    }

    public List<ApproveRecord> logs(Long borrowId) {
        detail(borrowId);
        return approveRecordMapper.selectByBiz(BIZ_BORROW, borrowId);
    }

    public List<OutOrder> outbounds(String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        return outOrderMapper.selectBorrowOuts(s.borrowerId, keyword);
    }

    /** 待审批（库管员 / 管理员兜底）：部门负责人只负责申请，不参与审批 */
    public List<BorrowReturn> pendingApprove() {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER) && !me.isAdmin())
            throw new BizException(4003, "只有库管员可以查看待审批列表");
        return borrowMapper.selectBorrows(null, null, PENDING, null, false, null);
    }

    /** 待借出（库管员）：已批准但尚未办理借出登记 */
    public List<BorrowReturn> pendingLend(String keyword) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER) && !me.isAdmin())
            throw new BizException(4003, "只有仓库管理员可以办理借出");
        return borrowMapper.selectBorrows(null, null, WAIT_LEND, null, false, keyword);
    }

    /** 在借清单（库管员/管理员） */
    public List<BorrowReturn> borrowedList(String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        return borrowMapper.selectBorrows(s.borrowerId, s.deptId, BORROWED, null, false, keyword);
    }

    /** 逾期清单：在借且已过预计归还日期，按逾期天数倒序 */
    public List<BorrowReturn> overdue(String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        List<BorrowReturn> rows = borrowMapper.selectBorrows(s.borrowerId, s.deptId, null, null, true, keyword);
        rows.sort((a, b) -> Long.compare(nvl(b.getOverdueDays()), nvl(a.getOverdueDays())));
        return rows;
    }

    public Map<String, Object> dashboard() {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        List<BorrowReturn> all = borrowMapper.selectBorrows(s.borrowerId, s.deptId, null, null, false, null);
        long overdueCnt = all.stream().filter(b -> b.getOverdue() != null && b.getOverdue()).count();
        long pendingLoss = lossOrderMapper.selectLosses(0, s.deptId(), s.borrowerId, 1, null).size();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (BorrowReturn b : all) byStatus.merge(String.valueOf(b.getBorrowStatus()), 1L, Long::sum);

        Map<String, Object> data = new LinkedHashMap<>();
        data.put("total", all.size());
        data.put("byStatus", byStatus);
        data.put("waitApprove", all.stream().filter(b -> b.getBorrowStatus() == PENDING).count());
        data.put("waitLend", all.stream().filter(b -> b.getBorrowStatus() == WAIT_LEND).count());
        data.put("borrowed", all.stream().filter(b -> b.getBorrowStatus() == BORROWED).count());
        data.put("overdue", overdueCnt);
        data.put("pendingLoss", pendingLoss);
        data.put("role", me.getRoleCode());
        data.put("roleName", me.getRoleName());
        data.put("deptName", me.getDeptName());
        return data;
    }

    // ------------------------------------------------------------------ 申请与审批

    /** 提交借用申请：校验物资是否为低值耐用品、可用量是否满足 */
    @Transactional
    public BorrowReturn create(BorrowDTOs.Create dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.CAN_APPLY) && !me.isAdmin())
            throw new BizException(4003, "当前角色不能提交借用申请");

        if (dto.materialId() == null) throw new BizException(4050, "请选择借用物资");
        BigDecimal qty = nvl(dto.borrowQty());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(4051, "借用数量必须大于 0");
        if (dto.dueDate() == null) throw new BizException(4052, "请填写预计归还日期");
        if (dto.dueDate().isBefore(LocalDate.now())) throw new BizException(4052, "预计归还日期不得早于今天");

        Material m = materialMapper.selectById(dto.materialId());
        if (m == null || (m.getDelFlag() != null && m.getDelFlag() == 1))
            throw new BizException(4053, "物资不存在或已停用");
        if (m.getMaterialType() == null || m.getMaterialType() != 2)
            throw new BizException(4054, "「" + m.getMaterialName() + "」是低值易耗品，请走领用流程");

        BigDecimal available = metaService.available(m.getMaterialId());
        if (available.compareTo(qty) < 0)
            throw new BizException(4001, "当前可借数量不足：可借 " + strip(available) + " " + m.getUnit());

        BorrowReturn b = new BorrowReturn();
        b.setBorrowNo(NoUtil.next("JY", borrowMapper.countByNoPrefix("JY" + NoUtil.today())));
        b.setBorrowerId(me.getUserId());
        b.setMaterialId(m.getMaterialId());
        b.setBorrowQty(qty);
        b.setReturnedQty(BigDecimal.ZERO);
        b.setWrittenOffQty(BigDecimal.ZERO);
        b.setDueDate(dto.dueDate());
        b.setBorrowStatus(PENDING);
        b.setCreatedBy(me.getUserId());
        b.setUpdatedBy(me.getUserId());
        borrowMapper.insert(b);
        return borrowMapper.selectDetail(b.getBorrowId());
    }

    @Transactional
    public void approve(Long borrowId, BorrowDTOs.Approve dto) {
        LoginUser me = AuthContext.require();
        BorrowReturn b = mustGet(borrowId);
        if (b.getBorrowStatus() != PENDING) throw new BizException(4010, "当前状态不允许审批");
        assertCanApprove(me, b);

        Integer result = dto.result();
        if (result == null || (result != 0 && result != 1)) throw new BizException(4055, "审批结论必须为通过或驳回");
        if (result == 0) {
            if (dto.opinion() == null || dto.opinion().isBlank())
                throw new BizException(4056, "驳回必须填写审批意见");
            // 报告 borrow_status 取值集合无「已驳回」，驳回即关闭单据，统一置为 5 已核销终态，
            // 驳回结论通过 approve_record（biz_type=2，approve_result=0）留痕。
            updateStatus(borrowId, WRITTEN_OFF, me.getUserId());
            writeApproveRecord(borrowId, me.getUserId(), 0, dto.opinion());
            return;
        }
        // 通过前再次校验可用量，避免审批期间被领用占用
        BigDecimal available = metaService.available(b.getMaterialId());
        if (available.compareTo(b.getBorrowQty()) < 0)
            throw new BizException(4001, "可用库存不足，无法批准：当前可借 " + strip(available));
        updateStatus(borrowId, WAIT_LEND, me.getUserId());
        writeApproveRecord(borrowId, me.getUserId(), 1, dto.opinion());
    }

    // ------------------------------------------------------------------ 借出

    /** 借出登记：库管员选定批次，库存只扣可用量（在库总量不变） */
    @Transactional
    public OutOrder lend(BorrowDTOs.Lend dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(4003, "只有仓库管理员可以办理借出");

        BorrowReturn b = mustGet(dto.borrowId());
        if (b.getBorrowStatus() != WAIT_LEND)
            throw new BizException(4012, "借用单状态为「" + statusName(b.getBorrowStatus()) + "」，只有待借出状态可以办理");
        if (outOrderMapper.countByBorrow(b.getBorrowId()) > 0)
            throw new BizException(4013, "该借用单已办理借出，请勿重复操作");
        if (dto.warehouseId() == null) throw new BizException(4057, "请选择借出仓库");
        if (dto.batchNo() == null || dto.batchNo().isBlank()) throw new BizException(4057, "请选择借出批次");

        OutOrder out = new OutOrder();
        out.setOutNo(NoUtil.next("CK", outOrderMapper.countByNoPrefix("CK" + NoUtil.today())));
        out.setSourceType(2);
        out.setSourceId(b.getBorrowId());
        out.setWarehouseId(dto.warehouseId());
        out.setKeeperId(me.getUserId());
        out.setMaterialId(b.getMaterialId());
        out.setBatchNo(dto.batchNo());
        out.setOutQty(b.getBorrowQty());
        out.setOutTime(LocalDateTime.now());
        out.setCreatedBy(me.getUserId());
        out.setUpdatedBy(me.getUserId());
        outOrderMapper.insert(out);

        // 借出：可用量 -q，在库总量不变（reserved=false 表示未提前预占）
        inventoryClient.borrowOut(b.getMaterialId(), dto.warehouseId(), dto.batchNo(), b.getBorrowQty(),
                out.getOutId(), 0L, me.getUserId());

        BorrowReturn upd = new BorrowReturn();
        upd.setBorrowId(b.getBorrowId());
        upd.setWarehouseId(dto.warehouseId());
        upd.setBatchNo(dto.batchNo());
        upd.setBorrowStatus(BORROWED);
        upd.setUpdatedBy(me.getUserId());
        borrowMapper.updateById(upd);
        return outOrderMapper.selectById(out.getOutId());
    }

    // ------------------------------------------------------------------ 归还验收

    /**
     * 归还验收（支持分次、部分归还）：
     * <ul>
     *   <li>完好（1）：调用②恢复可用量，累计 returned_qty；</li>
     *   <li>损坏（2）/缺失（3）：生成报损单（待审批），审批通过并执行后才扣减在库总量。</li>
     * </ul>
     */
    @Transactional
    public Map<String, Object> doReturn(BorrowDTOs.ReturnReq dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(4003, "只有仓库管理员可以办理归还验收");

        BorrowReturn b = mustGet(dto.borrowId());
        if (b.getBorrowStatus() != BORROWED && b.getBorrowStatus() != LOSS_DAMAGED)
            throw new BizException(4014, "借用单状态为「" + statusName(b.getBorrowStatus()) + "」，无法归还");
        if (b.getWarehouseId() == null || b.getBatchNo() == null)
            throw new BizException(4058, "该借用单缺少借出批次信息，请先完成借出登记");

        BigDecimal qty = nvl(dto.returnQty());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(4059, "归还数量必须大于 0");
        BigDecimal remaining = nvl(b.getBorrowQty()).subtract(nvl(b.getReturnedQty())).subtract(nvl(b.getWrittenOffQty()));
        if (qty.compareTo(remaining) > 0)
            throw new BizException(4059, "归还数量超过未归还数量 " + strip(remaining));

        Integer accept = dto.acceptResult();
        if (accept == null || accept < 1 || accept > 3)
            throw new BizException(4060, "请选择验收结果：1 完好入库、2 损坏核销、3 缺失核销");
        if (accept != 1 && (dto.remark() == null || dto.remark().isBlank()))
            throw new BizException(4060, "损坏或缺失必须填写情况说明");

        BorrowReturnItem item = new BorrowReturnItem();
        item.setBorrowId(b.getBorrowId());
        item.setItemNo(NoUtil.next("GH", itemMapper.countByNoPrefix("GH" + NoUtil.today())));
        item.setReturnQty(qty);
        item.setAcceptResult(accept);
        item.setWarehouseId(b.getWarehouseId());
        item.setBatchNo(b.getBatchNo());
        item.setKeeperId(me.getUserId());
        item.setReturnTime(LocalDateTime.now());
        item.setRemark(dto.remark());
        item.setCreatedBy(me.getUserId());
        item.setUpdatedBy(me.getUserId());
        itemMapper.insert(item);

        Map<String, Object> data = new LinkedHashMap<>();
        if (accept == 1) {
            // 完好归还：在库总量不变，可用量恢复
            inventoryClient.returnGood(b.getMaterialId(), b.getWarehouseId(), b.getBatchNo(), qty,
                    item.getItemId(), 0L, me.getUserId());
            BorrowReturn upd = new BorrowReturn();
            upd.setBorrowId(b.getBorrowId());
            upd.setReturnedQty(nvl(b.getReturnedQty()).add(qty));
            upd.setReturnTime(LocalDateTime.now());
            upd.setConditionCode(b.getConditionCode() != null && b.getConditionCode() > 1 ? b.getConditionCode() : 1);
            upd.setUpdatedBy(me.getUserId());
            borrowMapper.updateById(upd);
            data.put("result", "已完好入库");
        } else {
            // 损坏/缺失：转报损登记，库存待审批通过并执行核销后再扣减
            LossOrder loss = new LossOrder();
            loss.setLossNo(NoUtil.next("BS", lossOrderMapper.countByNoPrefix("BS" + NoUtil.today())));
            loss.setSourceType(1);
            loss.setSourceId(b.getBorrowId());
            loss.setMaterialId(b.getMaterialId());
            loss.setBatchNo(b.getBatchNo());
            loss.setWarehouseId(b.getWarehouseId());
            loss.setLossQty(qty);
            loss.setReason(dto.remark());
            loss.setApproveStatus(0);
            loss.setExecuted(0);
            loss.setCreatedBy(me.getUserId());
            loss.setUpdatedBy(me.getUserId());
            lossOrderMapper.insert(loss);

            BorrowReturn upd = new BorrowReturn();
            upd.setBorrowId(b.getBorrowId());
            upd.setReturnTime(LocalDateTime.now());
            upd.setConditionCode(accept == 2 ? 3 : 4);
            upd.setUpdatedBy(me.getUserId());
            borrowMapper.updateById(upd);
            data.put("lossNo", loss.getLossNo());
            data.put("result", "已转报损登记，待审批核销");
        }

        recomputeStatus(b.getBorrowId(), me.getUserId());
        data.put("borrow", borrowMapper.selectDetail(b.getBorrowId()));
        return data;
    }

    /** 根据累计归还/核销数量与待审批报损，重算借用单状态 */
    @Transactional
    public void recomputeStatus(Long borrowId, Long operatorId) {
        BorrowReturn b = mustGet(borrowId);
        BigDecimal done = nvl(b.getReturnedQty()).add(nvl(b.getWrittenOffQty()));
        int status;
        if (done.compareTo(nvl(b.getBorrowQty())) >= 0) {
            status = nvl(b.getWrittenOffQty()).compareTo(BigDecimal.ZERO) > 0 ? WRITTEN_OFF : RETURNED;
        } else if (lossOrderMapper.countPendingByBorrow(borrowId) > 0) {
            status = LOSS_DAMAGED;
        } else {
            status = BORROWED;
        }
        updateStatus(borrowId, status, operatorId);
    }

    // ------------------------------------------------------------------ 内部

    private record Scope(Long borrowerId, String deptId) {
    }

    private Scope scopeOf(LoginUser me) {
        String r = me.getRoleCode() == null ? "" : me.getRoleCode();
        if (Role.PURCHASER.equals(r) || Role.CHECKER.equals(r))
            throw new BizException(4003, "当前角色（" + AuthService.roleName(r) + "）不参与借用业务");
        if (Role.APPLICANT.equals(r) || Role.STUDENT.equals(r) || Role.DEPT_HEAD.equals(r))
            return new Scope(me.getUserId(), null); // 部门负责人只申请与查看本人单据
        return new Scope(null, null);
    }

    private void assertVisible(LoginUser me, BorrowReturn b) {
        String r = me.getRoleCode() == null ? "" : me.getRoleCode();
        if (me.isAdmin() || Role.STORE_KEEPER.equals(r)) return;
        if (Objects.equals(me.getUserId(), b.getBorrowerId())) return;
        throw new BizException(4015, "无权查看该借用单");
    }

    private void assertCanApprove(LoginUser me, BorrowReturn b) {
        if (me.isAdmin()) return;
        // 禁止自审：借用人不能审批自己的借用申请
        if (Objects.equals(me.getUserId(), b.getBorrowerId()))
            throw new BizException(4019, "不能审批本人提交的借用申请，请由库管员审批");
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(4003, "只有库管员可以审批借用申请");
    }

    private void writeApproveRecord(Long borrowId, Long approverId, int result, String opinion) {
        ApproveRecord r = new ApproveRecord();
        r.setBizType(BIZ_BORROW);
        r.setBizId(borrowId);
        r.setApproverId(approverId);
        r.setApproveResult(result);
        r.setApproveTime(LocalDateTime.now());
        r.setOpinion(opinion);
        r.setCreatedBy(approverId);
        r.setUpdatedBy(approverId);
        approveRecordMapper.insert(r);
    }

    /**
     * 取单据：用带联查的详情查询，保证 deptId / materialName 等展示字段可用
     * （借用人部门来自 sys_user.dept_id，不在 borrow_return 表内，selectById 取不到）。
     */
    private BorrowReturn mustGet(Long borrowId) {
        BorrowReturn b = borrowMapper.selectDetail(borrowId);
        if (b == null) throw new BizException(4004, "借用单不存在");
        return b;
    }

    private void updateStatus(Long borrowId, int status, Long operatorId) {
        BorrowReturn upd = new BorrowReturn();
        upd.setBorrowId(borrowId);
        upd.setBorrowStatus(status);
        upd.setUpdatedBy(operatorId);
        borrowMapper.updateById(upd);
    }

    public static String statusName(Integer s) {
        if (s == null) return "未知";
        return switch (s) {
            case PENDING -> "待审批";
            case WAIT_LEND -> "待借出";
            case BORROWED -> "在借";
            case RETURNED -> "已归还";
            case LOSS_DAMAGED -> "已报损";
            case WRITTEN_OFF -> "已核销";
            default -> "未知";
        };
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static long nvl(Long v) {
        return v == null ? 0L : v;
    }

    private static String strip(BigDecimal v) {
        return nvl(v).setScale(3, RoundingMode.HALF_UP).stripTrailingZeros().toPlainString();
    }
}
