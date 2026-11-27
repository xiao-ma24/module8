package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 系统参数 */
@Data
@TableName("sys_config")
public class SysConfig {
    @TableId(type = IdType.AUTO)
    private Long configId;
    private String configKey;
    private String configValue;
    private String configName;
    private String configGroup;
    private String valueType;
    private Integer builtin;
    private String remark;
    private LocalDateTime updatedAt;
}
