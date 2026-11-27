package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.time.LocalDateTime;

/** 字典类型 */
@Data
@TableName("sys_dict_type")
public class SysDictType {
    @TableId(type = IdType.INPUT)
    private String dictType;
    private String dictName;
    private String remark;
    private Integer dictStatus;
    private LocalDateTime createdAt;

    @TableField(exist = false)
    private Integer itemCount;
}
