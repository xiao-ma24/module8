package com.school.material.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 物资目录（团队数据字典 material 表）
 */
@Data
@TableName("mat_material")
public class Material {

    @TableId(value = "material_id", type = IdType.AUTO)
    private Long id;

    /** 物资编码 */
    @TableField("material_code")
    private String materialNo;

    /** 物资名称 */
    private String materialName;

    /** 分类编码：对应平台字典 sys_dict_item.item_value */
    private String categoryCode;

    /** 物资类型：1低值易耗品 2低值耐用品 */
    private Integer materialType;

    /** 计量单位 */
    private String unit;

    /** 参考单价 */
    @TableField("ref_price")
    private BigDecimal referencePrice;

    /** 1管控物资 0普通物资 */
    private Integer isControlled;

    /** 限用年限（年） */
    private Integer serviceLifeYears;

    /** 保质期天数 */
    private Integer shelfLifeDays;

    /** 安全库存下限 */
    private BigDecimal safeStock;

    private String createdBy;

    private LocalDateTime createdAt;

    private String updatedBy;

    private LocalDateTime updatedAt;

    @TableLogic(value = "0", delval = "1")
    private Integer delFlag;
}
