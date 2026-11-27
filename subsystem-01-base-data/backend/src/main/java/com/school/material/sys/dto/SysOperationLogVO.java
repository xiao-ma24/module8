package com.school.material.sys.dto;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 操作日志查询响应：保持子系统①原有 JSON 字段不变
 */
@Data
public class SysOperationLogVO {

    private String operator;

    private String module;

    private String operationType;

    private String requestMethod;

    private String requestUri;

    private String requestParams;

    private String ip;

    private Long costMs;

    private Integer status;

    private String errorMsg;

    private LocalDateTime createdAt;
}
