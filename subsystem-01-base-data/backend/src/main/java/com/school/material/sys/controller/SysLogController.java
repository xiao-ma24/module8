package com.school.material.sys.controller;

import com.school.material.security.RequireRole;
import com.school.material.common.PageResult;
import com.school.material.common.Result;
import com.school.material.sys.dto.SysOperationLogVO;
import com.school.material.sys.service.SysOperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 操作日志查询接口（管理员监控用）
 */
@RestController
@RequestMapping("/sys/log")
@RequiredArgsConstructor
@RequireRole({"ADMIN"})
public class SysLogController {

    private final SysOperationLogService sysOperationLogService;

    @GetMapping("/list")
    public Result<PageResult<SysOperationLogVO>> list(@RequestParam(defaultValue = "1") Integer pageNum,
                                                      @RequestParam(defaultValue = "10") Integer pageSize,
                                                      @RequestParam(required = false) String operator,
                                                      @RequestParam(required = false) String module,
                                                      @RequestParam(required = false) Integer status) {
        return Result.ok(sysOperationLogService.page(pageNum, pageSize, operator, module, status));
    }
}
