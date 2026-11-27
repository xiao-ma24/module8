package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 操作审计日志 */
@Data
@TableName("mat_operation_log")
public class SysAuditLog {
    @TableId(type = IdType.AUTO)
    private Long logId;
    @TableField("operator_id")
    private Long userId;
    private String userName;
    private String module;
    private String action;
    private String target;
    private String detail;
    private String beforeValue;
    private String afterValue;
    private String ip;
    private Integer result;
    private Integer costMs;
    @TableField("operate_time")
    private LocalDateTime createdAt;
}
