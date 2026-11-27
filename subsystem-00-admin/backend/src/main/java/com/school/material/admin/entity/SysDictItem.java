package com.school.material.admin.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

/** 字典项 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {
    @TableId(type = IdType.AUTO)
    private Long itemId;
    private String dictType;
    private String itemValue;
    private String itemLabel;
    private String itemColor;
    private Integer sortNo;
    private Integer itemStatus;
    private String remark;
}
