package com.school.material.admin.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.Dept;
import com.school.material.admin.entity.User;
import com.school.material.admin.mapper.DeptMapper;
import com.school.material.admin.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/** 部门（组织架构） */
@Service
@RequiredArgsConstructor
public class DeptService {

    private final DeptMapper deptMapper;
    private final UserMapper userMapper;
    private final AuditService audit;

    public List<Dept> list(String keyword, Integer status) {
        List<Dept> rows = deptMapper.selectList(Wrappers.<Dept>lambdaQuery()
                .eq(status != null, Dept::getDeptStatus, status)
                .and(keyword != null && !keyword.isBlank(), w -> w
                        .like(Dept::getDeptName, keyword).or().like(Dept::getDeptCode, keyword))
                .orderByAsc(Dept::getSortNo));
        Map<Long, String> users = userMapper.selectList(Wrappers.<User>lambdaQuery().select(User::getUserId, User::getUserName))
                .stream().collect(Collectors.toMap(User::getUserId, User::getUserName, (a, b) -> a));
        for (Dept d : rows) {
            d.setManagerName(users.getOrDefault(d.getManagerId(), "—"));
            Integer c = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getDeptId, d.getDeptId())).intValue();
            d.setUserCount(c);
        }
        return rows;
    }

    public Dept save(Dept in) {
        if (in.getDeptName() == null || in.getDeptName().isBlank()) throw new BizException("部门名称不能为空");
        if (in.getDeptId() == null || in.getDeptId().isBlank()) {
            in.setDeptId(nextOrgId());
            in.setDeptStatus(in.getDeptStatus() == null ? 1 : in.getDeptStatus());
            in.setParentId(in.getParentId() == null || in.getParentId().isBlank() ? "000000" : in.getParentId());
            in.setAnnualQuota(in.getAnnualQuota() == null ? BigDecimal.ZERO : in.getAnnualQuota());
            in.setUsedAmount(BigDecimal.ZERO);
            deptMapper.insert(in);
            audit.write("部门管理", "新增", in.getDeptName(), "编码 " + in.getDeptCode());
        } else {
            Dept old = deptMapper.selectById(in.getDeptId());
            deptMapper.updateById(in);
            audit.write("部门管理", "修改", in.getDeptName(), deptDiff(old, in),
                    old == null ? null : "编码=" + old.getDeptCode() + "，上级=" + old.getParentId()
                            + "，负责人=" + old.getManagerId() + "，年度额度=" + old.getAnnualQuota() + "，状态=" + old.getDeptStatus(),
                    "编码=" + in.getDeptCode() + "，上级=" + in.getParentId()
                            + "，负责人=" + in.getManagerId() + "，年度额度=" + in.getAnnualQuota() + "，状态=" + in.getDeptStatus());
        }
        return in;
    }

    private String deptDiff(Dept old, Dept now) {
        if (old == null) return "更新部门信息";
        List<String> changes = new java.util.ArrayList<>();
        if (!java.util.Objects.equals(old.getDeptCode(), now.getDeptCode()))
            changes.add("编码 " + old.getDeptCode() + " → " + now.getDeptCode());
        if (!java.util.Objects.equals(old.getParentId(), now.getParentId()))
            changes.add("上级部门 " + old.getParentId() + " → " + now.getParentId());
        if (!java.util.Objects.equals(old.getManagerId(), now.getManagerId()))
            changes.add("负责人已变更");
        if (old.getAnnualQuota() != null && now.getAnnualQuota() != null
                && old.getAnnualQuota().compareTo(now.getAnnualQuota()) != 0)
            changes.add("年度额度 " + old.getAnnualQuota() + " → " + now.getAnnualQuota());
        if (!java.util.Objects.equals(old.getDeptStatus(), now.getDeptStatus()))
            changes.add("状态 " + old.getDeptStatus() + " → " + now.getDeptStatus());
        return changes.isEmpty() ? "更新部门信息" : String.join("；", changes);
    }

    /** 生成 6 位组织编码（与第4组师资系统 org_organization.org_id 对齐） */
    private String nextOrgId() {
        List<Dept> all = deptMapper.selectList(Wrappers.<Dept>lambdaQuery().select(Dept::getDeptId));
        int max = 0;
        for (Dept d : all) {
            try {
                max = Math.max(max, Integer.parseInt(d.getDeptId()));
            } catch (Exception ignore) {
            }
        }
        return String.format("%06d", max + 1);
    }

    public void remove(String deptId) {
        Dept d = deptMapper.selectById(deptId);
        if (d == null) throw new BizException("部门不存在");
        Long c = userMapper.selectCount(Wrappers.<User>lambdaQuery().eq(User::getDeptId, deptId));
        if (c != null && c > 0) throw new BizException("该部门下还有 " + c + " 名成员，请先调整人员归属");
        deptMapper.deleteById(deptId);
        audit.write("部门管理", "删除", d.getDeptName(), null);
    }
}
