package com.school.material.sys.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.school.material.common.PageResult;
import com.school.material.sys.dto.SysOperationLogVO;
import com.school.material.sys.entity.SysOperationLog;
import com.school.material.sys.mapper.SysOperationLogMapper;
import com.school.material.sys.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SysOperationLogServiceImpl implements SysOperationLogService {

    private static final String ERROR_SEPARATOR = " | 失败: ";

    private final SysOperationLogMapper sysOperationLogMapper;

    @Override
    public void save(SysOperationLog log) {
        sysOperationLogMapper.insert(log);
    }

    @Override
    public PageResult<SysOperationLogVO> page(Integer pageNum, Integer pageSize, String operator,
                                              String module, Integer status) {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);

        LambdaQueryWrapper<SysOperationLog> wrapper = Wrappers.<SysOperationLog>lambdaQuery()
                .like(StringUtils.hasText(operator), SysOperationLog::getUserName,
                        operator == null ? null : operator.trim())
                .like(StringUtils.hasText(module), SysOperationLog::getModule,
                        module == null ? null : module.trim())
                .eq(status != null, SysOperationLog::getResult, status)
                .orderByDesc(SysOperationLog::getLogId);

        Page<SysOperationLog> page = sysOperationLogMapper.selectPage(new Page<>(num, size), wrapper);
        return PageResult.of(page.getTotal(), page.getRecords().stream().map(this::toVO).toList());
    }

    private SysOperationLogVO toVO(SysOperationLog log) {
        SysOperationLogVO vo = new SysOperationLogVO();
        vo.setOperator(log.getUserName());
        vo.setModule(log.getModule());
        vo.setOperationType(log.getAction());
        vo.setIp(log.getIp());
        vo.setCostMs(log.getCostMs());
        vo.setStatus(log.getResult());
        vo.setCreatedAt(log.getOperateTime());

        String target = log.getTarget();
        if (StringUtils.hasText(target)) {
            int index = target.indexOf(' ');
            if (index > 0) {
                vo.setRequestMethod(target.substring(0, index));
                vo.setRequestUri(target.substring(index + 1));
            } else {
                vo.setRequestUri(target);
            }
        }

        String detail = log.getDetail();
        if (detail != null) {
            int index = detail.indexOf(ERROR_SEPARATOR);
            if (index >= 0) {
                vo.setRequestParams(detail.substring(0, index));
                vo.setErrorMsg(detail.substring(index + ERROR_SEPARATOR.length()));
            } else {
                vo.setRequestParams(detail);
            }
        }
        return vo;
    }
}
