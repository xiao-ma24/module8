package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 登录日志 */
@Data
@TableName("sys_login_log")
public class SysLoginLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String userNo;
    private String userName;
    private String ip;
    private String device;
    private Integer result;
    private String message;
    private LocalDateTime createdAt;
}
