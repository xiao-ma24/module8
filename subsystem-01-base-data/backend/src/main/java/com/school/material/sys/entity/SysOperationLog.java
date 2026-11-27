package com.school.material.sys.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 平台操作日志（团队 sys_operation_log 表）
 */
@Data
@TableName("mat_operation_log")
public class SysOperationLog {

    @TableId(value = "log_id", type = IdType.AUTO)
    private Long logId;

    /** 操作人 user_id，暂无真实登录态记 0 */
    private Long operatorId;

    /** 操作人姓名 / 工号 */
    @TableField("user_name")
    private String userName;

    /** 操作模块，如：物资档案 / 用户管理 */
    private String module;

    /** 操作类型：新增 / 修改 / 删除 / 审核 / 提交 / 登记 / 分配角色 */
    private String action;

    /** 操作对象，形如 "POST /api/v1/base/sys/dept" */
    private String target;

    /** 请求参数 + 失败原因（≤500 字符） */
    private String detail;

    private String beforeValue;

    private String afterValue;

    /** 来源 IP */
    private String ip;

    /** 1 成功 / 0 失败 */
    private Integer result;

    /** 耗时（毫秒） */
    private Long costMs;

    private LocalDateTime operateTime;
}
