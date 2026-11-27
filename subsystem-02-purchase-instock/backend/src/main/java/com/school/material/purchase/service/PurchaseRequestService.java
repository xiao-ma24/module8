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
import java.util.List;

/** 采购申请：创建→提交(预算冻结)→审批→转订单 */
@Service
@RequiredArgsConstructor
public class PurchaseRequestService {

    private final PurchaseRequestMapper prMapper;
    private final PurchaseRequestItemMapper priMapper;
    private final MaterialMapper materialMapper;
    private final BudgetMapper budgetMapper;
    private final ApproveLogMapper approveLogMapper;
    private final ItemMappers itemMappers;
    private final UserMapper userMapper;
    private final PurchasePlanMapper planMapper;

    /** 补货计划（⑤ 盘点补货）批准后转采购申请：一计划一条申请，幂等 */
    @Transactional
    public PurchaseRequest createFromPlan(Long planId) {
        PurchasePlan pl = planMapper.selectById(planId);
        if (pl == null) throw new BizException(2012, "采购计划不存在");
        PurchaseRequest exist = prMapper.selectOne(Wrappers.<PurchaseRequest>lambdaQuery()
                .eq(PurchaseRequest::getSourceType, 3)
                .eq(PurchaseRequest::getSourceId, planId)
                .last("LIMIT 1"));
        if (exist != null) return exist;
        if (pl.getPlanStatus() == null || pl.getPlanStatus() != 2)
            throw new BizException(2013, "采购计划未批准，不能转采购申请");
        Material m = materialMapper.selectById(pl.getMaterialId());
        if (m == null) throw new BizException(2003, "物资不存在：" + pl.getMaterialId());

        PurchaseRequest pr = new PurchaseRequest();
        pr.setSourceType(3);
        pr.setSourceId(planId);
        pr.setPurchaseMode(pl.getPurchaseMode() == null ? 1 : pl.getPurchaseMode());
        pr.setApplicantId(pl.getCreatedBy());
        pr.setDeptId(toLong(pl.getDeptId()));
        pr.setUsagePurpose("库存预警补货（计划 " + pl.getPlanNo() + "）");
        pr.setExpectDate(pl.getExpectDate());
        pr.setPriority(1);
        pr.setPrStatus(1);
        pr.setRemark("由补货计划 " + pl.getPlanNo() + " 自动转入，请采购员审批后生成采购订单");
        BigDecimal price = m.getRefPrice() == null ? BigDecimal.ZERO : m.getRefPrice();
        BigDecimal total = price.multiply(pl.getPlanQty());
        // 补货来源没有预算科目，自动占用余额充足的科目，保证后续扣减一致
        Budget budget = budgetMapper.selectList(Wrappers.<Budget>lambdaQuery()).stream()
                .filter(b -> b.getBalance() != null
                        && b.getBalance().subtract(b.getFrozen() == null ? BigDecimal.ZERO
                            : BigDecimal.valueOf(b.getFrozen())).compareTo(total) >= 0)
                .findFirst()
                .orElseThrow(() -> new BizException(2014, "预算余额不足，补货计划 " + pl.getPlanNo() + " 无法转采购申请"));
        pr.setBudgetNo(budget.getBudgetNo());
        pr.setTotalAmount(total);
        pr.setPrNo(NoUtil.next("CG", planMapper.maxSeq("CG" + NoUtil.today())));
        prMapper.insert(pr);
        if (budgetMapper.freeze(budget.getBudgetNo(), total) == 0)
            throw new BizException(2006, "预算余额不足，预算科目：" + budget.getBudgetNo());

        PurchaseRequestItem it = new PurchaseRequestItem();
        it.setPrId(pr.getPrId());
        it.setMaterialId(pl.getMaterialId());
        it.setQtyApply(pl.getPlanQty());
        it.setRefPrice(price);
        it.setAmount(price.multiply(pl.getPlanQty()));
        it.setQtyConfirmed(pl.getPlanQty());
        priMapper.insert(it);
        return pr;
    }

    /** 采购申请只能由采购员、部门负责人或管理员审批 */
    private void requireApproverRole(Long approverId) {
        String denied = "采购申请须由采购员、部门负责人或管理员审批";
        if (approverId == null) throw new BizException(2010, "请选择审批人");
        List<String> roles = userMapper.selectRoleCodes(approverId);
        boolean ok = roles != null && roles.stream()
                .anyMatch(r -> "PURCHASER".equals(r) || "DEPT_HEAD".equals(r) || "ADMIN".equals(r));
        if (!ok) throw new BizException(2010, denied);
    }

    public List<PurchaseRequest> list() {
        return prMapper.selectListWithNames();
    }

    public PurchaseRequest detail(Long id) {
        PurchaseRequest pr = prMapper.selectDetail(id);
        if (pr == null) throw new BizException(2001, "采购申请不存在");
        return pr;
    }

    public List<PurchaseRequestItem> items(Long prId) {
        return itemMappers.selectPrItems(prId);
    }

    @Transactional
    public PurchaseRequest create(ReqDTOs.PrCreate dto) {
        if (dto.getItems() == null || dto.getItems().isEmpty())
            throw new BizException(2002, "申请明细不能为空");
        if (dto.getApplicantId() == null || dto.getApplicantId() <= 0)
            throw new BizException(2015, "请选择申请人");
        String purpose = dto.getUsagePurpose() == null ? "" : dto.getUsagePurpose().trim();
        if (purpose.isEmpty())
            throw new BizException(2016, "请填写用途说明（采购理由）");
        if (dto.getBudgetNo() == null || dto.getBudgetNo().isBlank())
            throw new BizException(2018, "请选择预算科目");
        // 部门缺失时按申请人归属部门兜底，避免出现部门为空的申请
        Long deptId = dto.getDeptId();
        if (deptId == null || deptId == 0) deptId = toLong(userMapper.selectDeptId(dto.getApplicantId()));
        if (deptId == null || deptId == 0)
            throw new BizException(2017, "申请人未归属部门，无法创建采购申请，请先在用户管理中分配部门");
        PurchaseRequest pr = new PurchaseRequest();
        pr.setSourceType(dto.getSourceType() == null ? 1 : dto.getSourceType());
        pr.setPurchaseMode(dto.getPurchaseMode() == null ? 1 : dto.getPurchaseMode());
        pr.setSourceId(dto.getSourceId());
        pr.setApplicantId(dto.getApplicantId());
        pr.setDeptId(deptId);
        pr.setHolderId(dto.getHolderId());
        pr.setUsagePurpose(purpose);
        pr.setBudgetNo(dto.getBudgetNo());
        pr.setExpectDate(dto.getExpectDate());
        pr.setPriority(dto.getPriority() == null ? 1 : dto.getPriority());
        pr.setPrStatus(0);
        pr.setRemark(dto.getRemark());

        if (pr.getPurchaseMode() != 1 && pr.getPurchaseMode() != 2)
            throw new BizException(2010, "采购方式仅支持 1=集中采购 或 2=科研经费自购");
        if (pr.getPurchaseMode() == 2 && pr.getHolderId() == null)
            throw new BizException(2011, "科研经费自购必须指定实际保管教师");

        BigDecimal total = BigDecimal.ZERO;
        for (ReqDTOs.PrCreate.Item i : dto.getItems()) {
            Material m = materialMapper.selectById(i.getMaterialId());
            if (m == null) throw new BizException(2003, "物资不存在：" + i.getMaterialId());
            if (i.getQtyApply() == null || i.getQtyApply().signum() <= 0)
                throw new BizException(2004, "申请数量必须大于0");
            total = total.add(m.getRefPrice().multiply(i.getQtyApply()));
        }
        pr.setTotalAmount(total);
        pr.setPrNo(NoUtil.next("CG", planMapper.maxSeq("CG" + NoUtil.today())));
        prMapper.insert(pr);

        for (ReqDTOs.PrCreate.Item i : dto.getItems()) {
            Material m = materialMapper.selectById(i.getMaterialId());
            PurchaseRequestItem it = new PurchaseRequestItem();
            it.setPrId(pr.getPrId());
            it.setMaterialId(i.getMaterialId());
            it.setQtyApply(i.getQtyApply());
            it.setRefPrice(m.getRefPrice());
            it.setAmount(m.getRefPrice().multiply(i.getQtyApply()));
            it.setQtyConfirmed(i.getQtyApply());
            priMapper.insert(it);
        }
        return detail(pr.getPrId());
    }

    /** 提交审批：必填信息校验 → 预算余额校验并冻结 */
    @Transactional
    public void submit(Long id) {
        PurchaseRequest pr = detail(id);
        if (pr.getPrStatus() != 0) throw new BizException(2005, "只有草稿状态可以提交");
        if (pr.getUsagePurpose() == null || pr.getUsagePurpose().isBlank())
            throw new BizException(2016, "请先补充用途说明（采购理由）再提交");
        if (pr.getDeptId() == null || pr.getDeptId() == 0)
            throw new BizException(2017, "申请人未归属部门，请先在用户管理中分配部门");
        if (pr.getBudgetNo() == null || pr.getBudgetNo().isBlank())
            throw new BizException(2018, "请先选择预算科目再提交");
        if (budgetMapper.freeze(pr.getBudgetNo(), pr.getTotalAmount()) == 0)
            throw new BizException(2006, "预算余额不足，预算科目：" + pr.getBudgetNo());
        PurchaseRequest up = new PurchaseRequest();
        up.setPrId(id);
        up.setPrStatus(1);
        prMapper.updateById(up);
    }

    /** 审批：1同意 2驳回（含部分核准数量回填） */
    @Transactional
    public void approve(Long id, ReqDTOs.Approve dto, List<ReqDTOs.ConfirmItem> confirms) {
        PurchaseRequest pr = detail(id);
        requireApproverRole(dto.getApproverId());
        if (pr.getPrStatus() != 1) throw new BizException(2007, "单据不在待审批状态");
        if (dto.getApproverId() != null && dto.getApproverId().equals(pr.getApplicantId())) {
            throw new BizException(2008, "不能审批本人提交的申请，请由采购员或其他负责人审批");
        }
        // 部门负责人只能审批本部门申请，跨部门单据由采购员或管理员处理
        List<String> roles = userMapper.selectRoleCodes(dto.getApproverId());
        boolean globalApprover = roles != null && roles.stream()
                .anyMatch(r -> "PURCHASER".equals(r) || "ADMIN".equals(r));
        if (!globalApprover) {
            Long approverDept = toLong(userMapper.selectDeptId(dto.getApproverId()));
            if (approverDept == null || !approverDept.equals(pr.getDeptId())) {
                throw new BizException(2019, "部门负责人只能审批本部门的采购申请，跨部门申请请由采购员处理");
            }
        }

        // 按申请金额分级授权：≤2000 部门负责人；2000~10000 采购员；>10000 管理员
        java.math.BigDecimal amount = pr.getTotalAmount() == null ? java.math.BigDecimal.ZERO : pr.getTotalAmount();
        boolean admin = roles != null && roles.contains("ADMIN");
        boolean purchaser = roles != null && roles.contains("PURCHASER");
        if (!admin) {
            if (purchaser) {
                if (amount.compareTo(new java.math.BigDecimal("10000")) > 0) {
                    throw new BizException(2020, "申请金额超过 10000 元，需由管理员审批");
                }
            } else if (amount.compareTo(new java.math.BigDecimal("2000")) > 0) {
                throw new BizException(2021, "申请金额超过 2000 元，部门负责人无审批权限，请选择采购员或管理员审批");
            }
        }

        if (dto.getAction() == 1) {
            // 部分核准：调整明细数量与总金额
            BigDecimal total = BigDecimal.ZERO;
            for (PurchaseRequestItem it : items(id)) {
                if (confirms != null) {
                    confirms.stream().filter(c -> c.getPriId().equals(it.getPriId())).findFirst()
                            .ifPresent(c -> it.setQtyConfirmed(c.getQtyConfirmed()));
                }
                it.setAmount(it.getRefPrice().multiply(it.getQtyConfirmed()));
                priMapper.updateById(it);
                total = total.add(it.getAmount());
            }
            PurchaseRequest up = new PurchaseRequest();
            up.setPrId(id);
            up.setPrStatus(2);
            up.setTotalAmount(total);
            prMapper.updateById(up);
        } else {
            budgetMapper.unfreeze(pr.getBudgetNo(), pr.getTotalAmount());
            PurchaseRequest up = new PurchaseRequest();
            up.setPrId(id);
            up.setPrStatus(3);
            prMapper.updateById(up);
        }
        ApproveLog log = new ApproveLog();
        log.setBizType(4);   // 4=采购（报告字典口径）
        log.setBizId(id);
        log.setApproverId(dto.getApproverId());
        log.setApproveResult(dto.getAction() != null && dto.getAction() == 1 ? 1 : 0);
        log.setOpinion(dto.getOpinion());
        log.setApproveTime(java.time.LocalDateTime.now());
        approveLogMapper.insert(log);
    }

    public List<ApproveLog> approveLogs(Long id) {
        return approveLogMapper.selectByBiz(4, id);
    }

    public List<PurchaseRequest> pending() {
        return prMapper.selectListWithNames().stream()
                .filter(p -> p.getPrStatus() == 1).toList();
    }

    /** 6 位组织编码（mat_dept.org_id）转 purchase_request.dept_id 的 BIGINT */
    private Long toLong(String orgId) {
        if (orgId == null || orgId.isBlank()) return null;
        try {
            return Long.valueOf(orgId.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
