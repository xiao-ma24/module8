package com.school.material.admin.service;

import com.school.material.admin.common.BizException;
import com.school.material.admin.entity.DeptQuota;
import com.school.material.admin.mapper.DeptQuotaMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.YearMonth;
import java.util.List;

/**
 * 部门月度领用额度（报告表2-4）：额度来自财务系统预算表 budget 中按部门核定的年度预算，
 * 月度额度 = 年度额度 ÷ 12；本模块只读展示，出库时由 03 子系统累加已用额度。
 */
@Service
@RequiredArgsConstructor
public class DeptQuotaService {

    private final DeptQuotaMapper quotaMapper;

    public List<DeptQuota> list(String period, String keyword) {
        String p = normalizePeriod(period);
        // 每次读取前按财务预算刷新月度额度，保证与财务系统口径一致
        quotaMapper.syncFromBudget(p);
        return quotaMapper.selectWithDept(p, keyword);
    }

    /** 缺省取当前年月；格式必须为 yyyyMM */
    private String normalizePeriod(String period) {
        String p = period == null ? "" : period.trim();
        if (p.isEmpty()) p = YearMonth.now().toString().replace("-", "");
        if (!p.matches("\\d{6}")) throw new BizException("额度年月格式应为 yyyyMM，如 202611");
        return p;
    }
}
