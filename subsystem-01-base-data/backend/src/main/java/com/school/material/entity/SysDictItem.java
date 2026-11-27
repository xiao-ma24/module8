package com.school.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 字典项（平台管理端 sys_dict_item），物资分类由分类字典维护
 */
@Data
@TableName("sys_dict_item")
public class SysDictItem {

    @TableId(type = IdType.AUTO)
    private Long itemId;

    /** 字典类型编码，如 DictTypes.MATERIAL_CATEGORY */
    private String dictType;

    /** 字典项编码 */
    private String itemValue;

    /** 字典项名称 */
    private String itemLabel;

    /** 标签颜色 */
    private String itemColor;

    /** 排序号 */
    private Integer sortNo;

    /** 1 启用 / 0 停用 */
    private Integer itemStatus;

    private String remark;
}
