package com.school.material.purchase.service;

import com.school.material.purchase.entity.*;
import com.school.material.purchase.mapper.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

/** 个人中心：账号信息 + 岗位视角工作量 + 待办 + 操作记录 */
@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserMapper userMapper;
    private final DeptMapper deptMapper;
    private final PurchaseRequestMapper prMapper;
    private final PurchaseOrderMapper poMapper;
    private final InboundOrderMapper inMapper;
    private final AcceptanceOrderMapper acptMapper;
    private final ApproveLogMapper logMapper;
    private final ItemMappers itemMappers;

    public Map<String, Object> profile(Long userId) {
        User found = userMapper.selectById(userId == null ? 2L : userId);
        if (found == null) found = userMapper.selectList(null).stream().findFirst()
                .orElseThrow(() -> new RuntimeException("用户不存在"));
        final User u = found;
        Dept d = u.getDeptId() == null ? null : deptMapper.selectById(u.getDeptId());
        String month = LocalDate.now().toString().substring(0, 7);

        Map<String, Object> r = new HashMap<>();
        Map<String, Object> info = new HashMap<>();
        info.put("userId", u.getUserId());
        info.put("userNo", u.getUserNo());
        info.put("userName", u.getUserName());
        info.put("roleCode", u.getRoleCode());
        info.put("roleName", roleName(u.getRoleCode()));
        info.put("deptId", u.getDeptId());
        info.put("deptName", d == null ? "—" : d.getDeptName());
        info.put("userStatus", u.getUserStatus());
        info.put("createdAt", u.getCreatedAt() == null ? "—" : u.getCreatedAt().toString().replace('T', ' ').substring(0, 10));
        r.put("info", info);

        List<PurchaseRequest> prs = prMapper.selectListWithNames();
        List<PurchaseOrder> pos = poMapper.selectListWithNames();
        List<InboundOrder> ins = inMapper.selectListWithNames();
        List<AcceptanceOrder> acpts = acptMapper.selectList(null);

        // 采购视角
        List<Map<String, Object>> buy = new ArrayList<>();
        long myPr = prs.stream().filter(p -> Objects.equals(p.getApplicantId(), u.getUserId())).count();
        long monthPr = prs.stream().filter(p -> Objects.equals(p.getApplicantId(), u.getUserId()))
                .filter(p -> p.getCreatedAt() != null && p.getCreatedAt().toString().startsWith(month)).count();
        BigDecimal monthAmt = pos.stream()
                .filter(p -> p.getOrderDate() != null && p.getOrderDate().toString().startsWith(month))
                .filter(p -> p.getPoStatus() != 4)
                .map(p -> p.getTotalAmount() == null ? BigDecimal.ZERO : p.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long activeOrders = pos.stream().filter(p -> p.getPoStatus() == 1 || p.getPoStatus() == 2).count();
        long supCount = pos.stream().map(PurchaseOrder::getSupplierId).filter(Objects::nonNull).distinct().count();
        buy.add(stat("我发起的申请", myPr, "单", "累计"));
        buy.add(stat("本月申请", monthPr, "单", "当月新增"));
        buy.add(stat("本月采购金额", monthAmt, "元", "不含已取消"));
        buy.add(stat("在途订单", activeOrders, "单", "已确认/部分到货"));
        buy.add(stat("合作供应商", supCount, "家", "去重统计"));
        r.put("buyStats", buy);

        // 仓储视角
        List<Map<String, Object>> store = new ArrayList<>();
        long monthIn = ins.stream().filter(i -> i.getInStatus() == 1)
                .filter(i -> i.getInDate() != null && i.getInDate().toString().startsWith(month)).count();
        BigDecimal monthInAmt = ins.stream().filter(i -> i.getInStatus() == 1)
                .filter(i -> i.getInDate() != null && i.getInDate().toString().startsWith(month))
                .map(i -> i.getTotalAmount() == null ? BigDecimal.ZERO : i.getTotalAmount())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long waitIn = ins.stream().filter(i -> i.getInStatus() == 0).count();
        long expiring = 0;
        for (InboundOrder io : ins) {
            for (InboundItem it : itemMappers.selectInItems(io.getInId())) {
                if (it.getExpireDate() != null) {
                    long days = ChronoUnit.DAYS.between(LocalDate.now(), it.getExpireDate());
                    if (days >= 0 && days <= 30) expiring++;
                }
            }
        }
        long totalKinds = ins.stream().filter(i -> i.getInStatus() == 1).count();
        store.add(stat("本月入库单", monthIn, "单", "已完成"));
        store.add(stat("本月入库金额", monthInAmt, "元", "已入库"));
        store.add(stat("待入库单", waitIn, "单", "需登记批次"));
        store.add(stat("30天临期批次", expiring, "批", "需优先发放"));
        store.add(stat("累计入库批次", totalKinds, "单", "历史总量"));
        r.put("storeStats", store);

        // 待办
        List<Map<String, Object>> todo = new ArrayList<>();
        prs.stream().filter(p -> p.getPrStatus() == 1).forEach(p -> todo.add(todo("采购申请待审批", p.getPrNo(),
                p.getApplicantName() + "｜" + nvl(p.getUsagePurpose()), "warning", "/requests")));
        pos.stream().filter(p -> p.getPoStatus() == 0).forEach(p -> todo.add(todo("订单待确认", p.getPoNo(),
                p.getSupplierName() + "｜¥" + nvl(p.getTotalAmount()), "primary", "/orders")));
        ins.stream().filter(i -> i.getInStatus() == 0).forEach(i -> todo.add(todo("待入库登记", i.getInNo(),
                nvl(i.getWarehouseName()) + "｜¥" + nvl(i.getTotalAmount()), "danger", "/inbounds")));
        acpts.stream().filter(a -> a.getAcptStatus() == 1).forEach(a -> todo.add(todo("验收未完成", a.getAcptNo(),
                nvl(a.getSupplierName()), "info", "/acceptances")));
        r.put("todos", todo);

        // 操作记录：审批日志 + 入库记录
        List<Map<String, Object>> logs = new ArrayList<>();
        for (ApproveLog l : logMapper.selectList(null)) {
            logs.add(log(l.getApproveTime() == null ? "" : l.getApproveTime().toString().replace('T', ' '),
                    l.getApproverName(), l.getApproveResult() != null && l.getApproveResult() == 1 ? "同意" : "驳回",
                    nvl(l.getOpinion())));
        }
        for (InboundOrder i : ins.stream().filter(x -> x.getInStatus() == 1).collect(Collectors.toList())) {
            logs.add(log(i.getInDate() == null ? "" : i.getInDate().toString().replace('T', ' '),
                    i.getKeeperName(), "入库确认", i.getInNo() + " 已更新库存并写入流水"));
        }
        logs.sort((a, b) -> String.valueOf(b.get("time")).compareTo(String.valueOf(a.get("time"))));
        r.put("logs", logs.stream().limit(10).collect(Collectors.toList()));

        // 同岗位可切换账号（课程演示用：无登录态，切换查看不同岗位视角）
        r.put("roles", userMapper.selectList(null).stream().map(x -> {
            Map<String, Object> m = new HashMap<>();
            m.put("userId", x.getUserId());
            m.put("userName", x.getUserName());
            m.put("roleName", roleName(x.getRoleCode()));
            return m;
        }).collect(Collectors.toList()));
        return r;
    }

    private static Map<String, Object> stat(String label, Object value, String unit, String note) {
        Map<String, Object> m = new HashMap<>();
        m.put("label", label);
        m.put("value", value);
        m.put("unit", unit);
        m.put("note", note);
        return m;
    }

    private static Map<String, Object> todo(String type, String no, String desc, String tag, String path) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("no", no);
        m.put("desc", desc);
        m.put("tag", tag);
        m.put("path", path);
        return m;
    }

    private static Map<String, Object> log(String time, String who, String action, String detail) {
        Map<String, Object> m = new HashMap<>();
        m.put("time", time);
        m.put("who", who);
        m.put("action", action);
        m.put("detail", detail);
        return m;
    }

    private static String nvl(Object v) {
        return v == null ? "—" : String.valueOf(v);
    }

    private static String roleName(String code) {
        if (code == null) return "—";
        return switch (code) {
            case "DEPT_HEAD" -> "部门负责人";
            case "PURCHASER" -> "采购员";
            case "STORE_KEEPER" -> "仓库管理员";
            case "CHECKER" -> "验收人";
            case "STOCKTAKER" -> "盘点员";
            case "APPLICANT" -> "申购人 / 教师";
            case "ADMIN" -> "系统管理员";
            default -> code;
        };
    }
}
