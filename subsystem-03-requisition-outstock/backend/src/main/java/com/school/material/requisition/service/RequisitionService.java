package com.school.material.requisition.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.requisition.auth.AuthContext;
import com.school.material.requisition.auth.LoginUser;
import com.school.material.requisition.common.BizException;
import com.school.material.requisition.common.NoUtil;
import com.school.material.requisition.common.Role;
import com.school.material.requisition.dto.ReqDTOs;
import com.school.material.requisition.entity.*;
import com.school.material.requisition.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

/**
 * 子系统③ 领用申请审批出库主流程。
 *
 * <p>状态机（req_status）：0 草稿 → 1 待审批 → 2 审批中 → 3 已批准（可出库）→ 5 已出库 → 6 已完成；
 * 4 已驳回、7 已关闭。仅 3 及以后状态允许出库。</p>
 *
 * <p>关键控制：</p>
 * <ol>
 *   <li>提交时校验可用库存与部门月度额度（material_dept_quota，按当前年月），不满足直接退回（不进入审批）；</li>
 *   <li>审批只允许本部门负责人（或系统管理员兜底）操作，可部分批准；</li>
 *   <li>出库只允许库管员操作，按「先到期先出」选批，库存变动一律调用子系统②库存服务；</li>
 *   <li>出库即累加部门 used_amount，同时扣减库存并写 2 号领用流水。</li>
 * </ol>
 */
@Service
@RequiredArgsConstructor
public class RequisitionService {

    /** 单据状态常量 */
    public static final int DRAFT = 0, WAIT_APPROVE = 1, APPROVING = 2, APPROVED = 3,
            REJECTED = 4, OUTBOUND = 5, FINISHED = 6, CLOSED = 7;

    private static final int BIZ_REQUISITION = 1;

    private final ReqOrderMapper reqOrderMapper;
    private final OutOrderMapper outOrderMapper;
    private final ApproveRecordMapper approveRecordMapper;
    private final MaterialMapper materialMapper;
    private final DeptMapper deptMapper;
    private final DeptQuotaMapper deptQuotaMapper;
    private final MetaService metaService;
    private final InventoryClient inventoryClient;

    // ------------------------------------------------------------------ 查询

    /** 按角色数据范围查询：申请人=本人、部门负责人=本部门、库管员/管理员=全部 */
    public List<ReqOrder> list(Integer status, String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        List<ReqOrder> rows = reqOrderMapper.selectOrders(s.applicantId, s.deptId, status, null, keyword);
        return rows;
    }

    public ReqOrder detail(Long reqId) {
        ReqOrder o = reqOrderMapper.selectDetail(reqId);
        if (o == null) throw new BizException(3004, "领用单不存在");
        assertVisible(AuthContext.require(), o);
        return o;
    }

    public List<ApproveRecord> logs(Long reqId) {
        detail(reqId);
        return approveRecordMapper.selectByBiz(BIZ_REQUISITION, reqId);
    }

    /** 待审批列表（库管员/管理员）：部门负责人只负责申请，不参与审批 */
    public List<ReqOrder> pendingApprove() {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER) && !me.isAdmin())
            throw new BizException(3003, "只有库管员可以查看待审批列表");
        return reqOrderMapper.selectOrders(null, null, WAIT_APPROVE, null, null);
    }

    /** 待出库列表（库管员）：状态 3 已批准但尚未生成出库单 */
    public List<ReqOrder> pendingOutbound(String keyword) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER) && !me.isAdmin())
            throw new BizException(3003, "只有仓库管理员可以办理出库");
        return reqOrderMapper.selectOrders(null, null, APPROVED, null, keyword);
    }

    /** 出库单列表：申请人看本人、部门负责人看本部门、库管员/管理员看全部 */
    public List<OutOrder> outbounds(String keyword) {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        return outOrderMapper.selectOutOrders(s.applicantId, s.deptId, keyword);
    }

    /** 工作台统计：按当前角色返回待办与汇总 */
    public Map<String, Object> dashboard() {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        List<ReqOrder> all = reqOrderMapper.selectOrders(s.applicantId, s.deptId, null, null, null);
        Map<String, Object> data = new LinkedHashMap<>();
        Map<String, Long> byStatus = new LinkedHashMap<>();
        for (ReqOrder o : all) {
            byStatus.merge(String.valueOf(o.getReqStatus()), 1L, Long::sum);
        }
        data.put("total", all.size());
        data.put("byStatus", byStatus);
        data.put("waitApprove", all.stream().filter(o -> o.getReqStatus() == WAIT_APPROVE).count());
        data.put("waitOutbound", all.stream().filter(o -> o.getReqStatus() == APPROVED).count());
        data.put("outbound", all.stream().filter(o -> o.getReqStatus() != null && o.getReqStatus() >= OUTBOUND).count());
        data.put("amount", all.stream().filter(o -> o.getReqStatus() != null && o.getReqStatus() >= APPROVED)
                .map(ReqOrder::getAmount).filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        data.put("role", me.getRoleCode());
        data.put("roleName", me.getRoleName());
        data.put("deptName", me.getDeptName());
        if (me.getDeptId() != null) {
            DeptQuota q = currentQuota(me.getDeptId());
            if (q != null) {
                data.put("quotaPeriod", q.getQuotaPeriod());
                data.put("monthQuota", q.getMonthQuota());
                data.put("usedAmount", q.getUsedAmount());
                data.put("quotaLeft", q.left());
            }
        }
        return data;
    }

    /** 部门/物资领用统计（管理员、部门负责人、库管员可看） */
    public Map<String, Object> stats() {
        LoginUser me = AuthContext.require();
        Scope s = scopeOf(me);
        List<ReqOrder> all = reqOrderMapper.selectOrders(s.applicantId, s.deptId, null, null, null);
        Map<String, BigDecimal> byMaterial = new LinkedHashMap<>();
        Map<String, BigDecimal> byDept = new LinkedHashMap<>();
        for (ReqOrder o : all) {
            if (o.getReqStatus() == null || o.getReqStatus() < APPROVED || o.getReqStatus() == REJECTED) continue;
            BigDecimal amt = o.getAmount() == null ? BigDecimal.ZERO : o.getAmount();
            byMaterial.merge(nvl(o.getMaterialName()), amt, BigDecimal::add);
            byDept.merge(nvl(o.getDeptName()), amt, BigDecimal::add);
        }
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("byMaterial", byMaterial);
        data.put("byDept", byDept);
        return data;
    }

    // ------------------------------------------------------------------ 申请

    /** 新建领用申请：提交前一次性完成库存与额度校验 */
    @Transactional
    public ReqOrder create(ReqDTOs.Create dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.CAN_APPLY) && !me.isAdmin())
            throw new BizException(3003, "当前角色不能提交领用申请");

        if (dto.materialId() == null) throw new BizException(3050, "请选择领用物资");
        BigDecimal qty = nvl(dto.qtyApply());
        if (qty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(3051, "申请数量必须大于 0");

        Material m = materialMapper.selectById(dto.materialId());
        if (m == null || (m.getDelFlag() != null && m.getDelFlag() == 1))
            throw new BizException(3052, "物资不存在或已停用");
        if (m.getMaterialType() != null && m.getMaterialType() == 2)
            throw new BizException(3053, "「" + m.getMaterialName() + "」是低值耐用品，请走借用流程");

        // 1) 可用库存校验
        BigDecimal available = metaService.available(m.getMaterialId());
        if (available.compareTo(qty) < 0)
            throw new BizException(3001, "可用库存不足：当前可领 " + strip(available) + " " + m.getUnit() + "，申请 " + strip(qty));

        // 2) 部门月度额度校验（material_dept_quota，报告表2-4）
        String deptId = me.getDeptId();
        if (deptId == null) throw new BizException(3054, "当前账号未分配部门，无法申请领用");
        DeptQuota quota = requireQuota(deptId);
        BigDecimal amount = qty.multiply(nvl(m.getRefPrice())).setScale(2, RoundingMode.HALF_UP);
        if (quota.left().compareTo(amount) < 0)
            throw new BizException(3002, "本部门剩余额度不足：" + quota.getQuotaPeriod() + " 剩余 "
                    + strip(quota.left()) + " 元，本单需要 " + strip(amount) + " 元，请与部门负责人确认");

        ReqOrder o = new ReqOrder();
        o.setReqNo(NoUtil.next("LY", reqOrderMapper.countByNoPrefix("LY" + NoUtil.today())));
        o.setApplicantId(me.getUserId());
        o.setDeptId(deptId);
        o.setMaterialId(m.getMaterialId());
        o.setQtyApply(qty);
        o.setAmount(amount);
        boolean submit = dto.submit() == null || dto.submit();
        o.setReqStatus(submit ? WAIT_APPROVE : DRAFT);
        o.setCreatedBy(me.getUserId());
        o.setUpdatedBy(me.getUserId());
        reqOrderMapper.insert(o);
        return reqOrderMapper.selectDetail(o.getReqId());
    }

    /** 草稿提交审批 */
    @Transactional
    public void submit(Long reqId) {
        LoginUser me = AuthContext.require();
        ReqOrder o = mustGet(reqId);
        if (!Objects.equals(o.getApplicantId(), me.getUserId())) throw new BizException(3008, "只能提交本人的领用申请");
        if (o.getReqStatus() != DRAFT) throw new BizException(3009, "只有草稿状态可以提交");
        BigDecimal available = metaService.available(o.getMaterialId());
        if (available.compareTo(o.getQtyApply()) < 0)
            throw new BizException(3001, "可用库存不足：当前可领 " + strip(available));
        updateStatus(reqId, WAIT_APPROVE, me.getUserId());
    }

    /** 撤回：仅限待审批、且为本人 */
    @Transactional
    public void withdraw(Long reqId) {
        LoginUser me = AuthContext.require();
        ReqOrder o = mustGet(reqId);
        if (!Objects.equals(o.getApplicantId(), me.getUserId())) throw new BizException(3008, "只能撤回本人的领用申请");
        if (o.getReqStatus() != WAIT_APPROVE) throw new BizException(3009, "只有待审批状态可以撤回");
        updateStatus(reqId, CLOSED, me.getUserId());
    }

    /** 申请人签收：已出库 → 已完成 */
    @Transactional
    public void receive(Long reqId) {
        LoginUser me = AuthContext.require();
        ReqOrder o = mustGet(reqId);
        if (!Objects.equals(o.getApplicantId(), me.getUserId())) throw new BizException(3008, "只能签收本人的领用单");
        if (o.getReqStatus() != OUTBOUND) throw new BizException(3009, "只有已出库状态可以签收");
        updateStatus(reqId, FINISHED, me.getUserId());
    }

    // ------------------------------------------------------------------ 审批

    @Transactional
    public void approve(Long reqId, ReqDTOs.Approve dto) {
        LoginUser me = AuthContext.require();
        ReqOrder o = mustGet(reqId);
        if (o.getReqStatus() != WAIT_APPROVE && o.getReqStatus() != APPROVING)
            throw new BizException(3010, "当前状态不允许审批");
        assertCanApprove(me, o);

        Integer result = dto.result();
        if (result == null || (result != 0 && result != 1)) throw new BizException(3055, "审批结论必须为通过或驳回");

        if (result == 0) {
            if (dto.opinion() == null || dto.opinion().isBlank())
                throw new BizException(3056, "驳回必须填写审批意见");
            updateStatus(reqId, REJECTED, me.getUserId());
            writeApproveRecord(reqId, me.getUserId(), 0, dto.opinion());
            return;
        }

        // 通过：核准数量可小于申请数量
        Material m = materialMapper.selectById(o.getMaterialId());
        BigDecimal qtyApprove = dto.qtyApprove() == null ? o.getQtyApply() : dto.qtyApprove();
        if (qtyApprove.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(3057, "核准数量必须大于 0");
        if (qtyApprove.compareTo(o.getQtyApply()) > 0) throw new BizException(3057, "核准数量不得大于申请数量");

        BigDecimal available = metaService.available(o.getMaterialId());
        if (available.compareTo(qtyApprove) < 0)
            throw new BizException(3001, "可用库存不足，无法批准：当前可领 " + strip(available));

        BigDecimal amount = qtyApprove.multiply(nvl(m == null ? null : m.getRefPrice())).setScale(2, RoundingMode.HALF_UP);
        DeptQuota quota = currentQuota(o.getDeptId());
        if (quota == null)
            throw new BizException(3002, "本部门 " + currentPeriod() + " 未核定月度领用额度，请先由平台管理端核定");
        if (quota.left().compareTo(amount) < 0)
            throw new BizException(3002, "本部门剩余额度不足：剩余 " + strip(quota.left())
                    + " 元，本单需要 " + strip(amount) + " 元");

        // 管控物资需加签，必须写明用途/加签说明
        if (m != null && m.getIsControlled() != null && m.getIsControlled() == 1
                && (dto.opinion() == null || dto.opinion().isBlank()))
            throw new BizException(3058, "管控物资需加签，请填写审批意见说明用途");

        ReqOrder upd = new ReqOrder();
        upd.setReqId(reqId);
        upd.setQtyApprove(qtyApprove);
        upd.setAmount(amount);
        upd.setReqStatus(APPROVED);
        upd.setUpdatedBy(me.getUserId());
        reqOrderMapper.updateById(upd);
        writeApproveRecord(reqId, me.getUserId(), 1, dto.opinion());
    }

    // ------------------------------------------------------------------ 出库

    /**
     * 出库确认：库管员按先到期先出选定批次，实发数量可小于核准数量。
     * 事务顺序：先落出库单 → 调②扣库存 → 累加部门额度 → 回写领用单状态；
     * 任一步失败整体回滚，②侧幂等键保证不会重复扣减。
     */
    @Transactional
    public OutOrder outbound(ReqDTOs.Outbound dto) {
        LoginUser me = AuthContext.require();
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(3003, "只有仓库管理员可以办理出库");

        ReqOrder o = mustGet(dto.reqId());
        if (o.getReqStatus() != APPROVED)
            throw new BizException(3012, "领用单状态为「" + statusName(o.getReqStatus()) + "」，只有已批准的单据可以出库");
        if (outOrderMapper.countByReq(o.getReqId()) > 0)
            throw new BizException(3013, "该领用单已出库，请勿重复操作");
        if (dto.warehouseId() == null) throw new BizException(3059, "请选择出库仓库");
        if (dto.batchNo() == null || dto.batchNo().isBlank()) throw new BizException(3059, "请选择出库批次");

        BigDecimal approved = nvl(o.getQtyApprove()).compareTo(BigDecimal.ZERO) > 0 ? o.getQtyApprove() : o.getQtyApply();
        BigDecimal outQty = dto.outQty() == null ? approved : dto.outQty();
        if (outQty.compareTo(BigDecimal.ZERO) <= 0) throw new BizException(3060, "实发数量必须大于 0");
        if (outQty.compareTo(approved) > 0) throw new BizException(3060, "实发数量不得大于核准数量 " + strip(approved));

        OutOrder out = new OutOrder();
        out.setOutNo(NoUtil.next("CK", outOrderMapper.countByNoPrefix("CK" + NoUtil.today())));
        out.setSourceType(1);
        out.setSourceId(o.getReqId());
        out.setWarehouseId(dto.warehouseId());
        out.setKeeperId(me.getUserId());
        out.setMaterialId(o.getMaterialId());
        out.setBatchNo(dto.batchNo());
        out.setOutQty(outQty);
        out.setOutTime(LocalDateTime.now());
        out.setCreatedBy(me.getUserId());
        out.setUpdatedBy(me.getUserId());
        outOrderMapper.insert(out);

        // 库存变动：唯一写入口在子系统②（写 2 号领用流水，负数）
        inventoryClient.issueDirect(o.getMaterialId(), dto.warehouseId(), dto.batchNo(), outQty,
                out.getOutId(), me.getUserId());

        // 部门月度已用额度累加（material_dept_quota，按出库时所在年月；超出额度由 SQL 前置条件拦截）
        Material m = materialMapper.selectById(o.getMaterialId());
        BigDecimal amount = outQty.multiply(nvl(m == null ? null : m.getRefPrice())).setScale(2, RoundingMode.HALF_UP);
        if (deptQuotaMapper.addUsedAmount(o.getDeptId(), currentPeriod(), amount) != 1)
            throw new BizException(3002, "本部门剩余额度不足，出库失败");

        ReqOrder upd = new ReqOrder();
        upd.setReqId(o.getReqId());
        upd.setReqStatus(OUTBOUND);
        upd.setUpdatedBy(me.getUserId());
        reqOrderMapper.updateById(upd);

        return outOrderMapper.selectById(out.getOutId());
    }

    // ------------------------------------------------------------------ 内部

    private record Scope(Long applicantId, String deptId) {
    }

    private Scope scopeOf(LoginUser me) {
        String r = me.getRoleCode() == null ? "" : me.getRoleCode();
        if (Role.PURCHASER.equals(r) || Role.CHECKER.equals(r))
            throw new BizException(3003, "当前角色（" + AuthService.roleName(r) + "）不参与领用业务");
        if (Role.APPLICANT.equals(r) || Role.STUDENT.equals(r) || Role.DEPT_HEAD.equals(r))
            return new Scope(me.getUserId(), null); // 部门负责人只申请与查看本人单据
        return new Scope(null, null); // STORE_KEEPER / ADMIN：全部
    }

    private void assertVisible(LoginUser me, ReqOrder o) {
        String r = me.getRoleCode() == null ? "" : me.getRoleCode();
        if (me.isAdmin() || Role.STORE_KEEPER.equals(r)) return;
        if (Objects.equals(me.getUserId(), o.getApplicantId())) return;
        throw new BizException(3014, "无权查看该领用单");
    }

    private void assertCanApprove(LoginUser me, ReqOrder o) {
        if (me.isAdmin()) return; // 系统管理员兜底，审批记录照常留痕
        // 禁止自审：申请人不能审批自己的领用申请
        if (Objects.equals(me.getUserId(), o.getApplicantId()))
            throw new BizException(3019, "不能审批本人提交的领用申请，请由库管员审批");
        if (!me.hasRole(Role.STORE_KEEPER))
            throw new BizException(3003, "只有库管员可以审批领用申请");
    }

    private void writeApproveRecord(Long reqId, Long approverId, int result, String opinion) {
        ApproveRecord r = new ApproveRecord();
        r.setBizType(BIZ_REQUISITION);
        r.setBizId(reqId);
        r.setApproverId(approverId);
        r.setApproveResult(result);
        r.setApproveTime(LocalDateTime.now());
        r.setOpinion(opinion);
        r.setCreatedBy(approverId);
        r.setUpdatedBy(approverId);
        approveRecordMapper.insert(r);
    }

    private ReqOrder mustGet(Long reqId) {
        ReqOrder o = reqOrderMapper.selectById(reqId);
        if (o == null) throw new BizException(3004, "领用单不存在");
        return o;
    }

    private void updateStatus(Long reqId, int status, Long operatorId) {
        ReqOrder upd = new ReqOrder();
        upd.setReqId(reqId);
        upd.setReqStatus(status);
        upd.setUpdatedBy(operatorId);
        reqOrderMapper.updateById(upd);
    }

    public static String statusName(Integer s) {
        if (s == null) return "未知";
        return switch (s) {
            case DRAFT -> "草稿";
            case WAIT_APPROVE -> "待审批";
            case APPROVING -> "审批中";
            case APPROVED -> "已批准";
            case REJECTED -> "已驳回";
            case OUTBOUND -> "已出库";
            case FINISHED -> "已完成";
            case CLOSED -> "已关闭";
            default -> "未知";
        };
    }

    private static BigDecimal nvl(BigDecimal v) {
        return v == null ? BigDecimal.ZERO : v;
    }

    private static String strip(BigDecimal v) {
        return nvl(v).stripTrailingZeros().toPlainString();
    }

    private static String nvl(String v) {
        return v == null ? "—" : v;
    }

    // ------------------------------------------------------------------ 部门月度额度

    /** 当前额度年月，形如 202611 */
    public static String currentPeriod() {
        return java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyyMM"));
    }

    private DeptQuota currentQuota(String deptId) {
        if (deptId == null) return null;
        return deptQuotaMapper.selectOne(deptId, currentPeriod());
    }

    /** 取额度；未核定时直接拒绝（报告：额度由后勤处按期核定） */
    private DeptQuota requireQuota(String deptId) {
        DeptQuota q = currentQuota(deptId);
        if (q == null)
            throw new BizException(3002, "本部门 " + currentPeriod()
                    + " 未核定月度领用额度，请联系平台管理端核定额度后再申请");
        return q;
    }
}
