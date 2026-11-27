package com.school.material.sys.service;

import com.school.material.common.PageResult;
import com.school.material.sys.dto.SysOperationLogVO;
import com.school.material.sys.entity.SysOperationLog;

public interface SysOperationLogService {

    /** 写入一条操作日志 */
    void save(SysOperationLog log);

    /** 分页查询操作日志 */
    PageResult<SysOperationLogVO> page(Integer pageNum, Integer pageSize, String operator,
                                       String module, Integer status);
}
